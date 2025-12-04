#!/usr/bin/env python3
from __future__ import annotations
import argparse, json, math, os, random, signal, sys, time
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from typing import Iterator, List
from confluent_kafka import Producer, KafkaException

STOP = False
def _h(_, __):  # SIGINT/SIGTERM
    global STOP; STOP = True
signal.signal(signal.SIGINT, _h); signal.signal(signal.SIGTERM, _h)

@dataclass
class DeviceState:
    device_id: str
    base_kwh_per_10m: float
    drift: float

def iso_utc(dt: datetime) -> str:
    return dt.replace(tzinfo=timezone.utc).isoformat().replace("+00:00", "Z")

def round_down_10m(dt: datetime) -> datetime:
    m = (dt.minute // 10) * 10
    return dt.replace(minute=m, second=0, microsecond=0)

def daily_profile_multiplier(hour: int) -> float:
    radians = ((hour - 17) % 24) / 24 * 2 * math.pi
    sine = (math.cos(radians) + 1) / 2
    return 0.6 + 0.8 * sine

def noise_component() -> float:
    n = random.gauss(0.0, 0.03)
    return max(-0.08, min(0.08, n))

def generate_value_kwh(ts: datetime, st: DeviceState) -> float:
    mult = daily_profile_multiplier(ts.hour)
    seasonal = 1.0 + st.drift * math.sin((ts.timetuple().tm_yday / 365.0) * 2 * math.pi)
    val = st.base_kwh_per_10m * mult * seasonal
    val *= (1.0 + noise_component())
    return max(0.01, round(val, 4))

def iter_time(start: datetime, step: timedelta) -> Iterator[datetime]:
    t = start
    while True:
        yield t
        t += step

def build_devices(prefix: str, device_indices: List[int]) -> List[DeviceState]:
    out = []
    for idx in device_indices:
        device_id = f"{prefix}{idx:03d}"   # ← formats 1 → device-001
        base = random.uniform(0.05, 0.25)
        drift = random.uniform(-0.05, 0.05)
        out.append(DeviceState(device_id, base, drift))
    return out


def make_producer(bootstrap: str, acks: str) -> Producer:
    """
    Confluent (librdkafka) producer with Docker-friendly timeouts.
    - acks: "0" | "1" | "all"
    - request.timeout.ms: per-request broker wait
    - message.timeout.ms: overall delivery timeout (like delivery.timeout.ms)
    """
    conf = {
        "bootstrap.servers": bootstrap,
        "acks": acks,
        "linger.ms": 0,
        "batch.size": 16384,
        "request.timeout.ms": 40000,
        "message.timeout.ms": 130000,
        "enable.idempotence": False,
        "retries": 5,
        "retry.backoff.ms": 500,
        "max.in.flight.requests.per.connection": 5,
    }
    return Producer(conf)

def run(bootstrap: str, topic: str, speedup: int,
        hours: float | None, seed: int | None, prefix: str, acks: str,
        config_path: str):
    if seed is not None:
        random.seed(seed)

    with open(config_path, "r") as f:
        cfg = json.load(f)

    device_indices = cfg.get("device_ids", [])

    if not device_indices:
        raise ValueError("Config file must contain a non-empty 'device_ids' list")

    producer = make_producer(bootstrap, acks)
    devices = build_devices(prefix, device_indices)

    # Flush policy: with acks!=0 we can optionally flush every N messages
    do_periodic_flush = (acks != "0")
    flush_every = 5000 if do_periodic_flush else None
    periodic_flush_timeout_s = 130.0   # align with message.timeout.ms

    step = timedelta(minutes=10)
    start = round_down_10m(datetime.utcnow())
    end_ts = start + timedelta(hours=hours) if hours is not None else None
    tick_sleep = max(0.001, step.total_seconds() / max(1, speedup))

    # Warm-up: one synchronous delivery via flush
    warmup_err = {"e": None}
    def _warm_cb(err, msg):
        warmup_err["e"] = err
        if err is None:
            print(f"Warm-up send OK (p={msg.partition()} off={msg.offset()})")
        else:
            print(f"Warm-up delivery error: {err}", file=sys.stderr)

    try:
        producer.produce(topic, key=b"device-000", value=b"{\"warmup\":true}", callback=_warm_cb)
    except BufferError:
        pass
    producer.flush(130.0)  # wait up to message.timeout.ms
    if warmup_err["e"] is not None and acks != "0":
        print("Continuing without warm-up success (will retry during main loop)", file=sys.stderr)

    def _err_cb(err, msg):
        print(f"Send error: {err}", file=sys.stderr)

    sent = 0
    try:
        for ts in iter_time(start, step):
            if STOP or (end_ts and ts >= end_ts):
                break

            for st in devices:
                msg = {
                    "device_id": st.device_id,
                    "timestamp": iso_utc(ts),
                    "value_kwh": generate_value_kwh(ts, st),
                }
                try:
                    producer.produce(
                        topic,
                        key=st.device_id.encode(),
                        value=json.dumps(msg, separators=(",", ":")).encode(),
                        callback=lambda err, m: (_err_cb(err, m) if err else None)
                    )
                except BufferError:
                    # queue full: backoff a bit and retry
                    producer.poll(0.1)
                    producer.produce(
                        topic,
                        key=st.device_id.encode(),
                        value=json.dumps(msg, separators=(",", ":")).encode(),
                        callback=lambda err, m: (_err_cb(err, m) if err else None)
                    )
                sent += 1

            # Drive delivery callbacks
            producer.poll(0)

            if do_periodic_flush and flush_every and sent % flush_every == 0:
                # ensure backlog is drained occasionally
                producer.flush(periodic_flush_timeout_s)

            if ts.minute == 0:
                print(f"[{iso_utc(ts)}] sent={sent}")

            time.sleep(tick_sleep)
    finally:
        producer.flush(130.0)
        print(f"✔ Sent {sent} messages to topic '{topic}' (acks={acks}).")

if __name__ == "__main__":
    p = argparse.ArgumentParser(description="Smart-meter simulator -> Kafka (confluent-kafka)")
    p.add_argument("--config", default="config.json",
                  help="Path to config file containing device index list")
    p.add_argument("--bootstrap", default=os.getenv("KAFKA_BOOTSTRAP", "kafka:9092"))
    p.add_argument("--topic", default=os.getenv("KAFKA_TOPIC", "device-readings"))
    p.add_argument("--devices", type=int, default=int(os.getenv("DEVICES", 100)))
    p.add_argument("--speedup", type=int, default=int(os.getenv("SPEEDUP", 600)))
    p.add_argument("--hours", type=float, default=None)
    p.add_argument("--seed", type=int, default=None)
    p.add_argument("--prefix", default=os.getenv("DEVICE_PREFIX", "device-"))
    p.add_argument("--acks", default=os.getenv("ACKS", "1"), choices=["0", "1", "all"])
    args = p.parse_args()
    try:
        run(args.bootstrap, args.topic, args.speedup,
            args.hours, args.seed, args.prefix, args.acks, args.config)
    except KafkaException as e:
        print(f"Kafka error: {e}", file=sys.stderr); sys.exit(1)
    except Exception as e:
        print(f"Error: {e}", file=sys.stderr); sys.exit(1)
