CREATE TABLE scrutiny_approvals (

    id BIGSERIAL PRIMARY KEY,

    scrutiny_hash VARCHAR(255),

    delegate_name VARCHAR(255),

    alerts VARCHAR(255),

    level VARCHAR(50),

    decision VARCHAR(50),

    created_at TIMESTAMP
);