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
-- Banco criado pela versão anterior deste script (tabelas em
-- português)? Não rode este: aplique, em ordem, os scripts de
-- sql/migrations/ — eles renomeiam tudo sem perder os dados.
--
-- Espelha as entidades do Spring Boot (User, TeamMember,
-- VisitingHour) e a seção 6 de docs/regras-de-negocio.md.
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
-- users
-- Os dois perfis (NGO e PERSON) e as duas formas de
-- autenticação (LOCAL e GOOGLE/OAuth2) na mesma tabela.
-- "Número de adoções realizadas" (seção 6.2) não é coluna: é
-- derivado dos registros de adoção.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id                   BIGSERIAL     PRIMARY KEY,
    name                 VARCHAR(150)  NOT NULL,
    email                VARCHAR(150)  NOT NULL,
    password             VARCHAR(255),
    user_type            VARCHAR(20)   NOT NULL,
    provider             VARCHAR(20)   NOT NULL DEFAULT 'LOCAL',
    active               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Perfil (comum e ONG)
    photo_url            TEXT,
    bio                  TEXT,

    -- Perfil de ONG
    cnpj                 VARCHAR(14),
    institutional_email  VARCHAR(150),
    is_verified          BOOLEAN       NOT NULL DEFAULT FALSE,
    instagram            VARCHAR(30),
    twitter              VARCHAR(15),
    facebook             VARCHAR(255),
    founded_year         INTEGER,

    -- Endereço (obrigatório para ONG, validado na API)
    cep                  VARCHAR(8),
    street               VARCHAR(150),
    number               VARCHAR(20),
    complement           VARCHAR(100),
    district             VARCHAR(100),
    city                 VARCHAR(100),
    state                VARCHAR(2),

    CONSTRAINT uq_users_email                UNIQUE (email),
    CONSTRAINT uq_users_cnpj                 UNIQUE (cnpj),
    CONSTRAINT chk_users_user_type           CHECK (user_type IN ('NGO', 'PERSON')),
    CONSTRAINT chk_users_provider            CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_users_ngo_has_cnpj        CHECK (user_type <> 'NGO' OR cnpj IS NOT NULL),
    CONSTRAINT chk_users_local_has_password  CHECK (provider <> 'LOCAL' OR password IS NOT NULL),
    CONSTRAINT chk_users_founded_year        CHECK (founded_year IS NULL OR (user_type = 'NGO' AND founded_year >= 1800))
);

CREATE INDEX IF NOT EXISTS idx_users_user_type ON users (user_type);

COMMENT ON TABLE  users                     IS 'Usuários da plataforma: ONGs/abrigos e usuários comuns';
COMMENT ON COLUMN users.password            IS 'Hash BCrypt — nulo para contas criadas via OAuth2 (Google)';
COMMENT ON COLUMN users.photo_url           IS 'URL do avatar ou data URL da imagem comprimida no frontend (enquanto não há upload próprio)';
COMMENT ON COLUMN users.bio                 IS 'Bio do usuário comum (cuidadores autônomos) ou descrição da ONG';
COMMENT ON COLUMN users.cnpj                IS 'Apenas os 14 dígitos — obrigatório quando user_type = NGO';
COMMENT ON COLUMN users.institutional_email IS 'Contato público da ONG (opcional), pode diferir do e-mail de login';
COMMENT ON COLUMN users.is_verified         IS 'Selo de ONG verificada, concedido na aprovação pelo admin';
COMMENT ON COLUMN users.instagram           IS 'Nome de usuário, sem @';
COMMENT ON COLUMN users.twitter             IS 'Nome de usuário do X/Twitter, sem @';
COMMENT ON COLUMN users.facebook            IS 'Link completo da página';
COMMENT ON COLUMN users.founded_year        IS 'Ano de fundação da ONG (opcional) — "Fundada em [ano]" no perfil; nulo para usuário comum';
COMMENT ON COLUMN users.cep                 IS 'Apenas os 8 dígitos';
COMMENT ON COLUMN users.state               IS 'Sigla da UF, ex: SC';

