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
├── entity/                                 # Usuario, TipoUsuario, AuthProvider
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
sql/schema.sql                              # Script de criação do banco + tabela
```

## Como rodar

### 1. Banco de dados

```bash
psql -U postgres -f sql/schema.sql
```

Ou deixe o Hibernate criar a tabela automaticamente (já configurado `ddl-auto=update`) — nesse caso o `sql/schema.sql` serve como documentação/referência do schema.

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
  "email": "contato@amigosdosanimais.org",
  "senha": "senhaSegura123",
  "cnpj": "12.345.678/0001-95"
}
```

### Cadastro de Usuário Comum
```
POST /api/auth/register/user
Content-Type: application/json

{
  "nome": "Maria Silva",
  "email": "maria@email.com",
  "senha": "senhaSegura123"
}
```

### Login de ONG
```
POST /api/auth/login/ong
Content-Type: application/json

{ "email": "contato@amigosdosanimais.org", "senha": "senhaSegura123" }
```

### Login de Usuário Comum
```
POST /api/auth/login/user
Content-Type: application/json

{ "email": "maria@email.com", "senha": "senhaSegura123" }
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
    "cnpj": null,
    "tipoUsuario": "USUARIO_COMUM",
    "provider": "LOCAL"
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
- **Senha nula para contas OAuth2**: usuários criados via Google não têm senha própria; o login tradicional (`/login/user`, `/login/ong`) rejeita essas contas com uma mensagem explicativa.
- **CNPJ validado com dígito verificador real** (não apenas regex de formato), em `CnpjValidator`.
- **Constraints de banco** (`CHECK`) reforçam no schema.sql as mesmas regras já validadas na aplicação (ONG exige CNPJ; contas LOCAL exigem senha) como camada extra de integridade.
