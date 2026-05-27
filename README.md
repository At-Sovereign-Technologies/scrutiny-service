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
* **Agregación de votos por candidato por mesa**
* **Consultas de totales globales y por mesa**
* **Publicación de resultados electorales agregados via Kafka al aprobar el escrutinio nacional**
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
6. Si se incluye desglose por candidato, se persiste en `candidate_votes`

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

* Se publica el evento `scrutiny.results.generated` con los totales agregados de votos por candidato de todas las mesas
* Este evento habilita la generación del E26 por parte del futuro `results-service`

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

### Resultados electorales generados

Topic:

```txt
scrutiny.results.generated
```

Publicado cuando el escrutinio ascendente alcanza la aprobación a nivel **NACIONAL**. Contiene los totales ya agregados de todas las mesas del país. Este evento es el punto de entrada para el futuro `results-service` y la generación del E26.

Payload de ejemplo:

```json
{
  "scrutinyLevel": "NATIONAL",
  "candidateResults": [
    {
      "candidateId": "C1",
      "candidateName": "Juan Perez",
      "party": "Partido Azul",
      "totalVotes": 1540
    },
    {
      "candidateId": "C2",
      "candidateName": "Maria Lopez",
      "party": "Partido Verde",
      "totalVotes": 1210
    }
  ]
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

Request básico (sin desglose por candidato):

```json
{
  "validVotes": 400,
  "blankVotes": 10,
  "nullVotes": 2,
  "unmarkedVotes": 1
}
```

Request extendido (con desglose por candidato):

```json
{
  "validVotes": 400,
  "blankVotes": 10,
  "nullVotes": 2,
  "unmarkedVotes": 1,
  "candidateVotes": [
    {
      "candidateId": "C1",
      "candidateName": "Juan Perez",
      "party": "Partido Azul",
      "votes": 220
    },
    {
      "candidateId": "C2",
      "candidateName": "Maria Lopez",
      "party": "Partido Verde",
      "votes": 180
    }
  ]
}
```

El campo `candidateVotes` es opcional. Si se omite, el flujo existente funciona sin cambios.

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

## Votos por Candidato

### Votos registrados por mesa

```http
GET /api/v1/candidate-votes/by-mesa/{mesaCode}
```

Retorna los registros individuales de votos por candidato para una mesa específica.

Respuesta de ejemplo:

```json
[
  {
    "id": 1,
    "mesaCode": "MESA-001",
    "candidateId": "C1",
    "candidateName": "Juan Perez",
    "party": "Partido Azul",
    "votes": 220,
    "createdAt": "2026-05-26T10:30:00"
  }
]
```

---

### Totales globales por candidato

```http
GET /api/v1/candidate-votes/aggregate
```

Retorna la sumatoria de votos por candidato en todas las mesas, ordenada de mayor a menor.

Respuesta de ejemplo:

```json
[
  {
    "candidateId": "C1",
    "candidateName": "Juan Perez",
    "party": "Partido Azul",
    "totalVotes": 1540
  },
  {
    "candidateId": "C2",
    "candidateName": "Maria Lopez",
    "party": "Partido Verde",
    "totalVotes": 1210
  }
]
```

---

### Totales por candidato en una mesa

```http
GET /api/v1/candidate-votes/aggregate/by-mesa/{mesaCode}
```

Retorna la sumatoria de votos por candidato filtrada para una mesa específica.

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
* **Publicación de resultados electorales agregados por candidato al aprobar escrutinio nacional**
* Auditoría distribuida

---

# Estructura del Proyecto

```txt
src/main/java/com/registraduria/scrutiny_service
│
├── candidate
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
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

## HU5 — Agregación de Votos por Candidato

* Entidad `CandidateVoteRecord` en tabla `candidate_votes` con timestamp `created_at`
* Campo opcional `candidateVotes` en cierre de mesa (compatibilidad con flujo existente)
* Persistencia de votos por candidato por mesa al cerrar la mesa
* Consulta de registros por mesa: `GET /api/v1/candidate-votes/by-mesa/{mesaCode}`
* Agregación global de votos por candidato: `GET /api/v1/candidate-votes/aggregate`
* Agregación por candidato filtrada por mesa: `GET /api/v1/candidate-votes/aggregate/by-mesa/{mesaCode}`
* Evento Kafka `scrutiny.results.generated` publicado al aprobar el escrutinio a nivel NACIONAL, con totales agregados de todas las mesas y campo `scrutinyLevel`
* Migración V7 de Flyway — tabla `candidate_votes` con índices sobre `mesa_code` y `candidate_id`
* Migración V8 de Flyway — columna `created_at` en `candidate_votes`

---