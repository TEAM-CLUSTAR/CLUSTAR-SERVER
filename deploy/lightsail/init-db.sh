#!/usr/bin/env bash
set -euo pipefail

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set ON_ERROR_STOP=1 <<'SQL'
BEGIN;
\getenv app_password DB_PASSWORD
CREATE ROLE clustar LOGIN PASSWORD :'app_password';
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS hstore;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
GRANT USAGE, CREATE ON SCHEMA public TO clustar;

SET ROLE clustar;
CREATE TABLE IF NOT EXISTS public.vector_store (
    id uuid PRIMARY KEY DEFAULT uuid_generate_v4(),
    content text,
    metadata json,
    embedding vector(768)
);
CREATE INDEX IF NOT EXISTS spring_ai_vector_index
    ON public.vector_store USING hnsw (embedding vector_cosine_ops);

CREATE TABLE IF NOT EXISTS public.spring_ai_chat_memory (
    conversation_id varchar(36) NOT NULL,
    content text NOT NULL,
    type varchar(10) NOT NULL CHECK (type IN ('USER', 'ASSISTANT', 'SYSTEM', 'TOOL')),
    "timestamp" timestamp NOT NULL
);
CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
    ON public.spring_ai_chat_memory (conversation_id, "timestamp");
COMMIT;
SQL
