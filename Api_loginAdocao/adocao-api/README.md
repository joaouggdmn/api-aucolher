# API de Autenticação — Sistema de Adoção de Animais

API REST em Spring Boot 3 / Java 17 para cadastro, login e autenticação (tradicional + OAuth2 Google) de dois perfis de usuário: **ONG** e **Usuário Comum**.

## Stack

Java 17 · Spring Boot 3.3 · Spring Security · Spring Security OAuth2 Client · Spring Data JPA · PostgreSQL · Lombok · JJWT (tokens JWT)

## Estrutura do projeto

```
src/main/java/com/adocao/api/
├── AdocaoApiApplication.java
├── config/SecurityConfig.java              # Regras do Spring Security, CSRF off, OAuth2 login
├── controller/AuthController.java          # Cadastro e login
├── controller/UsuarioController.java       # Edição do perfil da própria conta
├── dto/                                    # LoginDTO, CadastroOngDTO, CadastroUserDTO, AtualizacaoPerfilDTO, respostas
│                                           # (os records normalizam os campos no construtor compacto)
├── entity/                                 # Usuario, MembroEquipe, HorarioVisita, TipoUsuario, AuthProvider
├── exception/                              # Exceções de negócio + @RestControllerAdvice
├── repository/UsuarioRepository.java
├── security/
│   ├── JwtService.java                     # Geração/validação de token JWT
│   ├── JwtAuthenticationFilter.java        # Filtro que autentica requisições via Bearer token
│   ├── CustomUserDetailsService.java
│   ├── CustomOAuth2UserService.java        # Cria o usuário automaticamente no 1º login Google
│   └── OAuth2AuthenticationSuccessHandler.java  # Devolve JWT em JSON após login Google
├── service/AuthService.java                # Regra de negócio e hashing BCrypt
└── service/UsuarioService.java             # Edição do perfil ("Minha conta")
sql/schema.sql                              # Script de criação do banco aucolher_db + tabelas
```

## Como rodar

### 1. Banco de dados

```bash
psql -U postgres -h localhost -f sql/schema.sql
```

O script cria o banco `aucolher_db` (se ainda não existir) e as tabelas `usuarios`, `ong_equipe` e `ong_horarios_visita`. Banco criado por uma versão anterior do script? Rode-o de novo: os `ALTER TABLE ... IF NOT EXISTS` logo depois do `CREATE TABLE usuarios` acrescentam as colunas novas (ex: `ano_fundacao`) — sem elas a API não sobe, por causa do `ddl-auto=validate`. É uma cópia de `docs/script_banco_aucolher.sql` do frontend — mantenha os dois iguais. O Hibernate (`ddl-auto=update`) não cria o banco, só as tabelas, então rode o script antes de subir a API.

### 2. Variáveis de ambiente

| Variável              | Descrição                                   | Padrão (dev)          |
|-----------------------|----------------------------------------------|------------------------|
| `DB_USERNAME`         | Usuário do PostgreSQL                        | `postgres`             |
| `DB_PASSWORD`         | Senha do PostgreSQL                          | `postgres`              |
| `GOOGLE_CLIENT_ID`    | Client ID do Google OAuth2                   | —                       |
| `GOOGLE_CLIENT_SECRET`| Client Secret do Google OAuth2               | —                       |
| `JWT_SECRET`          | Chave HMAC dos tokens JWT (mínimo 32 caracteres) | — (sem padrão versionado) |
| `JWT_EXPIRATION_MS`   | Validade do token em milissegundos           | `86400000` (24h)        |
| `CORS_ALLOWED_ORIGINS`| Origens liberadas no CORS (separadas por vírgula) | `http://localhost:5173` |

Sem `JWT_SECRET`, a API sobe gerando uma chave aleatória por execução (registra um WARN no log): dá para desenvolver, mas todo restart invalida os tokens já emitidos. Defina a variável em qualquer ambiente que não seja a sua máquina.