-- ------------------------------------------------------------
-- ngo_team — "Equipe" do perfil de ONG (seção 6.2)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ngo_team (
    user_id     BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sort_order  INTEGER       NOT NULL,
    name        VARCHAR(150)  NOT NULL,
    role        VARCHAR(100),

    PRIMARY KEY (user_id, sort_order)
);

COMMENT ON TABLE ngo_team IS 'Integrantes da equipe exibidos no perfil público da ONG, na ordem definida por "sort_order"';

-- ------------------------------------------------------------
-- ngo_visiting_hours — "Horário de visitas" (seção 6.2)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS ngo_visiting_hours (
    user_id     BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    sort_order  INTEGER      NOT NULL,
    days        VARCHAR(80)  NOT NULL,
    hours       VARCHAR(80)  NOT NULL,

    PRIMARY KEY (user_id, sort_order)
);

COMMENT ON TABLE ngo_visiting_hours IS 'Faixas de horário de visita da ONG (ex: "Terça a sexta" / "14h às 18h")';

-- ------------------------------------------------------------
-- animais — anúncios de adoção (ONG ou usuário comum)
-- Cidade/UF não ficam aqui: o animal está onde o dono está, então
-- a localização vem de users via dono_id.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS animais (
    id                      BIGSERIAL     PRIMARY KEY,
    dono_id                 BIGINT        NOT NULL REFERENCES users (id) ON DELETE CASCADE,

    -- Dados básicos
    nome                    VARCHAR(60)   NOT NULL,
    especie                 VARCHAR(20)   NOT NULL,
    raca                    VARCHAR(60)   NOT NULL,
    sexo                    VARCHAR(10)   NOT NULL,
    idade_valor             INTEGER       NOT NULL,
    idade_unidade           VARCHAR(10)   NOT NULL,
    faixa_etaria            VARCHAR(10)   NOT NULL,
    porte                   VARCHAR(10)   NOT NULL,

    -- Saúde
    vacinado                BOOLEAN       NOT NULL DEFAULT FALSE,
    castrado                BOOLEAN       NOT NULL DEFAULT FALSE,
    vermifugado             BOOLEAN       NOT NULL DEFAULT FALSE,
    necessidades_especiais  BOOLEAN       NOT NULL DEFAULT FALSE,

    -- Comportamento e compatibilidade
    nivel_energia           VARCHAR(10)   NOT NULL,
    temperamento            VARCHAR(20)   NOT NULL,
    nivel_independencia     VARCHAR(10)   NOT NULL,
    nivel_vocalizacao       VARCHAR(10)   NOT NULL,
    bom_com_criancas        BOOLEAN       NOT NULL,
    bom_com_caes            BOOLEAN       NOT NULL,
    bom_com_gatos           BOOLEAN       NOT NULL,
    adaptado_apartamento    BOOLEAN       NOT NULL,

    -- Anúncio
    resumo                  VARCHAR(200)  NOT NULL,
    historia                TEXT          NOT NULL,
    status                  VARCHAR(20)   NOT NULL DEFAULT 'DISPONIVEL',
    data_criacao            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    data_atualizacao        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_animais_especie        CHECK (especie IN ('CACHORRO', 'GATO', 'OUTRO')),
    CONSTRAINT chk_animais_sexo           CHECK (sexo IN ('MACHO', 'FEMEA')),
    CONSTRAINT chk_animais_porte          CHECK (porte IN ('PEQUENO', 'MEDIO', 'GRANDE')),
    CONSTRAINT chk_animais_idade_unidade  CHECK (idade_unidade IN ('ANOS', 'MESES')),
    CONSTRAINT chk_animais_idade          CHECK ((idade_unidade = 'MESES' AND idade_valor BETWEEN 0 AND 11)
                                              OR (idade_unidade = 'ANOS'  AND idade_valor BETWEEN 1 AND 30)),
    CONSTRAINT chk_animais_faixa_etaria   CHECK (faixa_etaria IN ('FILHOTE', 'ADULTO', 'IDOSO')),
    CONSTRAINT chk_animais_niveis         CHECK (nivel_energia       IN ('BAIXO', 'MODERADO', 'ALTO')
                                             AND nivel_independencia IN ('BAIXO', 'MODERADO', 'ALTO')
                                             AND nivel_vocalizacao   IN ('BAIXO', 'MODERADO', 'ALTO')),
    CONSTRAINT chk_animais_temperamento   CHECK (temperamento IN ('CALMO', 'BRINCALHAO', 'AFETUOSO', 'PROTETOR', 'INDEPENDENTE')),
    CONSTRAINT chk_animais_status         CHECK (status IN ('DISPONIVEL', 'ADOTADO', 'INATIVO'))
);

