--
-- PostgreSQL database dump
--

\restrict gTs7EEP18cXwwL9PSgC0MfR4l3ihMKArsxsLxqhsUb6PIFg25Zh25QFBwQim4fq

-- Dumped from database version 18.4
-- Dumped by pg_dump version 18.4

-- Started on 2026-08-10 21:52:43

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 220 (class 1259 OID 16418)
-- Name: usuarios; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.usuarios (
    id bigint NOT NULL,
    email character varying(150) NOT NULL,
    senha character varying(255),
    tipo_usuario character varying(20) NOT NULL,
    nome character varying(150) NOT NULL,
    telefone_whatsapp character varying(20),
    cnpj character varying(18),
    is_verificado boolean DEFAULT false NOT NULL,
    ativo boolean DEFAULT true NOT NULL,
    data_criacao timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    provider character varying(20) DEFAULT 'LOCAL'::character varying NOT NULL,
    CONSTRAINT chk_usuarios_cnpj_por_tipo CHECK (((((tipo_usuario)::text = 'ONG'::text) AND (cnpj IS NOT NULL)) OR (((tipo_usuario)::text = 'PESSOA'::text) AND (cnpj IS NULL)))),
    CONSTRAINT chk_usuarios_provider CHECK (((provider)::text = ANY ((ARRAY['LOCAL'::character varying, 'GOOGLE'::character varying])::text[]))),
    CONSTRAINT chk_usuarios_senha_por_provider CHECK (((((provider)::text = 'LOCAL'::text) AND (senha IS NOT NULL)) OR (((provider)::text = 'GOOGLE'::text) AND (senha IS NULL)))),
    CONSTRAINT chk_usuarios_tipo_usuario CHECK (((tipo_usuario)::text = ANY ((ARRAY['PESSOA'::character varying, 'ONG'::character varying])::text[])))
);


ALTER TABLE public.usuarios OWNER TO postgres;

--
-- TOC entry 4926 (class 0 OID 0)
-- Dependencies: 220
-- Name: TABLE usuarios; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON TABLE public.usuarios IS 'Usuários da plataforma: Pessoas físicas (adotantes) e ONGs/Abrigos';


--
-- TOC entry 4927 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN usuarios.tipo_usuario; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.usuarios.tipo_usuario IS 'Valores permitidos: PESSOA ou ONG';


--
-- TOC entry 4928 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN usuarios.cnpj; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.usuarios.cnpj IS 'Obrigatório para ONG, sempre NULL para Pessoa (ver chk_usuarios_cnpj_por_tipo)';


--
-- TOC entry 4929 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN usuarios.is_verificado; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.usuarios.is_verificado IS 'Selo de verificação exibido no frontend (borda dourada nos cards de ONG)';


--
-- TOC entry 4930 (class 0 OID 0)
-- Dependencies: 220
-- Name: COLUMN usuarios.provider; Type: COMMENT; Schema: public; Owner: postgres
--

COMMENT ON COLUMN public.usuarios.provider IS 'LOCAL (email/senha) ou GOOGLE (OAuth2)';


--
-- TOC entry 219 (class 1259 OID 16417)
-- Name: usuarios_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.usuarios_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.usuarios_id_seq OWNER TO postgres;

--
-- TOC entry 4931 (class 0 OID 0)
-- Dependencies: 219
-- Name: usuarios_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.usuarios_id_seq OWNED BY public.usuarios.id;


--
-- TOC entry 4755 (class 2604 OID 16421)
-- Name: usuarios id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios ALTER COLUMN id SET DEFAULT nextval('public.usuarios_id_seq'::regclass);


--
-- TOC entry 4920 (class 0 OID 16418)
-- Dependencies: 220
-- Data for Name: usuarios; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.usuarios (id, email, senha, tipo_usuario, nome, telefone_whatsapp, cnpj, is_verificado, ativo, data_criacao, provider) FROM stdin;
\.


--
-- TOC entry 4932 (class 0 OID 0)
-- Dependencies: 219
-- Name: usuarios_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.usuarios_id_seq', 1, false);


--
-- TOC entry 4767 (class 2606 OID 16445)
-- Name: usuarios uq_usuarios_cnpj; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT uq_usuarios_cnpj UNIQUE (cnpj);


--
-- TOC entry 4769 (class 2606 OID 16443)
-- Name: usuarios uq_usuarios_email; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT uq_usuarios_email UNIQUE (email);


--
-- TOC entry 4771 (class 2606 OID 16441)
-- Name: usuarios usuarios_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.usuarios
    ADD CONSTRAINT usuarios_pkey PRIMARY KEY (id);


--
-- TOC entry 4764 (class 1259 OID 16446)
-- Name: idx_usuarios_email; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_usuarios_email ON public.usuarios USING btree (email);


--
-- TOC entry 4765 (class 1259 OID 16447)
-- Name: idx_usuarios_tipo_usuario; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_usuarios_tipo_usuario ON public.usuarios USING btree (tipo_usuario);


-- Completed on 2026-08-10 21:52:44

--
-- PostgreSQL database dump complete
--

\unrestrict gTs7EEP18cXwwL9PSgC0MfR4l3ihMKArsxsLxqhsUb6PIFg25Zh25QFBwQim4fq

