# API de Autenticação — Sistema de Adoção de Animais

API REST em Spring Boot 3 / Java 17 para cadastro, login e autenticação (tradicional + OAuth2 Google) de dois perfis de usuário: **ONG** e **Usuário Comum**.

## Stack

Java 17 · Spring Boot 3.3 · Spring Security · Spring Security OAuth2 Client · Spring Data JPA · PostgreSQL · Lombok · JJWT (tokens JWT)

## Estrutura do projeto

```
src/main/java/com/adocao/api/
├── AdocaoApiApplication.java
├── config/SecurityConfig.java              # Regras do Spring Security, CSRF off, OAuth2 login
├── controller/AuthController.java          # Endpoints REST
├── dto/                                    # LoginDTO, CadastroOngDTO, CadastroUserDTO, respostas
├── entity/                                 # Usuario, MembroEquipe, HorarioVisita, TipoUsuario, AuthProvider
├── exception/                              # Exceções de negócio + @RestControllerAdvice
├── repository/UsuarioRepository.java
├── security/
│   ├── JwtService.java                     # Geração/validação de token JWT
│   ├── JwtAuthenticationFilter.java        # Filtro que autentica requisições via Bearer token
│   ├── CustomUserDetailsService.java
│   ├── CustomOAuth2UserService.java        # Cria o usuário automaticamente no 1º login Google
│   └── OAuth2AuthenticationSuccessHandler.java  # Devolve JWT em JSON após login Google
├── service/AuthService.java                # Regra de negócio, hashing BCrypt, validação de CNPJ
└── util/CnpjValidator.java                 # Validação de formato + dígitos verificadores do CNPJ
sql/schema.sql                              # Script de criação do banco aucolher_db + tabelas
```

## Como rodar

### 1. Banco de dados

```bash
psql -U postgres -h localhost -f sql/schema.sql
```

O script cria o banco `aucolher_db` (se ainda não existir) e as tabelas `usuarios`, `ong_equipe` e `ong_horarios_visita`. É uma cópia de `docs/script_banco_aucolher.sql` do frontend — mantenha os dois iguais. O Hibernate (`ddl-auto=update`) não cria o banco, só as tabelas, então rode o script antes de subir a API.

### 2. Variáveis de ambiente

| Variável              | Descrição                                   | Padrão (dev)          |
|-----------------------|----------------------------------------------|------------------------|
| `DB_USERNAME`         | Usuário do PostgreSQL                        | `postgres`             |
| `DB_PASSWORD`         | Senha do PostgreSQL                          | `postgres`              |
| `GOOGLE_CLIENT_ID`    | Client ID do Google OAuth2                   | —                       |
| `GOOGLE_CLIENT_SECRET`| Client Secret do Google OAuth2               | —                       |
| `JWT_SECRET`          | Chave usada para assinar os tokens JWT       | valor de exemplo no properties |
| `JWT_EXPIRATION_MS`   | Validade do token em milissegundos           | `86400000` (24h)        |

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
  "facebook": "https://facebook.com/amigosdosanimais"
}
```

Endereço é obrigatório; `fotoUrl`, `bio`, `emailInstitucional` e redes sociais são opcionais. CNPJ e CEP podem vir com ou sem máscara (são gravados só com dígitos) e Instagram/X com ou sem `@`.

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
    "fotoUrl": null,
    "bio": null,
    "cnpj": null,
    "emailInstitucional": null,
    "isVerificado": false,
    "instagram": null,
    "twitter": null,
    "facebook": null,
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

### Login via Google (OAuth2)
```
GET /oauth2/authorization/google
```
Redirecione o usuário para essa URL no navegador. Após o consentimento, o Spring Security completa o fluxo OAuth2; no primeiro acesso, `CustomOAuth2UserService` cria automaticamente o registro em `usuarios` com `tipoUsuario = USUARIO_COMUM` e `provider = GOOGLE`. Em seguida, `OAuth2AuthenticationSuccessHandler` devolve o mesmo formato de JSON (token + usuário) dos logins tradicionais.

## Decisões de projeto

- **JWT stateless**: como o requisito pede desativação de CSRF e uma API REST pura, optei por sessão stateless com JWT em vez de sessão HTTP tradicional — é o padrão de mercado para esse tipo de API e evita problemas de CORS/cookies com clientes SPA/mobile.
- **Senha nula para contas OAuth2**: usuários criados via Google não têm senha própria; o login tradicional (`/login`) rejeita essas contas com uma mensagem explicativa.
- **CNPJ validado com dígito verificador real** (não apenas regex de formato), em `CnpjValidator`.
- **Constraints de banco** (`CHECK`) reforçam no schema.sql as mesmas regras já validadas na aplicação (ONG exige CNPJ; contas LOCAL exigem senha) como camada extra de integridade.
