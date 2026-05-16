# Scrutiny Service

## Descripción

Scrutiny Service es un microservicio encargado de gestionar actas electorales E14 dentro del proceso de escrutinio.

El servicio administra la creación, firma digital y publicación oficial de registros electorales.

Además, valida cuarentenas institucionales mediante gRPC y consume eventos distribuidos mediante Kafka.

---

# Responsabilidades

* Crear registros E14
* Gestionar estado de actas electorales
* Aplicar firma digital
* Publicar resultados oficiales
* Bloquear publicación si existe cuarentena institucional
* Consumir eventos Kafka
* Integrarse con Dispute Service mediante gRPC

---

# Arquitectura

El microservicio implementa:

* Microservicios
* CQRS
* Event-Driven Architecture
* gRPC Client
* REST API
* PostgreSQL
* Flyway
* Kafka Consumer

---

# Tecnologías

* Java 21
* Spring Boot 3
* Spring Data JPA
* PostgreSQL
* Flyway
* Spring Kafka
* gRPC
* Maven
* Docker
* Swagger OpenAPI

---

# Estructura del Proyecto

```text
src/main/java
├── command
├── query
├── domain
├── infrastructure
├── events
├── grpc
└── config
```

---

# Variables de Entorno

Archivo `.env`:

```env
SERVER_PORT=8081

POSTGRES_DB=scrutiny_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
```

---

# Dependencias Externas

El servicio requiere:

* PostgreSQL
* Kafka
* Dispute Service

---

# Configuración Kafka

Kafka se ejecuta desde la carpeta compartida `infrastructure`.

## Levantar Kafka

```bash
docker compose up -d
```

---

# Base de Datos

PostgreSQL es utilizado como base de datos principal.

Las migraciones son administradas con Flyway.

---

# Ejecutar el Proyecto

## 1. Exportar variables

```bash
export $(grep -v '^#' .env | xargs)
```

## 2. Ejecutar servicio

```bash
./mvnw spring-boot:run
```

---

# Swagger

```text
http://localhost:8081/swagger-ui
```

---

# REST Endpoints

## Crear E14

```http
POST /api/v1/e14
```

Body:

```json
{
  "mesaCode": "MESA-001",
  "municipality": "Bogota",
  "pdfHash": "HASH_TEST"
}
```

---

## Firmar E14

```http
PATCH /api/v1/e14/{id}/sign
```

Body:

```json
{
  "digitalSignature": "SIGN_TEST"
}
```

---

## Publicar E14

```http
PATCH /api/v1/e14/{id}/publish
```

La publicación será bloqueada si la mesa se encuentra en cuarentena institucional.

---

# gRPC

El servicio consume comunicación gRPC desde Dispute Service.

## Servicio consumido

```text
GetQuarantinedMesaCodes
```

## Puerto utilizado

```text
9090
```

---

# Kafka

## Topic consumido

```text
dispute.created
```

## Evento consumido

```json
{
  "id": 1,
  "mesaCode": "MESA-001",
  "witnessName": "Carlos Perez",
  "reason": "Conteo inconsistente",
  "createdAt": "2026-05-16T17:00:00"
}
```

---

# Flujo General

1. Se crea un E14
2. El registro queda en estado DRAFT
3. El E14 es firmado digitalmente
4. Antes de publicar, el servicio consulta cuarentenas mediante gRPC
5. Si existe bloqueo institucional, la publicación es rechazada
6. Si la disputa fue resuelta, el E14 puede publicarse
7. El servicio consume eventos distribuidos desde Kafka

---

# Estados del E14

```text
DRAFT
SIGNED
PUBLISHED
```

---

# Estado Actual

Implementación funcional con:

* CQRS
* gRPC Client
* Kafka Consumer
* PostgreSQL
* Flyway
* Swagger
* Comunicación distribuida
* Validación institucional
