CREATE TABLE candidate_votes (

    id BIGSERIAL PRIMARY KEY,

    mesa_code VARCHAR(100) NOT NULL,

    candidate_id VARCHAR(100) NOT NULL,

    candidate_name VARCHAR(255) NOT NULL,

    party VARCHAR(255) NOT NULL,

    votes INTEGER NOT NULL
);

CREATE INDEX idx_candidate_votes_mesa_code
    ON candidate_votes (mesa_code);

CREATE INDEX idx_candidate_votes_candidate_id
    ON candidate_votes (candidate_id);
