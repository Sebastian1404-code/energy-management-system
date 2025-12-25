# 🧩 Distributed System – Authorization, User, Device & Monitoring Microservices

This project is a modular **microservices-based distributed system** built with **Spring Boot**, **Kafka**, **PostgreSQL**, **Docker**, and **Traefik**.  
It demonstrates:

- Secure authentication & authorization
- Event-driven microservice communication
- Real-time IoT data processing
- Separation of concerns with per-service databases
- API gateway routing and JWT validation
- Synthetic smart-meter simulation feeding Kafka

---

## 🧱 Architecture Overview

### 🔧 Microservices

| Service | Description | Database | Communication |
|---------|-------------|-----------|----------------|
| **Authorization Service** | Handles registration, login, credentials & JWT issuance/validation | CredentialDb | REST + Traefik ForwardAuth |
| **User Service** | Stores user profiles & publishes `user.created` events | UserDb | REST + Kafka |
| **Device Service** | Manages devices; processes user events; publishes `device.monitoring` events | DeviceDb | Kafka |
| **Monitoring Service** | Consumes smart-meter data, aggregates kWh usage, exposes monitoring dashboards | MonitoringDb | Kafka + REST |
| **Device Simulator** | Python service generating realistic IoT readings | — | Kafka |
| **Kafka Broker** | Central event backbone | — | Kafka |
| **Traefik Gateway** | Reverse proxy, routing, security enforcement | — | HTTP |

---

## ⚙️ Technology Stack

- **Java 17**
- **Spring Boot 3.x**
- **Spring Security + JWT**
- **Spring Kafka** (including request–reply)
- **PostgreSQL**
- **Apache Kafka (KRaft mode)**
- **Traefik v3**
- **Docker & Docker Compose**
- **Python (confluent-kafka)**

---

# 🔐 Security Model

### ✔ Password Handling
- BCrypt hashing
- Credentials stored *separately* from user profile data (CredentialDb)

### ✔ JWT Authentication
- Authorization Service issues JWT tokens
- Traefik performs `ForwardAuth` → calls `/auth/validate`
- If valid, Traefik forwards request and injects:
    - `X-User-Id`
    - `X-User-Role`

Microservices trust Traefik and do **not** verify JWTs themselves.

---

# 🧭 System Workflow

## 🟦 Registration Flow
1. Frontend → **Authorization Service** (`/api/auth/register`)
2. Authorization Service:
    - Creates credentials
    - Calls User Service to create user record
3. User Service publishes:
   user.created.v1
4. Device Service consumes this event to synchronize device references.
5. User is redirected to login.

---

## 🟩 Login Flow

1. Frontend → `POST /api/auth/login`
2. Authorization Service:
- Validates username/password (BCrypt)
- Generates and returns a **JWT**
3. Frontend stores JWT (localStorage/sessionStorage)
4. Every request includes:
   Authorization: Bearer <jwt>


---

## 🟧 Runtime Request Flow

1. Request reaches **Traefik**
2. Traefik uses **ForwardAuth**:
- Sends request to `authorization-service:/auth/validate`
- If JWT is valid → receives a 200 OK
3. Traefik forwards the request to the microservice
4. Traefik injects identity headers:
X-User-Id: <id>
X-User-Role: <role>


5. Microservice uses these headers for authorization/ownership checks

---

# 📨 Kafka Messaging Overview

## Kafka Topics

| Topic | Producer | Consumer | Purpose |
|--------|-----------|-----------|----------|
| `user.created.v1` | User Service | Device Service | Notify when a new user is created |
| `device.monitoring` | Device Service | Monitoring Service | Device lifecycle events (create/delete) |
| `device-readings` | Device Simulator | Monitoring Service | IoT smart-meter telemetry |

---

# 📊 Monitoring Service

The Monitoring Service performs **real-time analytics** on IoT smart-meter data.

## 1. Kafka Consumer: `device-readings`

Consumes messages of this shape:

```json
{
"device_id": "device-005",
"timestamp": "2025-12-05T12:40:00Z",
"value_kwh": 0.1623
}
```


1. Tasks performed:
- Tracks last-seen timestamps
- Buffers incoming readings in memory
- Periodically aggregates values into time windows
- Writes aggregated results into window_consumption

