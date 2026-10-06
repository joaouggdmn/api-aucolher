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
    ano_fundacao         INTEGER,

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

-- Colunas que chegaram depois da primeira versão do script. Em um banco já
-- criado, o CREATE TABLE IF NOT EXISTS acima não faz nada — os ALTER abaixo
-- acrescentam o que falta (sem isso a API não sobe: ddl-auto=validate).
-- São idempotentes: rodar o script de novo não quebra nada.
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS ano_fundacao INTEGER;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS chk_usuarios_ano_fundacao;
ALTER TABLE usuarios ADD CONSTRAINT chk_usuarios_ano_fundacao
    CHECK (ano_fundacao IS NULL OR (tipo_usuario = 'ONG' AND ano_fundacao >= 1800));

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
COMMENT ON COLUMN usuarios.ano_fundacao        IS 'Ano de fundação da ONG (opcional) — "Fundada em [ano]" no perfil; nulo para usuário comum';
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

-- ------------------------------------------------------------
-- animais — anúncios de adoção (ONG ou usuário comum)
-- Cidade/UF não ficam aqui: o animal está onde o dono está, então
-- a localização vem de usuarios via dono_id.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS animais (
    id                      BIGSERIAL     PRIMARY KEY,
    dono_id                 BIGINT        NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,

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
    usuario_id    BIGINT     NOT NULL REFERENCES usuarios (id) ON DELETE CASCADE,
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
-- ============================================================
