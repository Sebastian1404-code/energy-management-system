#!/usr/bin/env python3
import os, json, sys
from confluent_kafka import Producer

bootstrap = os.getenv("KAFKA_BOOTSTRAP", "kafka:9092")
topic = os.getenv("KAFKA_TOPIC", "device-readings")

conf = {
    "bootstrap.servers": bootstrap,
    "acks": "1",
    "request.timeout.ms": 40000,     # ~40s
    "message.timeout.ms": 130000,    # overall delivery timeout (like delivery.timeout.ms)
    "enable.idempotence": False,
    "retries": 5,
    "linger.ms": 0,
}

p = Producer(conf)

def dr_cb(err, msg):
    if err is not None:
        print(f"DELIVERY_ERROR: {err}", file=sys.stderr)
    else:
        print(f"DELIVERED: topic={msg.topic()} partition={msg.partition()} offset={msg.offset()} key={msg.key()}")

payload = {"warmup": True}
p.produce(topic, key="device-000".encode(), value=json.dumps(payload).encode(), callback=dr_cb)
p.flush(130.0)  # wait up to 130s for delivery report