--
2. Time Window Aggregation
The Monitoring Service aggregates raw device readings into fixed-size time windows.
Default aggregation settings:
- Window size: 1 minute (configurable)
- Flush interval: 5 seconds (configurable)
---
Aggregation:

- SUM(kwh) – total energy consumed
- COUNT(samples) – number of readings

| Column           | Type      | Description                            |
| ---------------- | --------- | -------------------------------------- |
| device_id        | String    | Device identifier (e.g., `device-005`) |
| window_start_utc | Timestamp | Start time of the window               |
| window_minutes   | Int       | Window duration in minutes             |
| kwh              | Double    | Total kWh in the window                |
| sample_count     | Int       | Number of readings received            |
| updated_at_utc   | Timestamp | Last update UTC timestamp              |


Example entries:

| device_id  | window_start_utc        | window_minutes | kwh    | sample_count |
|------------|--------------------------|----------------|--------|--------------|
| device-005 | 2025-12-04 21:30:00+00   | 1              | 0.7542 | 4            |
| device-005 | 2025-12-04 21:31:00+00   | 1              | 0.3779 | 2            |

---

## 3. Device Lifecycle Synchronization (`device.monitoring`)

The Monitoring Service also listens to **device lifecycle events** published by the Device Service.

Topic: device.monitoring


Example message:

```json
{
  "deviceId": 5,
  "devMonType": "DeviceCreated"
}
```

Messages on this topic inform the Monitoring Service about **device lifecycle events**.  
These events ensure that the Monitoring Service maintains an up-to-date list of devices that should appear in monitoring dashboards.

### Supported Event Types

| Event Type       | Meaning                                      |
|------------------|-----------------------------------------------|
| `DeviceCreated`  | A new device has been provisioned and should be tracked |
| `DeviceDeleted`  | A device has been removed and should no longer appear in monitoring |

---

### 🔄 How Monitoring Service Handles These Events

When a message arrives on `device.monitoring`, the Monitoring Service:

1. Deserializes it into a `KafkaPayloadMonitoring` object
2. Switches on the event type
3. Updates the `device_monitoring_ref` table accordingly

Pseudo-logic:

``` java
switch (event.getDevMonType()) {
    case DeviceCreated -> add deviceId to device_monitoring_ref
    case DeviceDeleted -> remove deviceId from device_monitoring_ref
}

```


When these events are processed, the Monitoring Service updates the **device_monitoring_ref** table so that it always reflects the current set of *active* devices in the ecosystem.

Here is the expanded explanation of what happens internally:

---

## 🧠 Detailed Handling Logic

### ✔ `DeviceCreated`

When a new device is provisioned in the **Device Service**, it sends:

```json
{
  "deviceId": 5,
  "devMonType": "DeviceCreated"
}
```

This JSON message represents a **device lifecycle event** emitted by the **Device Service** and consumed by the **Monitoring Service**.

It conveys two key pieces of information:

---

## 🔍 Field Breakdown

### **`deviceId`**
- A numeric identifier for the device.
- Example: `5` means this is **device-005** (depending on naming convention in your system).

### **`devMonType`**
- Describes what happened to the device.
- In this example, the value is: "DeviceCreated"



which indicates that a new device has been provisioned and should now be tracked by the Monitoring Service.

---

## 🔄 Event Purpose

This event notifies the Monitoring Service to **update its internal reference list**.  
Specifically:

- For `DeviceCreated` → **Add** the device to the monitoring reference table
- For `DeviceDeleted` → **Remove** the device from the table

This ensures that monitoring dashboards show only relevant devices.

---

## 🗃️ How the Monitoring Service Handles This Event

When the event arrives on Kafka topic: device.monitoring


the Monitoring Service deserializes it and executes:

``` java
switch (event.getDevMonType()) {
    case DeviceCreated -> deviceRefService.handleDeviceCreated(event.getDeviceId());
    case DeviceDeleted -> deviceRefService.handleDeviceDeleted(event.getDeviceId());
}
```

### 3. Device Lifecycle Synchronization (`device.monitoring`)

