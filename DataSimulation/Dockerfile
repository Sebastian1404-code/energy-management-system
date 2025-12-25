FROM python:3.11-slim
WORKDIR /app

RUN apt-get update && apt-get install -y bash

# app code
COPY kafka_device_simulator.py /app/

# deps
RUN pip install --no-cache-dir confluent-kafka==2.6.0

# entrypoint
COPY entrypoint.sh /entrypoint.sh

RUN sed -i 's/\r$//' /entrypoint.sh && chmod +x /entrypoint.sh

ENTRYPOINT ["/bin/bash", "/entrypoint.sh"]
