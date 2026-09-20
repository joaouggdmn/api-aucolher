-- ============================================================
-- AUcolher — Script de criação do banco de dados (PostgreSQL)
-- Banco: aucolher_db
--
-- Execute com o psql, conectado ao banco padrão "postgres":
--   psql -U postgres -h localhost -f script_banco_aucolher.sql
--
-- \gexec e \c são comandos do psql. No pgAdmin: crie o banco
-- aucolher_db pela interface, abra o Query Tool nele e rode a
-- partir da seção "Tabelas".
--
-- Espelha as entidades do Spring Boot (Usuario, MembroEquipe,
-- HorarioVisita) e a seção 6 de docs/regras-de-negocio.md.
-- Cópia mantida no backend em sql/schema.sql.
-- ============================================================

-- PostgreSQL não tem CREATE DATABASE IF NOT EXISTS: o SELECT só gera
-- o comando quando o banco ainda não existe, e o \gexec o executa.
-- template0 evita conflito de encoding com o template1 no Windows.
SELECT 'CREATE DATABASE aucolher_db WITH TEMPLATE = template0 ENCODING = ''UTF8'''
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'aucolher_db')\gexec

\c aucolher_db

-- ============================================================
-- Tabelas
-- ============================================================

-- ------------------------------------------------------------
-- usuarios
-- Os dois perfis (ONG e USUARIO_COMUM) e as duas formas de
-- autenticação (LOCAL e GOOGLE/OAuth2) na mesma tabela.
-- "Número de adoções realizadas" (seção 6.2) não é coluna: é
-- derivado dos registros de adoção.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS usuarios (
    id                   BIGSERIAL     PRIMARY KEY,
    nome                 VARCHAR(150)  NOT NULL,
    email                VARCHAR(150)  NOT NULL,
    senha                VARCHAR(255),
    tipo_usuario         VARCHAR(20)   NOT NULL,
    provider             VARCHAR(20)   NOT NULL DEFAULT 'LOCAL',
    ativo                BOOLEAN       NOT NULL DEFAULT TRUE,
    data_criacao         TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Perfil (comum e ONG)
    foto_url             TEXT,
    bio                  TEXT,

    -- Perfil de ONG
    cnpj                 VARCHAR(14),
    email_institucional  VARCHAR(150),
    is_verificado        BOOLEAN       NOT NULL DEFAULT FALSE,
    instagram            VARCHAR(30),
    twitter              VARCHAR(15),
    facebook             VARCHAR(255),

    -- Endereço (obrigatório para ONG, validado na API)
    cep                  VARCHAR(8),
    logradouro           VARCHAR(150),
    numero               VARCHAR(20),
    complemento          VARCHAR(100),
    bairro               VARCHAR(100),
    cidade               VARCHAR(100),
    estado               VARCHAR(2),

    CONSTRAINT uq_usuarios_email               UNIQUE (email),
    CONSTRAINT uq_usuarios_cnpj                UNIQUE (cnpj),
    CONSTRAINT chk_usuarios_tipo_usuario       CHECK (tipo_usuario IN ('ONG', 'USUARIO_COMUM')),
    CONSTRAINT chk_usuarios_provider           CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_usuarios_ong_possui_cnpj    CHECK (tipo_usuario <> 'ONG' OR cnpj IS NOT NULL),
    CONSTRAINT chk_usuarios_local_possui_senha CHECK (provider <> 'LOCAL' OR senha IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS idx_usuarios_tipo_usuario ON usuarios (tipo_usuario);

COMMENT ON TABLE  usuarios                     IS 'Usuários da plataforma: ONGs/abrigos e usuários comuns';
COMMENT ON COLUMN usuarios.senha               IS 'Hash BCrypt — nulo para contas criadas via OAuth2 (Google)';
COMMENT ON COLUMN usuarios.foto_url            IS 'URL do avatar ou data URL da imagem comprimida no frontend (enquanto não há upload próprio)';
COMMENT ON COLUMN usuarios.bio                 IS 'Bio do usuário comum (cuidadores autônomos) ou descrição da ONG';
COMMENT ON COLUMN usuarios.cnpj                IS 'Apenas os 14 dígitos — obrigatório quando tipo_usuario = ONG';
COMMENT ON COLUMN usuarios.email_institucional IS 'Contato público da ONG (opcional), pode diferir do e-mail de login';
COMMENT ON COLUMN usuarios.is_verificado       IS 'Selo de ONG verificada, concedido na aprovação pelo admin';
COMMENT ON COLUMN usuarios.instagram           IS 'Nome de usuário, sem @';
COMMENT ON COLUMN usuarios.twitter             IS 'Nome de usuário do X/Twitter, sem @';
COMMENT ON COLUMN usuarios.facebook            IS 'Link completo da página';
COMMENT ON COLUMN usuarios.cep                 IS 'Apenas os 8 dígitos';
COMMENT ON COLUMN usuarios.estado              IS 'Sigla da UF, ex: SC';

-- ------------------------------------------------------------
-- ong_equipe — "Equipe" do perfil de ONG (seção 6.2)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ong_equipe (
    usuario_id  BIGINT        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    ordem       INTEGER       NOT NULL,
    nome        VARCHAR(150)  NOT NULL,
    funcao      VARCHAR(100),

    PRIMARY KEY (usuario_id, ordem)
);

COMMENT ON TABLE ong_equipe IS 'Integrantes da equipe exibidos no perfil público da ONG, na ordem definida por "ordem"';

-- ------------------------------------------------------------
-- ong_horarios_visita — "Horário de visitas" (seção 6.2)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ong_horarios_visita (
    usuario_id  BIGINT       NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
    ordem       INTEGER      NOT NULL,
    dias        VARCHAR(80)  NOT NULL,
    horario     VARCHAR(80)  NOT NULL,

    PRIMARY KEY (usuario_id, ordem)
);

COMMENT ON TABLE ong_horarios_visita IS 'Faixas de horário de visita da ONG (ex: "Terça a sexta" / "14h às 18h")';

-- ============================================================
-- Opcional: trazer as contas de teste do banco antigo (adocao_db)
-- Rode no terminal, depois deste script:
--   pg_dump -U postgres -h localhost --data-only --table=usuarios adocao_db | psql -U postgres -h localhost -d aucolher_db
-- e em seguida, dentro do aucolher_db:
--   SELECT setval('usuarios_id_seq', (SELECT COALESCE(MAX(id), 1) FROM usuarios));
-- ============================================================
