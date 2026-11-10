
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (  
    id BIGSERIAL PRIMARY KEY,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    page_count INTEGER,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT now()

);