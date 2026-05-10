CREATE TABLE e14_records (
    id BIGSERIAL PRIMARY KEY,
    mesa_code VARCHAR(255) NOT NULL,
    municipality VARCHAR(255) NOT NULL,
    pdf_hash VARCHAR(5000),
    signed BOOLEAN,
    published BOOLEAN,
    created_at TIMESTAMP
);