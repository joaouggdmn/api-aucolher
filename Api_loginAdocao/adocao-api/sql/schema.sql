-- ============================================================
-- Sistema de Adoção de Animais - Script de criação do banco
-- ============================================================

-- Execute esta primeira parte conectado ao banco padrão (ex: postgres)
CREATE DATABASE adocao_db
    WITH ENCODING = 'UTF8';

-- Conecte-se ao banco recém-criado antes de rodar o restante do script
-- (no psql: \c adocao_db)
\c adocao_db;

-- ============================================================
-- Tabela: usuarios
-- Suporta os dois perfis de cadastro (ONG e USUARIO_COMUM) e as
-- duas formas de autenticação (LOCAL e GOOGLE/OAuth2).
-- ============================================================
CREATE TABLE IF NOT EXISTS usuarios (
    id              BIGSERIAL PRIMARY KEY,
    nome            VARCHAR(150) NOT NULL,
    email           VARCHAR(150) NOT NULL UNIQUE,
    senha           VARCHAR(255),                  -- hash BCrypt; nulo para contas 100% OAuth2
    cnpj            VARCHAR(18) UNIQUE,             -- obrigatório apenas para ONG
    tipo_usuario    VARCHAR(20) NOT NULL,
    provider        VARCHAR(20) NOT NULL DEFAULT 'LOCAL',
    ativo           BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tipo_usuario        CHECK (tipo_usuario IN ('ONG', 'USUARIO_COMUM')),
    CONSTRAINT chk_provider            CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_ong_possui_cnpj     CHECK (tipo_usuario <> 'ONG' OR cnpj IS NOT NULL),
    CONSTRAINT chk_local_possui_senha  CHECK (provider <> 'LOCAL' OR senha IS NOT NULL)
);

-- Índices para otimizar as buscas mais comuns (login e validação de duplicidade)
CREATE INDEX IF NOT EXISTS idx_usuarios_email ON usuarios (email);
CREATE INDEX IF NOT EXISTS idx_usuarios_cnpj  ON usuarios (cnpj);
CREATE INDEX IF NOT EXISTS idx_usuarios_tipo  ON usuarios (tipo_usuario);

COMMENT ON TABLE usuarios IS 'Usuários do Sistema de Adoção de Animais (ONGs e Usuários Comuns)';
COMMENT ON COLUMN usuarios.senha IS 'Hash BCrypt da senha - nulo para usuários autenticados apenas via OAuth2 (Google)';
COMMENT ON COLUMN usuarios.cnpj IS 'CNPJ apenas com dígitos (14 caracteres) - obrigatório apenas quando tipo_usuario = ONG';