Para o login Google funcionar, crie as credenciais em [Google Cloud Console](https://console.cloud.google.com/apis/credentials) com **Authorized redirect URI**: `http://localhost:8080/login/oauth2/code/google`.

### 3. Executar

```bash
./mvnw spring-boot:run
```

A API sobe em `http://localhost:8080`.

## Endpoints

### Cadastro de ONG
```
POST /api/auth/register/ong
Content-Type: application/json

{
  "nome": "ONG Amigos dos Animais",
  "email": "amigosdosanimais@gmail.com",
  "senha": "senhaSegura123",
  "cnpj": "12.345.678/0001-95",
  "cep": "88900-000",
  "logradouro": "Rua Caetano Lummertz",
  "numero": "1250",
  "complemento": null,
  "bairro": "Centro",
  "cidade": "Araranguá",
  "estado": "SC",
  "fotoUrl": null,
  "bio": "Resgatamos e encontramos lares para cães e gatos.",
  "emailInstitucional": "contato@amigosdosanimais.org",
  "instagram": "amigosdosanimais",
  "twitter": "amigosanimais",
  "facebook": "https://facebook.com/amigosdosanimais",
  "anoFundacao": 2016
}
```

Endereço é obrigatório; `fotoUrl`, `bio`, `emailInstitucional`, redes sociais e `anoFundacao` são opcionais. `anoFundacao` precisa estar entre 1800 e o ano atual. CNPJ e CEP podem vir com ou sem máscara (são gravados só com dígitos) e Instagram/X com ou sem `@`.

### Cadastro de Usuário Comum
```
POST /api/auth/register/user
Content-Type: application/json

{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "senha": "senhaSegura123",
  "fotoUrl": null
}
```

### Login (ONG e Usuário Comum)
```
POST /api/auth/login
Content-Type: application/json

{ "email": "maria@email.com", "senha": "senhaSegura123" }
```

Rota única para qualquer tipo de conta: o tipo vem em `usuario.tipoUsuario` na resposta.

### Formato dos erros

Todos os erros seguem o mesmo formato. `mensagem` já vem pronta para exibir; `erros` só aparece em falhas de validação, com o motivo de cada campo:

```json
{
  "timestamp": "2026-09-20T18:14:01.173",
  "status": 400,
  "erro": "Dados inválidos",
  "mensagem": "CNPJ inválido",
  "erros": { "cnpj": "CNPJ inválido", "cep": "CEP em formato inválido" }
}
```

Todas as respostas de autenticação seguem o formato:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tipo": "Bearer",
  "usuario": {
    "id": 1,
    "nome": "Maria Silva",
    "email": "maria@email.com",
    "tipoUsuario": "USUARIO_COMUM",
    "provider": "LOCAL",
    "dataCriacao": "2026-09-20T18:14:01.173",
    "fotoUrl": null,
    "bio": null,
    "cnpj": null,
    "emailInstitucional": null,
    "isVerificado": false,
    "instagram": null,
    "twitter": null,
    "facebook": null,
    "anoFundacao": null,
    "equipe": [],
    "horariosVisita": [],
    "cep": null,
    "logradouro": null,
    "numero": null,
    "complemento": null,
    "bairro": null,
    "cidade": null,
    "estado": null
  }
}
```

Use o token nas requisições autenticadas: `Authorization: Bearer {token}`.

### Edição do perfil ("Minha conta")
```
PUT /api/usuarios/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "nome": "ONG Amigos dos Animais",
  "fotoUrl": null,
  "bio": "Resgatamos e encontramos lares para cães e gatos.",
  "emailInstitucional": "contato@amigosdosanimais.org",
  "instagram": "amigosdosanimais",
  "twitter": null,
  "facebook": "https://facebook.com/amigosdosanimais",
  "anoFundacao": 2016,
  "equipe": [{ "nome": "Marina Costa", "funcao": "Presidente" }],
  "horariosVisita": [{ "dias": "Terça a sexta", "horario": "14h às 18h" }],
  "cep": "88900-000",
  "logradouro": "Rua Caetano Lummertz",
  "numero": "1250",
  "complemento": "Galpão B",
  "bairro": "Centro",
  "cidade": "Araranguá",
  "estado": "SC"
}
```

A conta editada é sempre a do token — não há id na rota. É uma substituição completa: campo opcional que não vier é apagado, e `equipe`/`horariosVisita` substituem as listas inteiras (na ordem enviada). Usuário comum grava só `nome`, `fotoUrl`, `bio`, `cep`, `cidade` e `estado`; o resto é ignorado. Para ONG o endereço continua obrigatório. E-mail, senha e CNPJ não mudam por aqui. Responde com o mesmo objeto `usuario` das rotas de autenticação — que também traz `equipe` e `horariosVisita`, para o frontend não depender de nada guardado no navegador.

### Login via Google (OAuth2)
```
GET /oauth2/authorization/google
```
Redirecione o usuário para essa URL no navegador. Após o consentimento, o Spring Security completa o fluxo OAuth2; no primeiro acesso, `CustomOAuth2UserService` cria automaticamente o registro em `usuarios` com `tipoUsuario = USUARIO_COMUM` e `provider = GOOGLE`. Em seguida, `OAuth2AuthenticationSuccessHandler` devolve o mesmo formato de JSON (token + usuário) dos logins tradicionais.

## Decisões de projeto

- **JWT stateless**: como o requisito pede desativação de CSRF e uma API REST pura, optei por sessão stateless com JWT em vez de sessão HTTP tradicional — é o padrão de mercado para esse tipo de API e evita problemas de CORS/cookies com clientes SPA/mobile.
- **Senha nula para contas OAuth2**: usuários criados via Google não têm senha própria; o login tradicional (`/login`) rejeita essas contas com uma mensagem explicativa.
- **Normalização nos DTOs**: os construtores compactos dos records limpam os campos (trim, vazio vira null, CNPJ/CEP só com dígitos, rede social sem `@`, link com protocolo) antes da validação. A Service recebe dados já canônicos e cuida só da regra de negócio.
- **CNPJ pelo Bean Validation**: `@CNPJ` do Hibernate Validator confere os dígitos verificadores; um `@Pattern` extra barra sequências repetidas (`00.000.000/0000-00`), que o `@CNPJ` sozinho aceita.
- **JWT mínimo**: o token carrega apenas o e-mail (subject) e a validade. Papel e id não viram claim para não circular desatualizados — quem responde por eles é o banco, relido a cada requisição autenticada.
- **`ddl-auto=validate`**: o schema vem do `sql/schema.sql`; o Hibernate só confere se o banco bate com as entidades, em vez de alterar tabelas sozinho.
- **Constraints de banco** (`CHECK`) reforçam no schema.sql as mesmas regras já validadas na aplicação (ONG exige CNPJ; contas LOCAL exigem senha) como camada extra de integridade.