The Monitoring Service listens to **device lifecycle events** published by the Device Service.

### Kafka Topic
device.monitoring

### Example Message

```json
{
  "deviceId": 5,
  "devMonType": "DeviceCreated"
}
```


These events ensure that the Monitoring Service maintains an accurate, real-time catalog of all devices that should be included in monitoring calculations and dashboards.

---

## 📥 What Happens When an Event Is Consumed

When the Monitoring Service receives a message on the `device.monitoring` topic, it:

1. Deserializes the JSON into `KafkaPayloadMonitoring`
2. Checks `devMonType`
3. Updates the `device_monitoring_ref` table

- If `devMonType = DeviceCreated` → **insert** `deviceId` into `device_monitoring_ref` (if not already there)
- If `devMonType = DeviceDeleted` → **delete** `deviceId` from `device_monitoring_ref`

This table then defines the set of **active devices** that:
- Appear in `/monitoring`
- Are allowed in `/monitoring/devices/{id}/series`
- Are shown in monitoring dashboards

---

# 🔵 Device Simulator (Python Service)

The Device Simulator produces **synthetic smart-meter readings** to the `device-readings` topic.

- Reads a list of device indices from `config.json`, e.g.:

```json
  {
    "device_ids": [1, 5, 42]
  }
```


  This configuration makes the simulator generate data for:

  - `device-001`
  - `device-005`
  - `device-042`

- Every **10 simulated minutes**, it sends a message like:

```json
  {
    "device_id": "device-005",
    "timestamp": "2025-12-05T12:40:00Z",
    "value_kwh": 0.1623
  }
```

Every 10 simulated minutes, it sends a message like:
```json
{
"device_id": "device-005",
"timestamp": "2025-12-05T12:40:00Z",
"value_kwh": 0.1623
}
```


This JSON payload represents a single smart-meter reading and includes:

- **`device_id`** — the unique identifier of the simulated device
- **`timestamp`** — the exact moment (in UTC) when the consumption reading was recorded
- **`value_kwh`** — the amount of energy consumed during that 10-minute interval

Each message is published to the **`device-readings`** Kafka topic, where it is consumed by the Monitoring Service.

The Monitoring Service then:

1. **Assigns** each reading to a time window (e.g., 1-minute buckets)
2. **Aggregates** total `kwh` and `sample_count` per device and window
3. **Stores** the aggregated results in the `window_consumption` table for dashboards and analytics

---

## 🔧 Monitoring Database Tables

### `window_consumption`
Stores per-device energy usage aggregated over fixed time windows.

| Column             | Type      | Description                                |
|--------------------|-----------|--------------------------------------------|
| device_id          | String    | Device identifier (e.g. `device-005`)      |
| window_start_utc   | Timestamp | Start timestamp of the aggregation window  |
| window_minutes     | Int       | Window size in minutes                     |
| kwh                | Double    | Total kWh in that window                   |
| sample_count       | Int       | Number of readings included                |
| updated_at_utc     | Timestamp | Last update timestamp                       |

### `device_monitoring_ref`
Tracks all devices that should appear in monitoring views.

| Column    | Type |
|-----------|------|
| device_id | Long |

---


## 📡 Monitoring REST API

The Monitoring Service exposes two primary endpoints used by dashboards and analytics components.

### 1️⃣ **GET /monitoring**
Returns a summary of the **latest aggregated window** for each active device.

Example response:
```json
[
  {
    "deviceId": "device-005",
    "windowStartUtc": "2025-12-05T12:40:00Z",
    "windowMinutes": 1,
    "kwh": 0.3779,
    "sampleCount": 2,
    "lastSeen": "2025-12-05T12:40:30Z"
  }
]
```

This endpoint provides:

- latest kWh usage
- number of samples
- last timestamp the device produced data

---

## 2️⃣ **GET /monitoring/devices/{deviceId}/series?date=YYYY-MM-DD&tz=UTC**

Returns a **time-series of aggregated windows** for a specific device and day.

### Example Response
```json
[
  {
    "hour": 12,
    "minute": 40,
    "kwh": 0.3779,
    "ts_utc": "2025-12-05T12:40:00Z"
  }
]
```

Used by:

