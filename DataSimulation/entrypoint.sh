#!/usr/bin/env bash
set -euo pipefail

echo "KAFKA_BOOTSTRAP=${KAFKA_BOOTSTRAP}  KAFKA_TOPIC=${KAFKA_TOPIC}  DEVICES=${DEVICES}  SPEEDUP=${SPEEDUP}  PREFIX=${DEVICE_PREFIX}  SEED=${SEED:-}  SIM_HOURS=${SIM_HOURS:-}  ACKS=${ACKS:-}"

# 1) Wait for TCP reachability (no Kafka CLI needed)
until python - <<'PY'
import socket, sys, os
host, port = os.environ.get("KAFKA_BOOTSTRAP","kafka:9092").split(":")
try:
    s = socket.create_connection((host, int(port)), 3); s.close(); sys.exit(0)
except Exception:
    sys.exit(1)
PY
do
  echo "Waiting for Kafka TCP @ ${KAFKA_BOOTSTRAP}..."
  sleep 2
done

# 2) Wait until metadata for the topic is obtainable using confluent-kafka
until python - <<'PY'
import os, sys, time
from confluent_kafka import Producer
bootstrap = os.environ.get("KAFKA_BOOTSTRAP","kafka:9092")
topic = os.environ.get("KAFKA_TOPIC","device-readings")
p = Producer({"bootstrap.servers": bootstrap, "request.timeout.ms": 40000, "message.timeout.ms": 130000})
ok = False
for _ in range(50):
    md = p.list_topics(timeout=5.0)     # fetch cluster metadata
    if topic in md.topics and md.topics[topic].error is None:
        ok = True
        break
    time.sleep(0.2)
p.flush(0)
sys.exit(0 if ok else 1)
PY
do
  echo "Waiting for Kafka bootstrap ${KAFKA_BOOTSTRAP}..."
  sleep 2
done
CONFIG_PATH="${CONFIG_PATH:-/app/config.json}"
# Build args array safely
ARGS=( --bootstrap "${KAFKA_BOOTSTRAP}" --topic "${KAFKA_TOPIC}"
       --devices "${DEVICES}" --speedup "${SPEEDUP}" --prefix "${DEVICE_PREFIX}"
       --acks "${ACKS:-0}" --config "${CONFIG_PATH}" )
[[ -n "${SEED:-}" ]] && ARGS+=( --seed "${SEED}" )
[[ -n "${SIM_HOURS:-}" ]] && ARGS+=( --hours "${SIM_HOURS}" )

echo "Starting simulator with: ${ARGS[*]}"
exec python -u /app/kafka_device_simulator.py "${ARGS[@]}"
