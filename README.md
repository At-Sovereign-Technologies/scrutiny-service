# Scrutiny Service

## Descripción General

Scrutiny Service es un microservicio desarrollado con Spring Boot encargado del proceso de escrutinio y consolidación electoral.

El servicio implementa:

* Gestión de mesas electorales
* Generación nativa del Acta E14
* Generación de PDF y hash SHA-256
* Validación de doble verdad VVPAT
* Cuarentena automática de mesas
* Aprobación de escrutinio ascendente
* Comunicación distribuida mediante Kafka
* Comunicación gRPC con dispute-service
* Persistencia en PostgreSQL
* Migraciones con Flyway

---

# Arquitectura

## Tecnologías utilizadas

* Java 21
* Spring Boot 3
* Spring Data JPA
* PostgreSQL
* Flyway
* Apache Kafka
* gRPC
* OpenPDF
* Docker
* Swagger OpenAPI

---

# Funcionalidades Principales

## Gestión de Mesas

El sistema permite:

* Crear mesas
* Cerrar mesas
* Consolidar votos
* Generar registros E14
* Generar PDFs nativos
* Generar hashes SHA-256

---

## Flujo E14

Cuando una mesa es cerrada:

1. Se consolidan los votos
2. Se genera un PDF E14 nativo
3. Se genera un hash SHA-256 del PDF
4. Se almacena un registro E14 en PostgreSQL
5. El E14 queda en estado DRAFT

Los PDFs generados se almacenan en:

```txt
/generated-pdfs
```

---

## Validación de Doble Verdad VVPAT

El sistema valida el conteo físico contra el conteo digital.

### Flujo

1. El jurado registra el conteo físico
2. El sistema compara votos físicos vs digitales
3. Se genera MATCH o MISMATCH
4. Se publica un evento Kafka en caso de discrepancia
5. Después de 3 discrepancias la mesa entra automáticamente en cuarentena

### Topic Kafka

```txt
vvpat.mismatch
```

---

## Cuarentena Automática

Las mesas son marcadas automáticamente en cuarentena después de múltiples discrepancias.

Condiciones:

* 3 intentos fallidos
* Los votos físicos no coinciden con los digitales

Resultado:

```txt
quarantined = true
```

registrado dentro de la mesa.

---

## Aprobación de Escrutinio Ascendente

El sistema soporta aprobación ascendente:

```txt
MUNICIPAL
DEPARTAMENTAL
NACIONAL
```

Los delegados pueden:

* Aprobar escrutinio
* Solicitar recuento
* Registrar alertas
* Generar hashes de integridad

### Topic Kafka

```txt
scrutiny.approved
```

Cuando se aprueba el nivel NACIONAL:

```txt
E26 ENABLED
```

es emitido dentro del flujo.

---

# Comunicación entre Microservicios

## gRPC

El servicio se comunica con dispute-service mediante gRPC.

Objetivos:

* Consultar mesas en cuarentena
* Validar disputas antes de publicación

---

# Eventos Kafka

## Eventos Producidos

### Discrepancia VVPAT

Topic:

```txt
vvpat.mismatch
```

Payload de ejemplo:

```json
{
  "mesaCode": "MESA-999",
  "digitalVotes": 400,
  "physicalVotes": 390,
  "attempt": 2
}
```

---

### Escrutinio aprobado

Topic:

```txt
scrutiny.approved
```

Payload de ejemplo:

```json
{
  "level": "NATIONAL",
  "delegateName": "Carlos Mendoza",
  "scrutinyHash": "abc123..."
}
```

---

# Endpoints REST

## Mesas

### Crear mesa

```http
POST /api/v1/mesas
```

Request:

```json
{
  "mesaCode": "MESA-999"
}
```

---

### Cerrar mesa

```http
PATCH /api/v1/mesas/{id}/close
```

Request:

```json
{
  "validVotes": 400,
  "blankVotes": 10,
  "nullVotes": 2,
  "unmarkedVotes": 1
}
```

---

## VVPAT

### Escaneo VVPAT

```http
POST /api/v1/vvpat/scan
```

Request:

```json
{
  "mesaCode": "MESA-999",
  "juradoId": "JURADO-1",
  "physicalVotes": 390
}
```

---

## Escrutinio Ascendente

### Aprobar escrutinio

```http
POST /api/v1/scrutiny/approve
```

Request:

```json
{
  "level": "NATIONAL",
  "decision": "APPROVED",
  "delegateName": "Carlos Mendoza",
  "alerts": "No active alerts"
}
```

---

# Migraciones de Base de Datos

Administradas mediante Flyway.

Ubicación:

```txt
src/main/resources/db/migration
```

---

# Variables de Entorno

Ejemplo `.env`:

```env
SERVER_PORT=8081

POSTGRES_DB=scrutiny_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=postgres
POSTGRES_PORT=5432
```

Cargar variables:

```bash
export $(grep -v '^#' .env | xargs)
```

---

# Ejecución del Servicio

## Levantar infraestructura

```bash
docker compose up -d
```

---

## Ejecutar servicio

```bash
./mvnw spring-boot:run
```

---

# Swagger

Disponible en:

```txt
http://localhost:8081/swagger-ui
```

---

# Kafka UI

Disponible en:

```txt
http://localhost:8085
```

---

# Arquitectura CQRS y Event-Driven

El servicio implementa patrones CQRS y Event-Driven.

## CQRS

* Los comandos gestionan escritura y ejecución de flujos
* Las consultas gestionan lectura y reportes

## Event-Driven

Kafka se utiliza para:

* Propagación de discrepancias VVPAT
* Propagación de aprobaciones de escrutinio
* Auditoría distribuida

---

# Estructura del Proyecto

```txt
src/main/java/com/registraduria/scrutiny_service
│
├── command
├── query
├── mesa
├── vvpat
├── scrutiny
├── events
├── grpc
├── pdf
└── domain
```

---

# Historias de Usuario Implementadas

## US-SR-M4-01

* Cierre de mesa
* Generación nativa E14
* Generación PDF
* Hash SHA-256

## US-SR-M4-02

* Validación doble verdad VVPAT
* Eventos Kafka de discrepancia
* Cuarentena automática

## US-SR-M4-05

* Escrutinio ascendente
* Flujo de aprobación nacional
* Eventos Kafka de aprobación
* Activación de E26

---