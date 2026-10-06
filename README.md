# AUcolher API — Plataforma de Adoção de Animais

API REST em Spring Boot 3 / Java 17 do AUcolher: cadastro, login e autenticação (tradicional + OAuth2 Google) de dois perfis de usuário, **ONG** e **Usuário Comum**, edição do perfil ("Minha conta"), anúncios de animais para adoção e favoritos.

## Stack

Java 17 · Spring Boot 3.3 · Spring Security · Spring Security OAuth2 Client · Spring Data JPA · PostgreSQL · Lombok · JJWT (tokens JWT)

## Estrutura do projeto

O código é organizado **por funcionalidade**: cada parte do sistema tem seu pacote com controller, service, repository, entidades e DTOs juntos. Uma funcionalidade nova (ex: `animal/`, `adocao/`) ganha o seu pacote no mesmo molde; o que é usado por várias delas fica em `shared/`.

```
src/main/java/com/aucolher/api/
├── AucolherApiApplication.java
├── auth/                                   # Cadastro e login
│   ├── AuthController.java
│   ├── AuthService.java                    # Regra de negócio e hashing BCrypt
│   └── dto/                                # LoginDTO, CadastroOngDTO, CadastroUserDTO, AuthResponseDTO
├── usuario/                                # Conta do usuário e perfil ("Minha conta")
│   ├── UsuarioController.java
│   ├── UsuarioService.java
│   ├── UsuarioRepository.java
│   ├── entity/                             # Usuario, MembroEquipe, HorarioVisita, TipoUsuario, AuthProvider
│   └── dto/                                # AtualizacaoPerfilDTO, UsuarioResponseDTO, MembroEquipeDTO, HorarioVisitaDTO
├── animal/                                 # Anúncios de adoção
│   ├── AnimalController.java
│   ├── AnimalService.java                  # Regras: só o dono altera, ADOTADO é definitivo, inativo só o dono vê
│   ├── AnimalRepository.java
│   ├── AnimalSpecifications.java           # Monta o WHERE da listagem a partir dos filtros recebidos
│   ├── entity/                             # Animal + enums (Especie, Porte, Sexo, Nivel, Temperamento, StatusAnimal...)
│   └── dto/                                # AnimalRequestDTO, AnimalDetalheDTO, AnimalResumoDTO (card), AnimalFiltroDTO...
├── favorito/                               # Animais favoritados por cada usuário
│   ├── FavoritoController.java
│   ├── FavoritoService.java
│   ├── FavoritoRepository.java
│   └── entity/Favorito.java
├── shared/                                 # Comum a todas as funcionalidades
│   ├── dto/PaginaDTO.java                  # Formato padrão das respostas paginadas
│   ├── exception/                          # Exceções (400, 401, 403, 404), @RestControllerAdvice e ErroResponseDTO
│   └── validation/                         # Sanitizador (normalização nos records), FotoUrl, @AnoFundacao
├── config/SecurityConfig.java              # Regras do Spring Security, CSRF off, OAuth2 login
└── security/
    ├── JwtService.java                     # Geração/validação de token JWT
    ├── JwtAuthenticationFilter.java        # Filtro que autentica requisições via Bearer token
    ├── CustomUserDetailsService.java
    ├── CustomOAuth2UserService.java        # Cria o usuário automaticamente no 1º login Google
    └── OAuth2AuthenticationSuccessHandler.java  # Devolve JWT em JSON após login Google
sql/schema.sql                              # Script de criação do banco aucolher_db + tabelas
src/test/java/...                           # Testes (JUnit 5 + Mockito + MockMvc)
```

## Como rodar

### 1. Banco de dados

```bash
psql -U postgres -h localhost -f sql/schema.sql
```

O script cria o banco `aucolher_db` (se ainda não existir) e as tabelas `usuarios`, `ong_equipe`, `ong_horarios_visita`, `animais`, `animal_fotos` e `favoritos`. Todas usam `IF NOT EXISTS`, então rodar o script de novo num banco existente só acrescenta o que falta. Banco criado por uma versão anterior do script? Rode-o de novo: os `ALTER TABLE ... IF NOT EXISTS` logo depois do `CREATE TABLE usuarios` acrescentam as colunas novas (ex: `ano_fundacao`) — sem elas a API não sobe, por causa do `ddl-auto=validate`. É uma cópia de `docs/script_banco_aucolher.sql` do frontend — mantenha os dois iguais. Rode o script antes de subir a API: o Hibernate (`ddl-auto=validate`) não cria o banco nem as tabelas, só confere se elas batem com as entidades.

### 2. Variáveis de ambiente

