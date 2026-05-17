CREATE TABLE mesas (

    id BIGSERIAL PRIMARY KEY,

    mesa_code VARCHAR(100) NOT NULL,

    valid_votes INTEGER NOT NULL,

    blank_votes INTEGER NOT NULL,

    null_votes INTEGER NOT NULL,

    unmarked_votes INTEGER NOT NULL,

    status VARCHAR(20) NOT NULL
);