CREATE INDEX IF NOT EXISTS idx_animais_status_data ON animais (status, data_criacao DESC);
CREATE INDEX IF NOT EXISTS idx_animais_dono        ON animais (dono_id);

COMMENT ON TABLE  animais               IS 'Animais anunciados para adoção por ONGs e usuários comuns';
COMMENT ON COLUMN animais.dono_id       IS 'Quem anunciou — a cidade/UF exibidas no anúncio são as do perfil do dono';
COMMENT ON COLUMN animais.idade_valor   IS 'Idade informada no cadastro, na unidade de idade_unidade (0-11 meses ou 1-30 anos)';
COMMENT ON COLUMN animais.faixa_etaria  IS 'Calculada pela API ao salvar: < 12 meses FILHOTE, < 8 anos ADULTO, senão IDOSO';
COMMENT ON COLUMN animais.resumo        IS 'Frase curta exibida no card da listagem';
COMMENT ON COLUMN animais.status        IS 'DISPONIVEL, ADOTADO ou INATIVO (retirado do ar pelo dono — exclusão lógica)';

-- ------------------------------------------------------------
-- animal_fotos — até 4 fotos por animal, na ordem de exibição
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS animal_fotos (
    animal_id  BIGINT   NOT NULL REFERENCES animais (id) ON DELETE CASCADE,
    ordem      INTEGER  NOT NULL,
    url        TEXT     NOT NULL,

    PRIMARY KEY (animal_id, ordem)
);

COMMENT ON TABLE  animal_fotos       IS 'Fotos do anúncio; ordem 0 é a capa usada no card da listagem';
COMMENT ON COLUMN animal_fotos.url   IS 'URL da imagem ou data URL comprimida no frontend (enquanto não há upload próprio)';

-- ------------------------------------------------------------
-- favoritos — animais salvos por cada usuário
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS favoritos (
    id            BIGSERIAL  PRIMARY KEY,
    usuario_id    BIGINT     NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    animal_id     BIGINT     NOT NULL REFERENCES animais (id)  ON DELETE CASCADE,
    data_criacao  TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_favoritos_usuario_animal UNIQUE (usuario_id, animal_id)
);

CREATE INDEX IF NOT EXISTS idx_favoritos_usuario ON favoritos (usuario_id, data_criacao DESC);

COMMENT ON TABLE favoritos IS 'Animais favoritados pelos usuários — um registro por par usuário/animal';

-- ============================================================
-- Opcional: trazer as contas de teste do banco antigo (adocao_db)
-- Rode no terminal, depois deste script:
--   pg_dump -U postgres -h localhost --data-only --table=usuarios adocao_db | psql -U postgres -h localhost -d aucolher_db
-- e em seguida, dentro do aucolher_db:
--   SELECT setval('usuarios_id_seq', (SELECT COALESCE(MAX(id), 1) FROM usuarios));
-- O adocao_db tem colunas em português: faça a importação num banco ainda na
-- versão em português deste script e só depois aplique sql/migrations/.
-- ============================================================