| Variável              | Descrição                                   | Padrão (dev)          |
|-----------------------|----------------------------------------------|------------------------|
| `DB_USERNAME`         | Usuário do PostgreSQL                        | `postgres`             |
| `DB_PASSWORD`         | Senha do PostgreSQL                          | `1234`                  |
| `GOOGLE_CLIENT_ID`    | Client ID do Google OAuth2                   | —                       |
| `GOOGLE_CLIENT_SECRET`| Client Secret do Google OAuth2               | —                       |
| `JWT_SECRET`          | Chave HMAC dos tokens JWT (mínimo 32 caracteres) | — (sem padrão versionado) |
| `JWT_EXPIRATION_MS`   | Validade do token em milissegundos           | `86400000` (24h)        |
| `CORS_ALLOWED_ORIGINS`| Origens liberadas no CORS (separadas por vírgula) | `http://localhost:5173` |

Sem `JWT_SECRET`, a API sobe gerando uma chave aleatória por execução (registra um WARN no log): dá para desenvolver, mas todo restart invalida os tokens já emitidos. Defina a variável em qualquer ambiente que não seja a sua máquina.

Para o login Google funcionar, crie as credenciais em [Google Cloud Console](https://console.cloud.google.com/apis/credentials) com **Authorized redirect URI**: `http://localhost:8080/login/oauth2/code/google`.

### 3. Executar

```bash
mvn spring-boot:run
```

A API sobe em `http://localhost:8080`.

### 4. Testes

```bash
mvn test
```

Os testes não precisam de banco: as regras de negócio são testadas com o repositório simulado (Mockito) e as rotas com MockMvc.

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

### Animais

Qualquer conta (ONG ou usuário comum) pode anunciar. A cidade/UF do animal são as do perfil de quem anuncia — preencha-as em `PUT /api/usuarios/me` antes, senão o cadastro responde 400.

| Método e rota | Login | O que faz |
|---|---|---|
| `GET /api/animais` | não | Vitrine: só os `DISPONIVEL`, do mais recente para o mais antigo, com filtros e paginação |
| `GET /api/animais/{id}` | não | Detalhes completos. Anúncio `INATIVO` só aparece para o dono (para os outros, 404) |
| `POST /api/animais` | sim | Cadastra — responde 201 com o animal completo |
| `PUT /api/animais/{id}` | dono | Edição completa (mesmo corpo do cadastro). Animal adotado não pode ser editado |
| `PATCH /api/animais/{id}/status` | dono | `{ "status": "ADOTADO" }`, `"INATIVO"` ou `"DISPONIVEL"`. `ADOTADO` é definitivo |
| `DELETE /api/animais/{id}` | dono | Tira do ar (exclusão lógica: vira `INATIVO`) — responde 204 |
| `GET /api/animais/meus` | sim | Todos os anúncios da conta logada, em qualquer status |
| `GET /api/usuarios/{id}/animais` | não | Os disponíveis de um perfil público |

Quem não é dono recebe 403 nas rotas de alteração.

**Cadastro / edição** (`POST` e `PUT`):
```json
{
  "nome": "Thor",
  "especie": "CACHORRO",
  "raca": "Vira-lata",
  "sexo": "MACHO",
  "idadeValor": 3,
  "idadeUnidade": "ANOS",
  "porte": "GRANDE",
  "vacinado": true,
  "castrado": true,
  "vermifugado": true,
  "necessidadesEspeciais": false,
  "nivelEnergia": "ALTO",
  "temperamento": "PROTETOR",
  "nivelIndependencia": "MODERADO",
  "nivelVocalizacao": "MODERADO",
  "bomComCriancas": true,
  "bomComCaes": true,
  "bomComGatos": false,
  "adaptadoApartamento": false,
  "resumo": "Protetor e brincalhão, pronto para uma nova aventura!",
  "historia": "Thor foi resgatado ainda filhote...",
  "fotos": ["data:image/jpeg;base64,...", "https://..."]
}
```

Valores aceitos:

| Campo | Valores |
|---|---|
| `especie` | `CACHORRO`, `GATO`, `OUTRO` |
| `sexo` | `MACHO`, `FEMEA` |
| `porte` | `PEQUENO`, `MEDIO`, `GRANDE` |
| `idadeUnidade` | `MESES` (0 a 11) ou `ANOS` (1 a 30) |
| `nivelEnergia`, `nivelIndependencia`, `nivelVocalizacao` | `BAIXO`, `MODERADO`, `ALTO` |
| `temperamento` | `CALMO`, `BRINCALHAO`, `AFETUOSO`, `PROTETOR`, `INDEPENDENTE` |
| `fotos` | de 1 a 4, URL http(s) ou data URL de imagem (mesmas regras da foto de perfil); a primeira é a capa |

Os itens de saúde (`vacinado`, `castrado`, `vermifugado`, `necessidadesEspeciais`) são opcionais e valem `false` quando não vêm. `faixaEtaria` (`FILHOTE`, `ADULTO`, `IDOSO`) não é enviada: a API calcula pela idade (menos de 1 ano é filhote; a partir de 8 anos, idoso).

A resposta do cadastro, da edição e de `GET /api/animais/{id}` traz esses campos mais `id`, `faixaEtaria`, `status`, `cidade`, `estado`, `dono` (`id`, `nome`, `fotoUrl`, `tipoUsuario`, `isVerificado`), `dataCriacao` e `dataAtualizacao`.

**Listagem** — filtros opcionais na query string; os de múltipla escolha podem se repetir:
```
GET /api/animais?especie=CACHORRO&especie=GATO&porte=PEQUENO&cidade=Araranguá&page=0&size=12
```

Filtros: `busca` (nome, raça ou cidade), `especie`, `porte`, `sexo`, `faixaEtaria`, `nivelEnergia`, `temperamento`, `necessidadesEspeciais=true`, `cidade`, `estado`. Paginação: `page` começa em 0; `size` padrão 12, máximo 50.

```json
{
  "conteudo": [ { "id": 1, "nome": "Thor", "fotoCapa": "data:image/jpeg;base64,...", "cidade": "Araranguá", "...": "..." } ],
  "pagina": 0,
  "tamanho": 12,
  "totalElementos": 1,
  "totalPaginas": 1
}
```

Os cards (listagem, meus animais, perfil e favoritos) trazem só a `fotoCapa` e não trazem a `historia` — a página de detalhes busca o animal completo.

### Favoritos

Todas exigem login.

| Método e rota | O que faz |
|---|---|
| `GET /api/favoritos` | Cards dos animais favoritados, do mais recente para o mais antigo. Inativos somem; adotados continuam, com `status: ADOTADO` |
| `GET /api/favoritos/ids` | Só os ids — para marcar os corações nos cards |
| `PUT /api/favoritos/{animalId}` | Favorita. Repetir não duplica — responde 204 |
| `DELETE /api/favoritos/{animalId}` | Desfavorita — responde 204 |

## Decisões de projeto

- **JWT stateless**: como o requisito pede desativação de CSRF e uma API REST pura, optei por sessão stateless com JWT em vez de sessão HTTP tradicional — é o padrão de mercado para esse tipo de API e evita problemas de CORS/cookies com clientes SPA/mobile.
- **Senha nula para contas OAuth2**: usuários criados via Google não têm senha própria; o login tradicional (`/login`) rejeita essas contas com uma mensagem explicativa.
- **Normalização nos DTOs**: os construtores compactos dos records limpam os campos (trim, vazio vira null, CNPJ/CEP só com dígitos, rede social sem `@`, link com protocolo) antes da validação. A Service recebe dados já canônicos e cuida só da regra de negócio.
- **CNPJ pelo Bean Validation**: `@CNPJ` do Hibernate Validator confere os dígitos verificadores; um `@Pattern` extra barra sequências repetidas (`00.000.000/0000-00`), que o `@CNPJ` sozinho aceita.
- **JWT mínimo**: o token carrega apenas o e-mail (subject) e a validade. Papel e id não viram claim para não circular desatualizados — quem responde por eles é o banco, relido a cada requisição autenticada.
- **`ddl-auto=validate`**: o schema vem do `sql/schema.sql`; o Hibernate só confere se o banco bate com as entidades, em vez de alterar tabelas sozinho.
- **Constraints de banco** (`CHECK`) reforçam no schema.sql as mesmas regras já validadas na aplicação (ONG exige CNPJ; contas LOCAL exigem senha; valores dos enums dos animais) como camada extra de integridade.
- **Localização do animal = a do dono**: `animais` não tem cidade/UF; a listagem filtra pelo endereço do perfil de quem anunciou. Filtro por distância ("perto de mim") fica para quando `usuarios` tiver latitude/longitude.
- **Exclusão lógica de animais**: `DELETE` só muda o status para `INATIVO`, preservando o histórico (favoritos e, no futuro, pedidos de adoção).
- **Cards leves**: a listagem busca só a foto de capa (numa consulta única) e carrega o dono junto, então uma página custa duas consultas SQL, com payload pequeno mesmo com fotos em data URL.
- **Favoritar é idempotente no banco**: `INSERT ... ON CONFLICT DO NOTHING` garante um favorito por par usuário/animal mesmo com cliques simultâneos.