- line charts
- dashboards
- analytics visualizations

---

# 🔵 Device Simulator Overview

The **Device Simulator** generates realistic smart-meter readings and publishes them to the **`device-readings`** Kafka topic.

### Key behaviors:

- Reads device IDs from **`config.json`**
- Produces a reading every **10 simulated minutes**
- Supports accelerated simulation time via a **speedup** factor
- Emits JSON messages such as:

```json
{
  "device_id": "device-005",
  "timestamp": "2025-12-05T12:40:00Z",
  "value_kwh": 0.1623
}
```


These readings are consumed by the **Monitoring Service**, which assigns them to aggregation windows (e.g., 1-minute intervals), updates energy usage totals, and stores the results for visualization.

---

# 🗃 Essential Monitoring Database Tables

## **`window_consumption`**
Stores aggregated energy usage for each device over fixed time windows.

| Column             | Type      | Description                               |
|--------------------|-----------|-------------------------------------------|
| device_id          | String    | Identifier of the device                  |
| window_start_utc   | Timestamp | Start time of the aggregation window      |
| window_minutes     | Int       | Window size in minutes                    |
| kwh                | Double    | Total energy consumed in the window       |
| sample_count       | Int       | Number of readings included               |

---

## **`device_monitoring_ref`**
Stores the list of **active devices** that should appear in monitoring dashboards.

| Column    | Type |
|-----------|------|
| device_id | Long |

This table ensures that only valid, registered devices appear in:

- `/monitoring` summaries
- `/monitoring/devices/{id}/series` time-series results
- UI dashboards and analytics tools  


---

# 📡 How the Monitoring and Device Simulator Work Together

The **Device Simulator** continuously streams synthetic IoT readings into Kafka.  
The **Monitoring Service** then:

1. **Consumes** real-time readings from `device-readings`
2. **Groups** readings into time windows (e.g., 1-minute)
3. **Aggregates**:
    - total `kwh` per window
    - number of samples
4. **Tracks** the most recent reading for each device (`lastSeen`)
5. **Exposes** analytics-ready data through REST endpoints

This flow enables a fully functional IoT monitoring pipeline—from simulated devices to real dashboards.

---

# 🧩 End-to-End Event Flow (Summary)

1. **User creates account**  
   → Authorization & User Service work together  
   → User Service emits `user.created.v1`

2. **Device Service listens**  
   → Creates device models  
   → Emits `device.monitoring` events

3. **Monitoring Service updates device registry**  
   → Maintains accurate `device_monitoring_ref`

4. **Device Simulator streams telemetry**  
   → Sends JSON readings to `device-readings`

5. **Monitoring Service aggregates data**  
   → Writes to `window_consumption`  
   → Exposes REST APIs for UI

6. **Frontend displays dashboards**  
   → Powered by `/monitoring` and `/series` endpoints

This architecture cleanly separates:
- authentication
- user data
- device definitions
- real-time telemetry
- analytics

while connecting all services through Kafka.

---

# 🧰 Useful Development Commands

### View Kafka messages for device monitoring
```bash
docker exec -it kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --topic device.monitoring \
    --from-beginning
```

### View aggregated monitoring data

To inspect the latest aggregated windows stored by the Monitoring Service:

```bash
docker exec -it postgres-monitoring \
  psql -U monitoring -d monitoring \
  -c "SELECT * FROM window_consumption ORDER BY window_start_utc DESC LIMIT 20;"
```

---

### View active monitored devices

To confirm which devices the Monitoring Service is currently tracking:

```bash
docker exec -it postgres-monitoring \
  psql -U monitoring -d monitoring \
  -c "SELECT * FROM device_monitoring_ref;"
```


---

### View raw device readings flowing through Kafka

To check the actual telemetry being produced by the Device Simulator:

```bash
docker exec -it kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --topic device-readings \
    --from-beginning \
    --property print.key=true \
    --property print.value=true
```

---

### View device lifecycle events

To inspect events that synchronize device creation/deletion across services:

```bash
docker exec -it kafka \
  /opt/kafka/bin/kafka-console-consumer.sh \
    --bootstrap-server kafka:9092 \
    --topic device.monitoring \
    --from-beginning
```



