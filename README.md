# 🧩 Distributed System – Authorization, User & Device Microservices

This project is a modular **microservices-based system** built with **Spring Boot**, **Kafka**, **PostgreSQL**, and **Traefik**.  
It implements a simple but realistic flow for **user registration, authentication, and data synchronization** across services.

---

## 🧱 Overview

### 💡 Purpose
The main goal of this project is to demonstrate how multiple Spring Boot services can:
- Communicate securely using **JWT authentication**.
- Synchronize data using **Apache Kafka**.
- Be exposed through a **central API Gateway (Traefik)**.
- Use **PostgreSQL databases** per service for separation of concerns.

### 🧩 Components
| Service | Description | Database | Communication |
|----------|-------------|-----------|----------------|
| **Authorization Service** | Manages user registration, login, credentials, and JWT validation. | CredentialDb | REST + Traefik ForwardAuth |
| **User Management Service** | Manages user profile data and emits events when users are created. | UserDb | REST (from Authorization) + Kafka |
| **Device Management Service** | Manages devices; subscribes to user creation events. | DeviceDb | Kafka |
| **Kafka** | Message broker for asynchronous communication. | — | — |
| **Traefik** | Reverse proxy and API Gateway; validates JWT via Authorization Service. | — | HTTP |

---

## ⚙️ Technologies Used
- **Java 17**
- **Spring Boot 3.x**
- **Spring Data JPA / Hibernate**
- **PostgreSQL**
- **Apache Kafka (KRaft mode)**
- **Traefik v3**
- **Docker & Docker Compose**
- **JWT (JSON Web Tokens)**
- **BCrypt** for password hashing

---

## 🔐 Security Design
- **Passwords** are hashed with BCrypt before being stored.
- **JWT** tokens are signed using **HS256** with a shared secret (`JWT_SECRET`).
- **Traefik** intercepts and validates JWTs using a `ForwardAuth` middleware.
- Backend services receive identity via headers (`X-User-Id`, `X-Role`) — they trust Traefik.

---

## 🧭 System Workflow

### Registration
1. The user registers on the **Frontend** → request goes to **Authorization Service** (`/api/auth/register`).
2. Authorization Service:
    - Calls **User Service** to create the user.
    - Gets back the generated `userId`.
    - Stores credentials in **CredentialDb** (`username`, `password_hash`, `role`, `userId`).
3. **User Service** publishes a Kafka event `user.created.v1` → **Device Service** consumes it and syncs data.
4. The user is redirected to login.

### Login
1. Frontend → **Authorization Service** (`/api/auth/login`) with `username`, `password`.
2. Authorization Service:
    - Validates password (BCrypt check).
    - Generates and returns a **JWT token**.
3. The frontend stores the token and attaches it to every API request:



### Token Validation
1. Every secured request goes through **Traefik**.
2. Traefik calls `authorization-service:/auth/validate` (ForwardAuth).
3. If valid → Traefik routes to the microservice and appends:



---

## 🧾 Environment Variables

| Variable | Description | Example |
|-----------|-------------|----------|
| `JWT_SECRET` | Secret key for signing/verifying JWTs | `a-very-strong-64-char-secret` |
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | `jdbc:postgresql://postgres-credential:5432/CredentialDb` |
| `SPRING_DATASOURCE_USERNAME` | DB username | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `Sebi1404` |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | Kafka broker address | `kafka:9092` |

---

## 🧱 Kafka Topics

| Topic | Producer | Consumer | Description |
|--------|-----------|-----------|--------------|
| `user.created.v1` | User Service | Device Service | Notifies when a new user is created. |

---

## 🗃️ Database Models

### CredentialDb (Authorization Service)
| Field | Type | Description |
|--------|------|-------------|
| id | Long | Primary key |
| userId | Long | Reference to UserDb |
| username | String | Unique username |
| passwordHash | String | Encrypted password |
| role | String | CLIENT / ADMIN |
| createdAt | Timestamp | Creation time |

### UserDb (User Service)
| Field | Type | Description |
|--------|------|-------------|
| id | Long | Primary key (userId) |
| username | String | Unique username |
| email | String | User’s email |
| role | String | User role |
| createdAt | Timestamp | Creation date |

---

## 🧰 How to Run the Project

### 1️⃣ Prerequisites
- Install **Docker** and **Docker Compose**
- Install **JDK 17+**
- Optional: Postman / curl for API testing

### 2️⃣ Build and Start All Services
From the project root:

```bash
docker compose up --build
