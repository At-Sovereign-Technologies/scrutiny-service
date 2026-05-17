CREATE TABLE vvpat_scans (

    id BIGSERIAL PRIMARY KEY,

    mesa_code VARCHAR(100) NOT NULL,

    jurado_id VARCHAR(100) NOT NULL,

    physical_votes INTEGER NOT NULL,

    result VARCHAR(30) NOT NULL,

    attempt INTEGER NOT NULL,

    scanned_at TIMESTAMP
);