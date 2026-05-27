-- =============================================================================
-- SR-M5: Aprobación del Escrutinio General y Acta E-26 Oficial e Inmutable
-- =============================================================================
-- Este script es ADITIVO: no modifica ninguna tabla existente de SR-M4.

-- -----------------------------------------------------------------------------
-- Escrutinio general (agregado nacional proveniente de SR-M4).
-- Estados: CERRADO (resultados listos para aprobacion) -> EN_APROBACION -> OFICIAL
-- -----------------------------------------------------------------------------
CREATE TABLE general_scrutiny (

    id               BIGSERIAL    PRIMARY KEY,

    scrutiny_code    VARCHAR(100) NOT NULL UNIQUE,

    status           VARCHAR(30)  NOT NULL,

    electoral_method VARCHAR(50)  NOT NULL,

    seats            INTEGER      NOT NULL,

    required_quorum  INTEGER      NOT NULL,

    -- SHA-256 (64 hex) del estado cerrado que los magistrados aprueban (CA-1).
    state_hash       VARCHAR(64)  NOT NULL,

    created_at       TIMESTAMP    NOT NULL,

    officialized_at  TIMESTAMP
);

CREATE INDEX idx_general_scrutiny_code ON general_scrutiny (scrutiny_code);

-- -----------------------------------------------------------------------------
-- Auditoria append-only de aprobaciones de magistrados (CA-1).
-- Registra: magistrado, timestamp UTC, IP de origen y hash del estado aprobado.
-- -----------------------------------------------------------------------------
CREATE TABLE magistrate_approvals (

    id                  BIGSERIAL    PRIMARY KEY,

    general_scrutiny_id BIGINT       NOT NULL REFERENCES general_scrutiny (id),

    magistrate_id       VARCHAR(100) NOT NULL,

    magistrate_name     VARCHAR(255) NOT NULL,

    approved_state_hash VARCHAR(64)  NOT NULL,

    -- Firma digital RSA (base64) del magistrado sobre el estado aprobado.
    signature           VARCHAR(1024) NOT NULL,

    ip_address          VARCHAR(45)  NOT NULL,   -- soporta IPv4 e IPv6

    approved_at_utc     TIMESTAMP    NOT NULL,   -- siempre en UTC

    created_at          TIMESTAMP    NOT NULL,

    -- Un magistrado solo puede aprobar una vez el mismo escrutinio.
    CONSTRAINT uq_magistrate_per_scrutiny
        UNIQUE (general_scrutiny_id, magistrate_id)
);

CREATE INDEX idx_magistrate_approvals_scrutiny
    ON magistrate_approvals (general_scrutiny_id);

-- -----------------------------------------------------------------------------
-- Acta E-26 oficial e INMUTABLE (CA-2, CA-3).
-- Almacenamiento Write-Once: los triggers de abajo prohiben UPDATE y DELETE
-- a nivel de base de datos, garantizando inmutabilidad incluso fuera de la app.
-- -----------------------------------------------------------------------------
CREATE TABLE acta_oficial (

    id                   BIGSERIAL    PRIMARY KEY,

    -- Numeracion oficial secuencial (ej. E26-000001).
    acta_number          VARCHAR(50)  NOT NULL UNIQUE,

    general_scrutiny_id  BIGINT       NOT NULL REFERENCES general_scrutiny (id),

    source_scrutiny_code VARCHAR(100) NOT NULL,

    electoral_method     VARCHAR(50)  NOT NULL,

    -- Siempre OFICIAL_INMUTABLE una vez generada.
    status               VARCHAR(30)  NOT NULL,

    -- Resultado canonico serializado (JSON) sobre el que se calcula el hash.
    content_json         TEXT         NOT NULL,

    -- SHA-256 (64 hex) del contenido; se reverifica en cada lectura (CA-3).
    content_hash         VARCHAR(64)  NOT NULL,

    pdf_path             VARCHAR(512) NOT NULL,   -- PDF/A-3 archivistico

    xml_path             VARCHAR(512) NOT NULL,   -- XML firmado (XML-DSig / XAdES-BES)

    generated_at         TIMESTAMP    NOT NULL,

    created_at           TIMESTAMP    NOT NULL
);

CREATE INDEX idx_acta_oficial_scrutiny ON acta_oficial (general_scrutiny_id);

-- Write-Once / append-only: rechaza cualquier intento de mutar o borrar un acta.
CREATE OR REPLACE FUNCTION reject_acta_oficial_mutation()
    RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION
        'acta_oficial es inmutable (append-only): operacion % no permitida', TG_OP;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_acta_oficial_no_update
    BEFORE UPDATE ON acta_oficial
    FOR EACH ROW EXECUTE FUNCTION reject_acta_oficial_mutation();

CREATE TRIGGER trg_acta_oficial_no_delete
    BEFORE DELETE ON acta_oficial
    FOR EACH ROW EXECUTE FUNCTION reject_acta_oficial_mutation();

-- -----------------------------------------------------------------------------
-- Estado de publicacion del portal publico (CA-4).
-- Antes de oficializar: PRELIMINAR. Tras generar el acta: RESULTADOS OFICIALES.
-- -----------------------------------------------------------------------------
CREATE TABLE portal_publication (

    id             BIGSERIAL    PRIMARY KEY,

    scrutiny_code  VARCHAR(100) NOT NULL UNIQUE,

    official       BOOLEAN      NOT NULL DEFAULT FALSE,

    badge_text     VARCHAR(100) NOT NULL,

    acta_permalink VARCHAR(512),

    updated_at     TIMESTAMP    NOT NULL
);

CREATE INDEX idx_portal_publication_code ON portal_publication (scrutiny_code);
