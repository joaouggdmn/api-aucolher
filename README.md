# AUcolher API — Plataforma de Adoção de Animais

API REST em Spring Boot 3 / Java 17 do AUcolher: cadastro, login e autenticação (tradicional + OAuth2 Google) de dois perfis de usuário, **ONG** e **Usuário Comum**, edição do perfil ("Minha conta"), anúncios de animais para adoção, favoritos, **eventos** das ONGs (com confirmação de presença), o **AUmatch** (quiz de estilo de vida e deck ordenado por compatibilidade) os **pedidos de adoção**, com avaliações depois da adoção concluída, o **retorno e o repasse** de uma adoção (a taxa de reabandono), as **denúncias** e o **painel do admin**, com estatísticas e moderação.

## Stack

Java 17 · Spring Boot 3.3 · Spring Security · Spring Security OAuth2 Client · Spring Data JPA · PostgreSQL · Lombok · JJWT (tokens JWT)

## Estrutura do projeto

O código é organizado **por funcionalidade**: cada parte do sistema tem seu pacote com controller, service, repository, entidades e DTOs juntos. Uma funcionalidade nova (ex: `adoption/`) ganha o seu pacote no mesmo molde; o que é usado por várias delas fica em `shared/`.

```
src/main/java/com/aucolher/api/
├── AucolherApiApplication.java
├── auth/                                   # Cadastro e login
│   ├── AuthController.java
│   ├── AuthService.java                    # Regra de negócio e hashing BCrypt
│   └── dto/                                # LoginDTO, NgoRegistrationDTO, PersonRegistrationDTO, AuthResponseDTO
├── user/                                   # Conta do usuário e perfil ("Minha conta")
│   ├── UserController.java
│   ├── UserService.java
│   ├── AccountDeletionService.java         # Excluir a própria conta: cancela, tira do ar e anonimiza
│   ├── UserRepository.java
│   ├── entity/                             # User, TeamMember, VisitingHour, UserType, AuthProvider
│   └── dto/                                # ProfileUpdateDTO, UserResponseDTO, PublicProfileDTO, TeamMemberDTO, VisitingHourDTO, AccountDeletionDTO
├── animal/                                 # Anúncios de adoção
│   ├── AnimalController.java
│   ├── AnimalService.java                  # Regras: só o dono altera, ADOPTED é definitivo, inativo só o dono vê
│   ├── AnimalRepository.java
│   ├── AnimalSpecifications.java           # Monta o WHERE da listagem a partir dos filtros recebidos
│   ├── entity/                             # Animal + enums (Species, AnimalSize, Sex, Level, Temperament, AnimalStatus...)
│   └── dto/                                # AnimalRequestDTO, AnimalDetailDTO, AnimalSummaryDTO (card), AnimalFilterDTO...
├── favorite/                               # Animais favoritados por cada usuário
│   ├── FavoriteController.java
│   ├── FavoriteService.java
│   ├── FavoriteRepository.java
│   └── entity/Favorite.java
├── event/                                  # Eventos das ONGs e confirmação de presença
│   ├── EventController.java
│   ├── EventService.java                   # Regras: só a ONG dona altera, capacidade, evento passado não muda
│   ├── EventRepository.java / EventAttendanceRepository.java
│   ├── entity/                             # Event, EventAttendance + EventCategory, EventStatus
│   └── dto/                                # EventRequestDTO, EventResponseDTO, AttendeeDTO, NgoSummaryDTO...
├── campaign/                               # Campanhas de arrecadação das ONGs
│   ├── CampaignController.java
│   ├── CampaignService.java                # Regras: uma urgente ativa por ONG, prazo vencido = encerrada, exclusão real ou lógica
│   ├── CampaignRepository.java
│   ├── MercadoPagoAccountGateway.java      # A conta Mercado Pago da ONG (por ora, o Placeholder com um token de teste)
│   ├── entity/                             # Campaign + CampaignCategory, CampaignStatus
│   └── dto/                                # CampaignRequestDTO, CampaignResponseDTO, CampaignNgoDTO
├── donation/                               # Doações por PIX (Mercado Pago, Orders API)
│   ├── DonationController.java
│   ├── DonationService.java                # Cria o PIX, polling da order, aprovação atômica
│   ├── MercadoPagoPixClient.java           # Chamadas à Orders API (criar e consultar a order)
│   ├── MercadoPagoWebhookController.java / MercadoPagoWebhookValidator.java  # Webhook com a assinatura x-signature
│   ├── entity/                             # Donation + DonationStatus
│   └── dto/                                # DonationResponseDTO, DonationPixDTO, DonationCampaignDTO...
├── match/                                  # AUmatch: quiz da pessoa e deck por compatibilidade
│   ├── MatchController.java
│   ├── MatchService.java                   # Perfil do quiz e deck (filtra, pontua e ordena)
│   ├── MatchScoreCalculator.java           # O algoritmo: 6 critérios que somam 100
│   ├── MatchProfileRepository.java
│   ├── entity/                             # MatchProfile + enums das respostas (Housing, TimeAway...)
│   └── dto/                                # MatchProfileRequestDTO, MatchProfileDTO, MatchCardDTO, MatchCriterionDTO
├── adoption/                               # Pedidos de adoção e avaliações
│   ├── AdoptionRequestController.java
│   ├── AdoptionRequestService.java         # Cria, lista e muda o status (quem pode cada transição)
│   ├── AdoptionReviewController.java       # Avaliações e o resumo público de um perfil
│   ├── AdoptionReviewService.java
│   ├── AdminAdoptionRequestController.java # Todos os pedidos, para o painel do admin
│   ├── AdoptionRequestRepository.java
│   ├── AdoptionReviewRepository.java
│   ├── entity/                             # AdoptionRequest, AdoptionReview + enums (AdoptionStatus, CancelReason, ReviewRole)
│   └── dto/                                # AdoptionRequestDTO, AdopterDTO, ReviewDTO, UserReviewsDTO...
├── report/                                 # Denúncias de anúncios e perfis
│   ├── ReportController.java               # POST /api/reports e /api/reports/mine
│   ├── ReportService.java                  # Regras do envio, cópia do alvo e resolução dos casos
│   ├── ReportRepository.java
│   ├── entity/                             # Report + enums (ReportTargetType, ReportReason, ReportStatus, ReportResolution)
│   └── dto/                                # NewReportDTO, ReportDTO (com reporter só para o admin), ReportTargetDTO
├── admin/                                  # Painel do admin
│   ├── AdminController.java                # /api/admin: estatísticas, listas e moderação
│   ├── AdminService.java                   # Listas de contas e anúncios
│   ├── AdminStatsService.java              # Visão geral, com a taxa de reabandono
│   ├── ModerationService.java              # Banir, reativar, remover anúncio/evento, arquivar denúncia
│   └── dto/                                # AdminStatsDTO, AdminUserDTO, AdminAnimalDTO, ListingStatus...
├── shared/                                 # Comum a todas as funcionalidades
│   ├── dto/PageDTO.java                    # Formato padrão das respostas paginadas
│   ├── exception/                          # Exceções (400, 401, 403, 404), @RestControllerAdvice e ErrorResponseDTO
│   └── validation/                         # Sanitizer (normalização nos records), PhotoUrl, @FoundedYear
├── config/SecurityConfig.java              # Regras do Spring Security, CSRF off, OAuth2 login
└── security/
    ├── JwtService.java                     # Geração/validação de token JWT
    ├── JwtAuthenticationFilter.java        # Filtro que autentica requisições via Bearer token
    ├── CustomUserDetailsService.java
    ├── CustomOAuth2UserService.java        # Cria o usuário automaticamente no 1º login Google
    ├── OAuth2AuthenticationSuccessHandler.java  # Volta ao frontend com um código de troca após login Google
    ├── OAuth2AuthenticationFailureHandler.java  # Volta ao frontend com ?error=failed
    └── GoogleLoginCodeStore.java           # Códigos de troca do login Google (uso único, 60 s)
sql/schema.sql                              # Script de criação do banco aucolher_db + tabelas
sql/migrations/                             # Atualizações para bancos criados por versões anteriores do script
src/test/java/...                           # Testes (JUnit 5 + Mockito + MockMvc)
```

## Como rodar

### 1. Banco de dados

```bash
psql -U postgres -h localhost -f sql/schema.sql
```

O script cria o banco `aucolher_db` (se ainda não existir) e as tabelas `users`, `ngo_team`, `ngo_visiting_hours`, `animals`, `animal_photos`, `favorites`, `match_profiles`, `adoption_requests`, `adoption_reviews`, `events`, `event_attendances`, `reports`, `campaigns` e `donations`. Todas usam `IF NOT EXISTS`, então rodar o script de novo não apaga nada. É uma cópia de `docs/script_banco_aucolher.sql` do frontend — mantenha os dois iguais. Rode o script antes de subir a API: o Hibernate (`ddl-auto=validate`) não cria o banco nem as tabelas, só confere se elas batem com as entidades.

#### Banco criado antes da padronização em inglês

Se o seu `aucolher_db` ainda tem as tabelas em português (`usuarios`, `animais`, `favoritos`...), **não** rode o `schema.sql`: aplique as migrações, em ordem, uma vez só. Elas renomeiam tabelas, colunas e constraints e traduzem os valores gravados (`ONG` → `NGO`, `CACHORRO` → `DOG`...) sem apagar nenhum dado — as contas e senhas continuam valendo.

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/001_users_english.sql
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/002_animals_english.sql
```

Cada migração roda numa transação: se algo falhar, o banco fica como estava. Sem elas a API não sobe (erro `Schema-validation: missing table [users]`).

#### Conta ADMIN e ONGs verificadas (migração 003)

Um banco criado antes do tipo `ADMIN` recusa a conta admin (a constraint `chk_users_user_type` só aceita `NGO` e `PERSON`). Aplique a 003 uma vez. Ela também marca como verificadas as ONGs que já existem, porque toda ONG nasce verificada:

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/003_admin_and_verified_ngos.sql
```

Roda numa transação e pode ser repetida sem estragar nada. Banco novo, criado pelo `schema.sql` atual, não precisa dela.

#### Perfil AUmatch (migração 004)

Um banco criado antes do AUmatch no backend não tem a tabela `match_profiles`, e a API não sobe sem ela (`Schema-validation: missing table [match_profiles]`). Aplique a 004 uma vez:

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/004_match_profiles.sql
```

Ela só cria a tabela nova (nenhum dado existente muda) e pode ser repetida. Banco novo, criado pelo `schema.sql` atual, não precisa dela.

#### Pedidos de adoção e avaliações (migração 005)

A 005 cria `adoption_requests` e `adoption_reviews`, que até aqui existiam só no navegador. Sem elas a API não sobe (`Schema-validation: missing table [adoption_requests]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/005_adoption_requests.sql
```

Também só cria tabelas novas e pode ser repetida.

#### Eventos (migração 006)

A 006 cria `events` e `event_attendances`, que até aqui existiam só no navegador (mock do frontend). Sem elas a API não sobe (`Schema-validation: missing table [events]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/006_events.sql
```

Só cria tabelas novas e pode ser repetida.

#### Campanhas e doações (migração 007)

A 007 cria `campaigns` (já com `deleted`, `removed_by_moderation`, `moderated_at` e `moderated_by`) e `donations` (com `campaign_title` e `ngo_id` guardados na doação, para o histórico continuar legível se a campanha for apagada de vez). Sem ela a API não sobe (`Schema-validation: missing table [campaigns]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/007_campaigns.sql
```

Só cria o que falta e reescreve os comentários: pode ser repetida.

#### Retorno, repasse e score congelado (migração 012)

A 012 prepara a **taxa de reabandono**: um pedido `CONCLUDED` pode virar `RETURNED` (o animal voltou para quem doou) ou `REHOMED` (o adotante republicou o animal). Ela acrescenta em `adoption_requests` o score do AUmatch congelado na conclusão (`match_score_at_adoption`) e os dados do retorno (`return_date`, `return_reason`, `return_note`, `return_recorded_at`), e em `animals` o `origin_request_id` do anúncio republicado. Sem ela a API não sobe (`Schema-validation: missing column [match_score_at_adoption]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/012_adoption_returns.sql
```

Nenhum dado existente muda: os pedidos já concluídos ficam com o score nulo (entram na taxa geral, fora das faixas). Pode ser repetida.

#### Denúncias e moderação (migração 014)

A 014 cria a tabela `reports` (denúncias de anúncios e perfis, com a cópia do alvo e a decisão do admin) e prepara a moderação: `animals.status` passa a aceitar `REMOVED`, `adoption_requests.cancel_reason` passa a aceitar `ACCOUNT_BANNED` e `LISTING_REMOVED` (e já `ACCOUNT_DELETED`, usado pela exclusão de conta da 013), e `events` ganha `removed_by_moderation`. O banimento usa a coluna `users.active`, que já existia. Sem ela a API não sobe (`Schema-validation: missing table [reports]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/014_reports_moderation.sql
```

Pode ser repetida. A 014 não mexe em `campaigns`: as colunas de moderação dela já nascem na 007.

#### Banner da ONG (migração 008)

A 008 acrescenta `users.cover_url`, o banner do perfil público. Só ONG pode ter (constraint `chk_users_cover_only_ngo`). Por segurança, ela também apaga a antiga coluna `telefone_whatsapp`, se o banco ainda veio do `adocao_db`: o contato acontece pelo chat do pedido e pelas redes da ONG. Sem ela a API não sobe (`Schema-validation: missing column [cover_url]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/008_ngo_cover.sql
```

Pode ser repetida.

#### Edição pela moderação (migração 016)

A 016 acrescenta `moderated_at` e `moderated_by` em `users`, `animals` e `events`: quando o admin edita a conta, o anúncio ou o evento de outra pessoa, fica registrado quando e quem. Sem ela a API não sobe (`Schema-validation: missing column [moderated_at]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/016_moderation_edits.sql
```

Só acrescenta colunas nulas e pode ser repetida.

#### Sem e-mail institucional (migração 015)

A 015 apaga a coluna `users.institutional_email`: o contato público da ONG passa a ser o próprio e-mail de login. (As migrações 009 a 011 estão reservadas para as próximas sprints.) A API sobe com ou sem ela, porque o `validate` não reclama de coluna sobrando, mas rode para o banco ficar igual ao `schema.sql`:

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/015_drop_institutional_email.sql
```

Apaga os e-mails institucionais já gravados. Pode ser repetida.

#### Exclusão de conta (migração 013)

A 013 acrescenta `users.deleted_at`, preenchida quando a pessoa ou a ONG exclui a própria conta. A linha não é apagada, e sim anonimizada (sem senha, sem CNPJ). Por isso as constraints `chk_users_ngo_has_cnpj` e `chk_users_local_has_password` são recriadas para valer só nas contas não excluídas, e a nova `chk_users_deleted_inactive` garante que conta excluída fica inativa. Sem ela a API não sobe (`Schema-validation: missing column [deleted_at]`):

```bash
psql -U postgres -h localhost -d aucolher_db -f sql/migrations/013_account_deletion.sql
```

Nenhum dado muda. Pode ser repetida.

Depois de trazer as mudanças do Git, faça **Build → Rebuild Project** no IntelliJ (ou `mvn clean`): classes antigas compiladas (`Usuario`, `Animal` com campos em português) ficam na pasta `target/` e impedem a API de subir.

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
| `FRONTEND_URL`        | Para onde a API volta depois do login Google | `http://localhost:5173` |
| `MP_DEV_ACCESS_TOKEN` | Token de teste do Mercado Pago, usado por todas as ONGs (provisório, até a migração 009) | — |
| `MP_SANDBOX`          | `true`: o pagador do PIX vai como `APRO` (o sandbox aprova sozinho) | `true` |
| `MP_WEBHOOK_SECRET`   | Assinatura secreta dos webhooks do Mercado Pago | — (sem ela, todo webhook vira 401) |
| `MP_BASE_URL`         | Endereço da API do Mercado Pago              | `https://api.mercadopago.com` |

Sem `JWT_SECRET`, a API sobe gerando uma chave aleatória por execução (registra um WARN no log): dá para desenvolver, mas todo restart invalida os tokens já emitidos. Defina a variável em qualquer ambiente que não seja a sua máquina.

**Conta admin.** Não existe cadastro público de admin, e a API não cria a conta ao subir. A conta `ADMIN` é inserida direto no banco (usuário com `user_type = 'ADMIN'`).

**Mercado Pago (provisório).** O contrato manda cada ONG conectar a própria conta do Mercado Pago pelo OAuth (migração 009, `api-mercado-pago-ongs.md`), mas isso ainda não existe. Até lá, a `PlaceholderMercadoPagoAccountGateway` trata toda ONG como conectada e usa um único `MP_DEV_ACCESS_TOKEN`. Sem ele, as campanhas funcionam normalmente, mas doar responde 400 "Esta ONG não está recebendo doações no momento.".

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
POST /api/auth/register/ngo
Content-Type: application/json

{
  "name": "ONG Amigos dos Animais",
  "email": "amigosdosanimais@gmail.com",
  "password": "senhaSegura123",
  "cnpj": "12.345.678/0001-95",
  "cep": "88900-000",
  "street": "Rua Caetano Lummertz",
  "number": "1250",
  "complement": null,
  "district": "Centro",
  "city": "Araranguá",
  "state": "SC",
  "photoUrl": null,
  "bio": "Resgatamos e encontramos lares para cães e gatos.",
  "instagram": "amigosdosanimais",
  "twitter": "amigosanimais",
  "facebook": "https://facebook.com/amigosdosanimais",
  "foundedYear": 2016
}
```

A ONG já nasce ativa e com `isVerified: true`: não há etapa de aprovação. Endereço é obrigatório; `photoUrl`, `bio`, redes sociais e `foundedYear` são opcionais. O contato público da ONG é o próprio e-mail de login. `foundedYear` precisa estar entre 1800 e o ano atual. CNPJ e CEP podem vir com ou sem máscara (são gravados só com dígitos) e Instagram/X com ou sem `@`.

### Cadastro de Usuário Comum
```
POST /api/auth/register/person
Content-Type: application/json

{
  "name": "Maria Silva",
  "email": "maria@email.com",
  "password": "senhaSegura123",
  "photoUrl": null
}
```

### Login (qualquer tipo de conta)
```
POST /api/auth/login
Content-Type: application/json

{ "email": "maria@email.com", "password": "senhaSegura123" }
```

Rota única para qualquer tipo de conta: o tipo vem em `user.userType` na resposta (`NGO`, `PERSON` ou `ADMIN`).

### Formato dos erros

Todos os erros seguem o mesmo formato. `message` já vem pronta para exibir (em português); `errors` só aparece em falhas de validação, com o motivo de cada campo:

```json
{
  "timestamp": "2026-09-20T18:14:01.173",
  "status": 400,
  "error": "Dados inválidos",
  "message": "CNPJ inválido",
  "errors": { "cnpj": "CNPJ inválido", "cep": "CEP em formato inválido" }
}
```

Todas as respostas de autenticação seguem o formato:

```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "user": {
    "id": 1,
    "name": "Maria Silva",
    "email": "maria@email.com",
    "userType": "PERSON",
    "provider": "LOCAL",
    "createdAt": "2026-09-20T18:14:01.173",
    "photoUrl": null,
    "coverUrl": null,
    "bio": null,
    "cnpj": null,
    "isVerified": false,
    "instagram": null,
    "twitter": null,
    "facebook": null,
    "foundedYear": null,
    "team": [],
    "visitingHours": [],
    "cep": null,
    "street": null,
    "number": null,
    "complement": null,
    "district": null,
    "city": null,
    "state": null
  }
}
```

Use o token nas requisições autenticadas: `Authorization: Bearer {token}`.

**Conta suspensa pela moderação** (`users.active = false`): o login (tradicional ou Google) responde **403** com `message` "Esta conta foi suspensa pela moderação do AUcolher." — só depois de a senha conferir. Um token emitido antes do banimento também deixa de valer na hora: qualquer requisição com ele responde o mesmo 403, e o frontend desloga com o aviso.

**Conta excluída pela própria pessoa** (`users.deleted_at` preenchida): o e-mail antigo não existe mais, então o login responde 401 como um e-mail desconhecido, e o token antigo vira uma requisição sem login (401 nas rotas protegidas). O mesmo e-mail (e, na ONG, o mesmo CNPJ) pode fazer um cadastro novo.

### Edição do perfil ("Minha conta")
```
PUT /api/users/me
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "ONG Amigos dos Animais",
  "photoUrl": null,
  "coverUrl": "data:image/jpeg;base64,...",
  "bio": "Resgatamos e encontramos lares para cães e gatos.",
  "instagram": "amigosdosanimais",
  "twitter": null,
  "facebook": "https://facebook.com/amigosdosanimais",
  "foundedYear": 2016,
  "team": [{ "name": "Marina Costa", "role": "Presidente" }],
  "visitingHours": [{ "days": "Terça a sexta", "hours": "14h às 18h" }],
  "cep": "88900-000",
  "street": "Rua Caetano Lummertz",
  "number": "1250",
  "complement": "Galpão B",
  "district": "Centro",
  "city": "Araranguá",
  "state": "SC"
}
```

A conta editada é sempre a do token — não há id na rota. É uma substituição completa: campo opcional que não vier é apagado, e `team`/`visitingHours` substituem as listas inteiras (na ordem enviada). Usuário comum grava só `name`, `photoUrl`, `bio`, `cep`, `city` e `state`; o resto é ignorado. Para ONG o endereço continua obrigatório.

`coverUrl` é o **banner** do perfil público, **só para ONG** e opcional: uma URL `http(s)://` ou um data URL `data:image/(jpeg|png|webp);base64,...` com até 600.000 caracteres. Ausente ou `null` remove o banner (por isso o frontend sempre manda o valor atual). Erros (400): conta de usuário comum com banner → "Só ONGs podem ter banner no perfil."; formato inválido → "Envie o banner como imagem JPG, PNG ou WebP."; grande demais → "O banner é grande demais.". E-mail, senha e CNPJ não mudam por aqui. Responde com o mesmo objeto `user` das rotas de autenticação — que também traz `team` e `visitingHours`, para o frontend não depender de nada guardado no navegador.

### Excluir a própria conta
```
POST /api/users/me/deletion
Authorization: Bearer {token}
Content-Type: application/json

{ "password": "minhaSenha1" }
```

Conta do Google sem senha confirma com `{ "confirmation": "EXCLUIR" }`. Responde **204** e não dá para desfazer. É POST, e não DELETE, porque DELETE com corpo é mal suportado por proxies e clientes HTTP.

Tudo numa transação, nesta ordem:
1. Os pedidos de adoção ativos (`PENDING`, `ACCEPTED`, `AWAITING_DELIVERY`) em que a conta é dona do animal ou adotante viram `CANCELLED` com `cancelReason: ACCOUNT_DELETED`.
2. Os animais `AVAILABLE` viram `INACTIVE`; os outros não mudam.
3. Na ONG, todas as campanhas viram exclusão lógica (`deleted`), e as ativas são encerradas antes. Os PIX pendentes são cancelados, e as doações aprovadas continuam no histórico de quem doou, com a campanha "removida".
4. Na ONG, os eventos futuros com confirmados viram `CANCELLED` (quem confirmou ainda os vê em `/api/events/attending`); os sem confirmados são apagados; os passados ficam.
5. O quiz do AUmatch, os favoritos e as presenças confirmadas são apagados.
6. A conta é anonimizada:
   - o nome vira "Conta excluída" e o e-mail, `excluida-{id}@aucolher.invalid`;
   - senha, foto, banner, bio, CNPJ, redes, ano de fundação, equipe, horários de visita e endereço são apagados;
   - o selo de verificada sai;
   - `active` vira `false` e `deleted_at` é preenchida.
   Pedidos concluídos, avaliações e doações das outras pessoas continuam, com o autor "Conta excluída".
7. As denúncias pendentes contra o perfil ou os anúncios da conta são arquivadas (`DISMISSED`, sem admin).

Erros: conta `ADMIN` → 403 "A conta de administrador não pode ser excluída."; senha ausente ou errada → 400 "Senha incorreta."; conta sem senha e `confirmation` diferente de `EXCLUIR` → 400 "Digite EXCLUIR para confirmar.".

### Perfil público
```
GET /api/users/{id}/public-profile
```

Público, sem login: é a página de perfil de uma ONG ou pessoa. Traz os mesmos campos do objeto `user` acima, menos `provider`. `coverUrl` vem sempre `null` para pessoa.
- ONG: perfil institucional completo, com endereço da sede, equipe, horários de visita e o `email` de login como contato público.
- Pessoa: só `city` e `state` do endereço; `email`, CEP, rua, número e os campos de ONG vêm nulos.
- Conta inexistente, desativada ou admin responde 404.
- **Exceção do admin:** a conta ADMIN ainda vê o perfil de uma conta banida (com `active: false`) e os animais disponíveis dela (`GET /api/users/{id}/animals`), para conferir e reativar a conta pelo site. Para todos os outros, o perfil banido continua 404.

### Login via Google (OAuth2)
```
GET /oauth2/authorization/google
```
Redirecione o usuário para essa URL no navegador. Após o consentimento, o Spring Security completa o fluxo OAuth2. No primeiro acesso, `CustomOAuth2UserService` cria o registro em `users` com `userType = PERSON`, `provider = GOOGLE` e a foto do Google. Um e-mail que já tem conta entra nela.

A API então **volta ao frontend** em `{FRONTEND_URL}/login/google`, com uma destas queries:
- `?code=...`: um código de uso único, válido por 60 s;
- `?error=suspended`: a conta foi banida;
- `?error=failed`: a pessoa cancelou no Google ou houve erro.

O token não vai na URL. O frontend troca o código:

```
POST /api/auth/google/exchange
{ "code": "..." }
```

- **200:** o mesmo `AuthResponseDTO` do `POST /api/auth/login`;
- **400:** código vazio, desconhecido, já usado ou vencido, com "O login com o Google expirou. Tente novamente.";
- **403:** conta suspensa.

Fluxo completo e como criar as credenciais: `docs/entrega-final/api-login-google.md` no frontend.

### Animais

Qualquer conta (ONG ou usuário comum) pode anunciar. A cidade/UF do animal são as do perfil de quem anuncia — preencha-as em `PUT /api/users/me` antes, senão o cadastro responde 400.

| Método e rota | Login | O que faz |
|---|---|---|
| `GET /api/animals` | não | Vitrine: só os `AVAILABLE`, do mais recente para o mais antigo, com filtros e paginação |
| `GET /api/animals/{id}` | não | Detalhes completos. Anúncio `INACTIVE` só aparece para o dono (para os outros, 404) |
| `POST /api/animals` | sim | Cadastra — responde 201 com o animal completo |
| `PUT /api/animals/{id}` | dono | Edição completa (mesmo corpo do cadastro). Animal adotado não pode ser editado |
| `PATCH /api/animals/{id}/status` | dono | `{ "status": "ADOPTED" }`, `"INACTIVE"` ou `"AVAILABLE"`. `ADOPTED` é definitivo |
| `DELETE /api/animals/{id}` | dono | Tira do ar (exclusão lógica: vira `INACTIVE`) — responde 204 |
| `GET /api/animals/mine` | sim | Todos os anúncios da conta logada, em qualquer status |
| `GET /api/users/{id}/animals` | não | Os disponíveis de um perfil público |

Quem não é dono recebe 403 nas rotas de alteração.

**Cadastro / edição** (`POST` e `PUT`):
```json
{
  "name": "Thor",
  "species": "DOG",
  "breed": "Vira-lata",
  "sex": "MALE",
  "ageValue": 3,
  "ageUnit": "YEARS",
  "size": "LARGE",
  "vaccinated": true,
  "neutered": true,
  "dewormed": true,
  "specialNeeds": false,
  "energyLevel": "HIGH",
  "temperament": "PROTECTIVE",
  "independenceLevel": "MODERATE",
  "vocalization": "MODERATE",
  "goodWithChildren": true,
  "goodWithDogs": true,
  "goodWithCats": false,
  "apartmentFriendly": false,
  "summary": "Protetor e brincalhão, pronto para uma nova aventura!",
  "story": "Thor foi resgatado ainda filhote...",
  "photos": ["data:image/jpeg;base64,...", "https://..."]
}
```

Valores aceitos:

| Campo | Valores |
|---|---|
| `species` | `DOG`, `CAT`, `OTHER` |
| `sex` | `MALE`, `FEMALE` |
| `size` | `SMALL`, `MEDIUM`, `LARGE` |
| `ageUnit` | `MONTHS` (0 a 11) ou `YEARS` (1 a 30) |
| `energyLevel`, `independenceLevel`, `vocalization` | `LOW`, `MODERATE`, `HIGH` |
| `temperament` | `CALM`, `PLAYFUL`, `AFFECTIONATE`, `PROTECTIVE`, `INDEPENDENT` |
| `photos` | de 1 a 4, URL http(s) ou data URL de imagem (mesmas regras da foto de perfil); a primeira é a capa |

Os itens de saúde (`vaccinated`, `neutered`, `dewormed`, `specialNeeds`) são opcionais e valem `false` quando não vêm. `ageGroup` (`PUPPY`, `ADULT`, `SENIOR`) não é enviado: a API calcula pela idade (menos de 1 ano é filhote; a partir de 8 anos, idoso).

A resposta do cadastro, da edição e de `GET /api/animals/{id}` traz esses campos mais `id`, `ageGroup`, `status`, `city`, `state`, `owner` (`id`, `name`, `photoUrl`, `userType`, `isVerified`), `createdAt` e `updatedAt`.

**Listagem** — filtros opcionais na query string. Os de múltipla escolha têm nome no plural e podem se repetir:
```
GET /api/animals?species=DOG&species=CAT&sizes=SMALL&city=Araranguá&page=0&size=12
```

Filtros: `search` (nome, raça ou cidade), `species`, `sizes`, `sexes`, `ageGroups`, `energyLevels`, `temperaments`, `specialNeeds=true`, `city`, `state`. Paginação: `page` começa em 0; `size` padrão 12, máximo 50. Atenção: `size` é o tamanho da página — o filtro de porte é `sizes`.

```json
{
  "content": [ { "id": 1, "name": "Thor", "coverPhoto": "data:image/jpeg;base64,...", "city": "Araranguá", "...": "..." } ],
  "page": 0,
  "size": 12,
  "totalElements": 1,
  "totalPages": 1
}
```

Os cards (listagem, meus animais, perfil e favoritos) trazem só a `coverPhoto` e não trazem a `story` nem a `vocalization` — a página de detalhes busca o animal completo. O `independenceLevel` vem no card porque entra no cálculo de compatibilidade do AUmatch.

### Favoritos

Todas exigem login.

| Método e rota | O que faz |
|---|---|
| `GET /api/favorites` | Cards dos animais favoritados, do mais recente para o mais antigo. Inativos somem; adotados continuam, com `status: ADOPTED` |
| `GET /api/favorites/ids` | Só os ids — para marcar os corações nos cards |
| `PUT /api/favorites/{animalId}` | Favorita. Repetir não duplica — responde 204 |
| `DELETE /api/favorites/{animalId}` | Desfavorita — responde 204 |

### Eventos

| Método e rota | Quem acessa | O que faz |
|---|---|---|
| `GET /api/events?ngoId=` | Público | Eventos ativos de hoje em diante; `ngoId` filtra por ONG |
| `GET /api/events/{id}` | Público | Detalhe; evento passado abre normalmente |
| `GET /api/events/mine` | ONG | Eventos da ONG logada, inclusive os passados |
| `GET /api/events/attending` | Logado | Eventos em que a pessoa confirmou presença (passados e cancelados inclusos) |
| `POST /api/events` | ONG | Cria o evento — 201 |
| `PUT /api/events/{id}` | ONG dona | Substitui todos os campos |
| `DELETE /api/events/{id}` | ONG dona | Exclui; com confirmados vira `CANCELLED` — 204 |
| `GET /api/events/{id}/attendees` | ONG dona | Lista de confirmados |
| `POST /api/events/{id}/attendance` | Logado | Confirma presença (idempotente) e devolve o evento |
| `DELETE /api/events/{id}/attendance` | Logado | Cancela a presença (idempotente) |

Categorias: `ADOPTION_FAIR`, `HEALTH`, `BAZAAR`, `WORKSHOP`. A data não pode estar no passado, o término precisa ser depois do início, a capacidade não pode ficar abaixo dos confirmados, e a ONG não confirma presença no próprio evento.

### Campanhas e doações

Contrato completo em `docs/entrega-final/api-campanhas-doacoes.md`, no frontend.

| Método e rota | Quem acessa | O que faz |
|---|---|---|
| `GET /api/campaigns?ngoId=&status=` | Público | `status` = `ACTIVE` (padrão) ou `CLOSED`; nunca devolve excluídas nem de ONG banida; `createdAt` decrescente |
| `GET /api/campaigns/{id}` | Público | Detalhe; encerrada abre normalmente. 404 se excluída ou de ONG banida |
| `GET /api/campaigns/mine` | ONG | Ativas e encerradas da ONG logada, sem as excluídas |
| `POST /api/campaigns` | ONG | Cria — 201. 400 sem conta Mercado Pago; 409 se ligar a urgência com outra urgente ativa |
| `PUT /api/campaigns/{id}` | ONG dona | Substitui todos os campos; 400 se encerrada |
| `POST /api/campaigns/{id}/close` | ONG dona | Encerra e cancela os PIX pendentes; 400 se já encerrada |
| `DELETE /api/campaigns/{id}` | ONG dona | Sem doação aprovada, exclusão real; com, exclusão lógica (`deleted`). Cancela os PIX pendentes — 204 |
| `POST /api/campaigns/{id}/donations` | Logado | `{ "amount": 50 }`: cria a doação `PENDING` e o PIX (Orders API) — 201. Admin → 403 |
| `GET /api/donations/{id}` | Doador | Se `PENDING`, consulta a order no Mercado Pago antes (polling do modal) |
| `GET /api/donations/mine` | Logado | Histórico de quem doou, em qualquer status |
| `GET /api/donations/received` | ONG | Só `APPROVED`, nas campanhas da ONG (inclusive excluídas), com `donor` |
| `POST /api/webhooks/mercadopago` | Mercado Pago | Confere o `x-signature` (HMAC-SHA256) e aprova a doação; 401 com assinatura inválida |

Categorias: `HEALTH`, `FOOD`, `INFRASTRUCTURE`, `NEUTERING`, `RESCUE` e `OTHER` (com o nome em `customCategory`). Valores em reais inteiros: meta de R$ 50 a R$ 1.000.000, doação de R$ 5 a R$ 100.000. A campanha com prazo vencido conta como `CLOSED` (calculado na leitura, sem job), e o PIX vence em 30 minutos (`EXPIRED`, também calculado). `raisedAmount` e `donationsCount` somam só as `APPROVED`, e a aprovação é um `UPDATE` atômico, para o webhook e o polling nunca contarem duas vezes.

### Pedidos de adoção

Todas exigem login. Enviar um pedido é para **pessoa ou ONG** (o admin recebe 403). O dono do pedido é sempre o dono do animal.

| Método e rota | Quem | O que faz |
|---|---|---|
| `POST /api/adoption-requests` | pessoa ou ONG | `{ "animalId": 12 }` — cria o pedido (`PENDING`) e responde 201 |
| `GET /api/adoption-requests/sent` | quem pediu | Os pedidos que a conta enviou ("Meus interesses") |
| `GET /api/adoption-requests/received` | dono | Os pedidos dos animais da conta, em qualquer status |
| `GET /api/adoption-requests/{id}` | as duas partes | Um pedido |
| `PATCH /api/adoption-requests/{id}/status` | depende da transição | `{ "status": "ACCEPTED" }` — ver a tabela abaixo |

Regras do envio:
- o animal precisa estar `AVAILABLE`; o próprio dono não pede (403);
- um pedido "vivo" por pessoa e animal: com outro pedido não cancelado para o mesmo animal, responde 400 ("Você já enviou um pedido..." ou "O dono já recusou o seu pedido..."). **Recusado é definitivo; só um pedido cancelado libera um novo.** O banco garante isso com um índice único parcial.

Transições (papel errado responde 403; status de partida errado, 400; repetir o status atual não muda nada):

| De → Para | Quem | Efeito |
|---|---|---|
| `PENDING` → `ACCEPTED` | dono | Libera o chat |
| `PENDING` → `REJECTED` | dono | Definitivo para aquele animal |
| `ACCEPTED` → `AWAITING_DELIVERY` | dono | O dono confirmou a entrega; trava o chat |
| `AWAITING_DELIVERY` → `CONCLUDED` | adotante | Numa transação só: o animal vira `ADOPTED` e os outros pedidos ativos dele são cancelados (`ANIMAL_ADOPTED`) |
| `PENDING` / `ACCEPTED` / `AWAITING_DELIVERY` → `CANCELLED` | adotante | Desistiu (`ADOPTER_WITHDREW`) |
| `ACCEPTED` / `AWAITING_DELIVERY` → `CANCELLED` | dono | Desistiu (`OWNER_WITHDREW`); pendente o dono recusa |

O dono pode aceitar vários pedidos do mesmo animal; vale o primeiro que concluir. Quando ele marca o animal como adotado por fora, tira do ar ou exclui o anúncio, os pedidos ativos são cancelados com `ANIMAL_UNAVAILABLE`. Todo cancelamento guarda `cancelReason`, `cancelledFromStatus` e `closedAt`.

```json
{
  "id": 8,
  "status": "PENDING",
  "cancelReason": null,
  "cancelledFromStatus": null,
  "createdAt": "2026-10-13T07:00:19.840718",
  "updatedAt": "2026-10-13T07:00:19.840718",
  "concludedAt": null,
  "closedAt": null,
  "animal": { "id": 4, "name": "Mel", "owner": { "id": 2, "name": "ONG Dona", "...": "..." }, "...": "..." },
  "adopter": { "id": 4, "name": "Maria", "photoUrl": null, "userType": "PERSON", "city": "Criciúma", "state": "SC",
               "matchProfile": { "housing": "APARTMENT", "...": "...", "complete": true } },
  "matchScore": 85,
  "matchScoreAtAdoption": null,
  "return": null,
  "reviews": { "adopter": null, "owner": null }
}
```

- `animal` é o card da listagem (`AnimalSummaryDTO`), com o dono em `animal.owner`.
- `adopter.matchProfile` traz as respostas do quiz do AUmatch (nulo para ONG ou para quem não respondeu); `matchScore` é a compatibilidade atual entre elas e o animal, calculada pelo `MatchScoreCalculator`.
- `matchScoreAtAdoption` é o score **congelado** quando o pedido vira `CONCLUDED`: não muda se o quiz ou o animal mudarem depois. É nulo sem o quiz completo (as 7 respostas), para ONG adotando e nos pedidos concluídos antes da migração 012.
- `return` traz o retorno ou o repasse (abaixo); é nulo fora de `RETURNED` e `REHOMED`.
- `reviews` traz as avaliações das duas partes (ver abaixo).

### Retorno e repasse (taxa de reabandono)

Uma adoção `CONCLUDED` pode não dar certo por dois caminhos, que contam igual na taxa de reabandono do painel do admin. Vale o primeiro: depois do retorno o adotante não republica, e depois do repasse o doador não registra retorno. **Adoção feita por ONG fica fora de tudo** (é resgate, não adoção final): as rotas respondem 400 "Adoções feitas por ONG não têm retorno nem repasse".

| Método e rota | Quem | O que faz |
|---|---|---|
| `POST /api/adoption-requests/{id}/return` | dono do animal | `{ "returnDate": "2026-11-03", "reason": "OTHER_PETS", "note": "..." }` — o pedido vira `RETURNED` e o anúncio volta para o doador fora do ar (`ADOPTED → INACTIVE`, a única saída de `ADOPTED`). Repetir num pedido já `RETURNED` corrige os dados sem mexer no animal |
| `GET /api/adoption-requests/{id}/rehome-draft` | adotante | O anúncio original no formato do cadastro (`AnimalRequestDTO`, fotos incluídas), para o formulário de "Republicar anúncio" abrir preenchido. Só em `CONCLUDED` |
| `POST /api/adoption-requests/{id}/rehome` | adotante | `{ "reason": "HOUSING_OR_ROUTINE", "note": "...", "animal": { ...cadastro } }` — numa transação, nasce o anúncio novo (`AVAILABLE`, em nome do adotante, com `originRequestId` = este pedido) e o pedido vira `REHOMED`. O `animal` passa pelas mesmas validações do `POST /api/animals`. O anúncio original continua `ADOPTED` |
| `POST /api/adoption-requests/{id}/rehome/undo` | adotante | "Desisti de doar": enquanto o anúncio novo não foi adotado, ele sai do ar (os pedidos ativos dele são cancelados com `ANIMAL_UNAVAILABLE`) e perde o vínculo, e o pedido volta a `CONCLUDED`. Tirar do ar ou excluir o anúncio novo **não** desfaz o repasse |

- `reason`: `BEHAVIOR`, `OTHER_PETS`, `CHILDREN`, `HOUSING_OR_ROUTINE`, `COSTS`, `ANIMAL_HEALTH` ou `OTHER` (que exige `note`). `note` tem até 500 caracteres.
- A data não pode ser futura nem anterior à conclusão.
- O `PATCH .../status` não aceita `RETURNED` nem `REHOMED`: cada um tem a sua ação.

```json
"return": {
  "type": "RETURNED",
  "date": "2026-11-03",
  "reason": "OTHER_PETS",
  "note": "Não se adaptou com os dois gatos da casa.",
  "recordedAt": "2026-11-03T18:20:00",
  "rehomedAnimalId": null
}
```

`type` repete o status; em `REHOMED`, `date` é o dia da republicação e `rehomedAnimalId` é o anúncio novo. Os cards de animal (`AnimalSummaryDTO` e `AnimalDetailDTO`) trazem `originRequestId`: nulo, ou o pedido de onde veio um anúncio republicado.

### Avaliações

Depois que o pedido é concluído, cada parte pode avaliar a outra uma vez: o adotante avalia o dono (`reviews.adopter`) e o dono avalia o adotante (`reviews.owner`). O autor edita ou apaga a própria avaliação quando quiser.

| Método e rota | Login | O que faz |
|---|---|---|
| `PUT /api/adoption-requests/{id}/review` | parte do pedido | `{ "rating": 5, "comment": "..." }` — cria ou edita a minha avaliação e responde com o pedido. Só depois da adoção (`CONCLUDED`, `RETURNED` ou `REHOMED`) |
| `DELETE /api/adoption-requests/{id}/review` | parte do pedido | Apaga a minha avaliação — responde 204 |
| `GET /api/users/{id}/reviews` | não | Resumo público de um perfil |

`rating` vai de 1 a 5 e `comment` é opcional, com até 500 caracteres. `updatedAt` só vem preenchida depois de uma edição.

```json
{
  "average": 4.5,
  "count": 2,
  "distribution": { "5": 1, "4": 1, "3": 0, "2": 0, "1": 0 },
  "adoptionsCount": 3,
  "reviews": [
    { "id": 1, "requestId": 8, "rating": 5, "comment": "Ótima ONG!", "animalName": "Thor",
      "author": { "id": 4, "name": "Maria", "photoUrl": null, "isOng": false },
      "createdAt": "2026-10-13T07:00:20", "updatedAt": null }
  ]
}
```

`adoptionsCount` conta as adoções que aconteceram (`CONCLUDED`, `RETURNED` ou `REHOMED`) em que a conta participou, como dona do animal ou como adotante.

### Denúncias

Qualquer conta logada de pessoa ou ONG denuncia um anúncio de animal (`ANIMAL`) ou um perfil (`USER`). O admin não denuncia (403): ele modera direto pelo painel.

| Método e rota | Quem | O que faz |
|---|---|---|
| `POST /api/reports` | pessoa ou ONG | `{ "targetType": "ANIMAL", "targetId": 8, "reason": "SCAM", "description": "..." }` — responde 201 com a denúncia (`PENDING`) |
| `GET /api/reports/mine` | logado | As denúncias da própria conta, mais recentes primeiro |

- `reason`: `FAKE_PROFILE` (só perfil), `SCAM`, `ANIMAL_ABUSE`, `IMPROPER_CONTENT`, `SPAM` ou `OTHER` (que exige `description`, até 500 caracteres).
- Ninguém denuncia o próprio anúncio ou o próprio perfil (400). Alvo inexistente ou fora do site: 404.
- **Uma denúncia em análise por pessoa e alvo:** a segunda responde **409**; depois de julgada, a pessoa pode denunciar de novo. O índice `uq_reports_pending` garante isso também com dois cliques ao mesmo tempo.
- A denúncia guarda uma **cópia do alvo** (`target`): nome, foto, espécie e dono (anúncio) ou nome, foto, tipo, cidade e UF (perfil). O admin continua vendo o que foi denunciado depois de o anúncio sair do ar.

### Admin

Tudo em `/api/admin/**` é só da conta ADMIN: sem token, 401; com token de pessoa ou ONG, 403. As listas não são paginadas (o volume do TCC é pequeno; a busca e os filtros ficam no frontend).

**O admin modera, mas não participa.** Ele remove, bane, reativa e **edita** contas, anúncios, eventos e campanhas de outras pessoas (pelas rotas abaixo). O conteúdo editado continua em nome do dono, e fica registrado quando e por quem foi editado (`moderated_at` / `moderated_by`, migração 016): o dono vê "Editado pela moderação". Fora do painel, as rotas de participante respondem 403 para a conta ADMIN:

| Rota | `message` |
|---|---|
| `POST`/`DELETE /api/events/{id}/attendance` | "Contas de admin não confirmam presença em eventos." |
| `PUT /api/favorites/{animalId}` | "Contas de admin não favoritam animais." |
| `POST /api/animals` | "Contas de admin não anunciam animais." |
| `POST /api/adoption-requests`, `/api/match/**` | (regra da SecurityConfig) |
| `POST /api/reports` | "Contas de admin não fazem denúncias: a moderação é feita pelo painel." |
| `POST /api/campaigns/{id}/donations` | "Contas de admin não fazem doações." |

| Método e rota | O que faz |
|---|---|
| `GET /api/admin/stats` | Números e gráficos da Visão geral (abaixo) |
| `GET /api/admin/users` | Contas de pessoas e ONGs (sem admins), mais recentes primeiro, com `active`, `animalsCount` e `deletedAt` (preenchida na conta que a própria pessoa excluiu, que aparece como "Conta excluída") |
| `GET /api/admin/animals` | Todos os anúncios, em qualquer status: o card da vitrine (com `owner.active`) + `listingStatus` (`AVAILABLE`, `IN_PROGRESS`, `ADOPTED`, `INACTIVE` ou `REMOVED`). `IN_PROGRESS` = disponível com pedido aceito ou aguardando entrega |
| `GET /api/admin/events` | Todos os eventos, da data mais distante para a mais antiga, com `ngo.active` e `removedByModeration` |
| `GET /api/admin/campaigns` | Todas as campanhas (ativas, encerradas e excluídas), mais recentes primeiro, com `removed`, `removedByModeration`, `moderatedAt` e `ngo.active` |
| `GET /api/admin/adoption-requests` | Todos os pedidos, enxutos: status, motivo, datas, o animal (`id`, `name`, `species`, `status`, `coverPhoto`), o dono e o adotante |
| `GET /api/admin/reports` | Todas as denúncias, em qualquer situação, com quem denunciou (`reporter`) |
| `POST /api/admin/reports/{id}/dismiss` | Arquiva o caso (esta denúncia e as outras pendentes do mesmo alvo) e devolve as arquivadas |
| `POST /api/admin/reports/{id}/ban` | Bane a conta por trás do caso (o perfil denunciado ou o dono do anúncio) e devolve as denúncias resolvidas |
| `PATCH /api/admin/users/{id}/status` | `{ "active": false }` bane, `{ "active": true }` reativa; devolve a conta. Conta excluída não volta: 400 "Contas excluídas não podem ser reativadas." |
| `GET /api/admin/users/{id}` | A conta inteira para editar: os campos de "Minha conta" (com o `email` de login e `moderatedAt`) + `active` |
| `PUT /api/admin/users/{id}` | `{ "email": "...", "profile": { ...o corpo de PUT /api/users/me } }` — edita o perfil e o e-mail de login; conta excluída → 400 "Contas excluídas não podem ser editadas." |
| `PUT /api/admin/animals/{id}` | O corpo de `PUT /api/animals/{id}` — edita o anúncio (ativo, fora do ar ou adotado; removido não muda) |
| `PUT /api/admin/events/{id}` | O corpo de `PUT /api/events/{id}` — edita o evento, com as regras da ONG (só ativo e que ainda não aconteceu) |
| `PUT /api/admin/campaigns/{id}` | O corpo de `PUT /api/campaigns/{id}` — edita a campanha, com as regras da ONG (não edita encerrada, uma urgente ativa por ONG); 404 se excluída |
| `DELETE /api/admin/animals/{id}` | Remove o anúncio (`REMOVED`) — 204 |
| `DELETE /api/admin/events/{id}` | Remove o evento (`CANCELLED` + `removedByModeration`) — 204 |
| `DELETE /api/admin/campaigns/{id}` | Remove a campanha (exclusão lógica + `removedByModeration`, mesmo sem doações) e cancela os PIX pendentes — 204 |

**Banir** (numa transação): a conta para de entrar e o token dela deixa de valer; as denúncias pendentes contra o perfil e contra os anúncios dela são resolvidas (`ACCOUNT_BANNED`); os pedidos ativos em que ela é adotante ou dona são cancelados (`ACCOUNT_BANNED`). Os animais, eventos e campanhas dela **não mudam**: somem das rotas públicas (vitrine, perfil, favoritos, deck, detalhe, eventos, campanhas e doação) e voltam se a conta for reativada. Pedidos cancelados e denúncias resolvidas não voltam. Contas admin não podem ser banidas (400).

**Editar uma conta:** valem as mesmas regras de "Minha conta" (a ONG precisa do endereço completo). Contas admin não são editadas (400). O e-mail novo precisa ser único (409 "Este e-mail já está em uso por outra conta."), e conta que entra pelo Google não troca de e-mail (400), porque o próximo login pelo Google criaria outra conta. **Trocar o e-mail derruba a sessão da conta** (o token é pelo e-mail): ela entra de novo com o e-mail novo.

**Remover anúncio:** vira `REMOVED`, sai do site (o detalhe responde 404 para todos, menos para o admin), os pedidos ativos são cancelados (`LISTING_REMOVED`) e as denúncias contra ele são resolvidas. O dono continua vendo o anúncio em `/api/animals/mine`, mas `PUT`, `PATCH /status` e `DELETE` respondem 403. Animal adotado não é removido (fica como histórico). Não há desfazer.

**Visão geral** (`GET /api/admin/stats`) — as séries mensais cobrem os últimos 12 meses (`"YYYY-MM"`), com zero onde não houve nada:

| Bloco | O que conta |
|---|---|
| `users` | Pessoas e ONGs; `newByMonth` pela data de cadastro |
| `animals` | `available`, `inProgress` e `bySpecies` sem os anúncios fora do ar e sem os de conta banida; `adopted` e `removed` contam sempre |
| `adoptions` | O funil (cada etapa conta quem chegou nela ou passou dela; retorno e repasse contam como adoção) e `concludedByMonth` |
| `events` | `upcoming`/`past` (ativos de ONG ativa), `cancelled` e `suspended` (de ONG banida) |
| `donations` | Só `APPROVED`: `totalRaised`, `approvedCount` e `raisedByMonth` pela data de aprovação, em reais inteiros (as de campanhas excluídas continuam contando) |
| `campaigns` | `active`/`closed` (sem excluídas e sem as de ONG banida), `removed` (excluídas que continuam no banco) e `suspended` (de ONG banida) |
| `ngoRanking.byRaised` | As 5 ONGs que mais arrecadaram (só com valor maior que zero), com as adoções de cada uma |
| `ngoRanking.byAdoptions` | As 5 ONGs com mais adoções de animais delas |
| `recentActivity` | Os 8 acontecimentos mais recentes: `ACCOUNT_CREATED`, `ADOPTION_CONCLUDED`, `EVENT_CREATED`, `CAMPAIGN_CREATED` |
| `reports` | `pending` (denúncias em análise) e `pendingTargets` (alvos diferentes, os "casos") |
| `returns` | A **taxa de reabandono**, sem as adoções feitas por ONG: `adoptions` (`CONCLUDED` + `RETURNED` + `REHOMED`), `returned`, `rehomed`, `rate` (% com uma casa, nula sem adoções), `byRange` pelo score congelado (`HIGH` 80–100, `MEDIUM` 50–79, `LOW` 0–49), `noScore` e `byReason` (sempre as 7 chaves) |

### AUmatch

Só pessoa física (`PERSON`), que é quem adota: sem token responde 401, e ONG ou admin recebem 403.

| Método e rota | O que faz |
|---|---|
| `GET /api/match/profile` | Respostas do quiz. Quem ainda não respondeu recebe tudo `null` e `complete: false` |
| `PUT /api/match/profile` | Grava as respostas. Substituição completa: pergunta que não vier fica sem resposta |
| `GET /api/match/deck` | Animais disponíveis, do maior para o menor score, com os pontos de cada critério |

**Perfil** (corpo do `PUT` e resposta do `GET`):
```json
{
  "housing": "APARTMENT",
  "exerciseRoutine": "ACTIVE",
  "timeAway": "REGULAR",
  "hasChildren": true,
  "hasDogs": false,
  "hasCats": true,
  "speciesPreference": "DOG",
  "idealPetProfile": "CALM_COMPANION",
  "preferredSize": "SMALL"
}
```

A resposta traz também `complete`: `true` quando as 7 perguntas têm resposta.
- Todos os campos são opcionais, porque "Minha conta" salva respostas soltas.
- `hasChildren`, `hasDogs` e `hasCats` respondem juntos a "Quem mora com você?": vêm os três ou nenhum, senão a API responde 400 com `errors.householdValid`.
- "Ninguém em casa" = os três `false`.

| Campo | Valores |
|---|---|
| `housing` | `APARTMENT`, `HOUSE_WITH_YARD`, `FARM` |
| `exerciseRoutine` | `SEDENTARY`, `ACTIVE`, `VERY_ACTIVE` |
| `timeAway` | `SHORT` (até 4h), `REGULAR` (4h a 8h), `LONG` (mais de 8h) |
| `speciesPreference` | `DOG`, `CAT`, `BOTH` |
| `idealPetProfile` | `CALM_COMPANION`, `PLAYFUL_ACTIVE`, `PROTECTIVE_INDEPENDENT` |
| `preferredSize` | `SMALL`, `MEDIUM`, `LARGE` |

**Deck** — lista de cards, sem paginação:
```json
[
  {
    "animal": { "id": 12, "name": "Thor", "coverPhoto": "...", "independenceLevel": "HIGH", "...": "..." },
    "matchScore": 85,
    "criteria": [
      { "key": "HOUSING", "points": 15, "maxPoints": 15, "answered": true },
      { "key": "ENERGY", "points": 8, "maxPoints": 20, "answered": true }
    ]
  }
]
```

- Entram os animais `AVAILABLE` de contas ativas, menos os da própria pessoa e os que ela já pediu (qualquer pedido que não esteja cancelado).
- `speciesPreference` `DOG` ou `CAT` deixa só essa espécie; `BOTH` ou sem resposta mostra todas.
- No empate de score, o anúncio mais recente vem primeiro.
- `animal` é o mesmo card da listagem de animais.

**O algoritmo** (`MatchScoreCalculator`) soma seis critérios, num total de 100:

| Critério (`key`) | Máx | Regra |
|---|---|---|
| `HOUSING` | 15 | Apartamento: 15 se o animal é `apartmentFriendly`, senão 0. Casa, sítio ou sem resposta: 15 |
| `ENERGY` | 20 | A rotina vira um nível (`SEDENTARY` LOW, `ACTIVE` MODERATE, `VERY_ACTIVE` HIGH), comparado com `energyLevel` |
| `INDEPENDENCE` | 20 | O tempo fora vira um nível (`SHORT` LOW, `REGULAR` MODERATE, `LONG` HIGH), comparado com `independenceLevel` |
| `LIVING` | 15 | Cada grupo em casa (crianças, cães, gatos) precisa do `goodWith...` correspondente: 15 × compatíveis ÷ grupos. Ninguém ou sem resposta: 15 |
| `TEMPERAMENT` | 15 | `CALM_COMPANION` aceita CALM ou AFFECTIONATE; `PLAYFUL_ACTIVE`, PLAYFUL; `PROTECTIVE_INDEPENDENT`, PROTECTIVE ou INDEPENDENT |
| `SIZE` | 15 | `preferredSize` comparado com `size`. Sem porte preferido, quem mora em apartamento conta como `SMALL` |

- Nas escalas (energia, independência e porte), igual vale o máximo, vizinho vale 40% (8 ou 6 pontos) e oposto vale 0.
- Os demais critérios sem resposta valem 0, então quem ainda não fez o quiz começa com 30.
- O frontend tem uma cópia do cálculo em `features/aumatch/utils/matchScore.js`: ao mudar uma regra, mude as duas.

## Decisões de projeto

- **Código em inglês, textos em português**: classes, campos, JSON, rotas, valores de enum, tabelas e colunas usam inglês, o padrão de mercado. Mensagens de erro (lidas pelo usuário final), comentários e documentação ficam em português. Termos brasileiros sem tradução exata (`cnpj`, `cep`) mantêm o nome.
- **JWT stateless**: como o requisito pede desativação de CSRF e uma API REST pura, optei por sessão stateless com JWT em vez de sessão HTTP tradicional — é o padrão de mercado para esse tipo de API e evita problemas de CORS/cookies com clientes SPA/mobile.
- **Senha nula para contas OAuth2**: usuários criados via Google não têm senha própria; o login tradicional (`/login`) rejeita essas contas com uma mensagem explicativa.
- **Normalização nos DTOs**: os construtores compactos dos records limpam os campos (trim, vazio vira null, CNPJ/CEP só com dígitos, rede social sem `@`, link com protocolo) antes da validação. A Service recebe dados já canônicos e cuida só da regra de negócio.
- **CNPJ pelo Bean Validation**: `@CNPJ` do Hibernate Validator confere os dígitos verificadores; um `@Pattern` extra barra sequências repetidas (`00.000.000/0000-00`), que o `@CNPJ` sozinho aceita.
- **Admin sem cadastro público**:
  - a conta `ADMIN` é inserida direto no banco, então nenhuma senha fica no repositório;
  - `/api/admin/**` exige `ROLE_ADMIN`: sem token responde 401, e com token de pessoa ou ONG responde 403;
  - o repasse interno para `/error` é liberado (`DispatcherType.ERROR`). Sem isso, todo 403 da camada de segurança chegava ao cliente como 401, e o frontend desloga no 401.
- **JWT mínimo**: o token carrega apenas o e-mail (subject) e a validade. Papel e id não viram claim para não circular desatualizados — quem responde por eles é o banco, relido a cada requisição autenticada.
- **`ddl-auto=validate`**: o schema vem do `sql/schema.sql`; o Hibernate só confere se o banco bate com as entidades, em vez de alterar tabelas sozinho. Mudanças em bancos já existentes vão em `sql/migrations/`, numeradas e transacionais.
- **Constraints de banco** (`CHECK`) reforçam no schema.sql as mesmas regras já validadas na aplicação (ONG exige CNPJ; contas LOCAL exigem senha; valores dos enums dos animais) como camada extra de integridade.
- **Localização do animal = a do dono**: `animals` não tem cidade/UF; a listagem filtra pelo endereço do perfil de quem anunciou. **Sem geolocalização por decisão do projeto**: nem `users` nem `animals` guardam latitude/longitude, e não há filtro nem ordenação por distância ("perto de mim"). A busca por local é só por cidade/UF.
- **Exclusão lógica de animais**: `DELETE` só muda o status para `INACTIVE`, preservando o histórico (favoritos e pedidos de adoção) — e os pedidos ativos do animal são cancelados.
- **Cards leves**: a listagem busca só a foto de capa (numa consulta única) e carrega o dono junto, então uma página custa duas consultas SQL, com payload pequeno mesmo com fotos em data URL.
- **Favoritar é idempotente no banco**: `INSERT ... ON CONFLICT DO NOTHING` garante um favorito por par usuário/animal mesmo com cliques simultâneos.
- **Score do AUmatch no backend**: o algoritmo roda na API (`MatchScoreCalculator`, coberto por testes) e o deck já chega ordenado. Como o score depende de cada animal, não dá para ordenar no banco: o deck carrega os candidatos e ordena em memória, sem paginação, o que basta para o volume do projeto.
- **Perfil AUmatch em tabela própria** (`match_profiles`, 1:1 com `users`): o quiz é só de pessoa física e pode mudar sem mexer no cadastro.
- **Swipes não são gravados**: o "passar" vale só na sessão do navegador; o "curtir" é um pedido de adoção (`POST /api/adoption-requests`), e o deck esconde os animais já pedidos.
- **Status do pedido validado no servidor**: cada transição tem papel e status de partida certos (`AdoptionRequestService.changeStatus`). O frontend só esconde os botões; quem decide é a API.
- **"Recusado é definitivo" no banco**: o índice único parcial `uq_adoption_requests_open (adopter_id, animal_id) WHERE status <> 'CANCELLED'` impede um segundo pedido vivo mesmo com dois cliques ao mesmo tempo.
- **Concluir trava o animal**: a conclusão lê o animal com `SELECT ... FOR UPDATE`. Se dois adotantes confirmarem a chegada ao mesmo tempo, o segundo encontra o animal já adotado e o próprio pedido já cancelado.
- **Pedidos sem dependência circular**: tirar o animal do ar publica um `AnimalLeftListingEvent`, e o módulo de pedidos cancela os ativos na mesma transação — o `AnimalService` não conhece os pedidos.
- **Sem cópia congelada**: o pedido lê o adotante, o animal e as respostas do quiz ao vivo, então o dono vê sempre a compatibilidade atual.
- **Vagas de evento sem corrida**: confirmar presença, editar e excluir leem o evento com `SELECT ... FOR UPDATE`, então dois cliques ao mesmo tempo não passam da capacidade.
- **Evento com confirmados não some**: excluir um evento que já tem presenças só muda o status para `CANCELLED`, e quem confirmou continua vendo o evento em `/api/events/attending`.
- **Exclusão de conta é anonimização**: a linha de `users` fica, porque pedidos, avaliações e doações das outras pessoas apontam para ela. Quando o chat (011), a recuperação de senha (010) e a conta Mercado Pago de cada ONG (009) chegarem, `AccountDeletionService` precisa apagar também as marcas de leitura, os links de senha e a conta vinculada.
- **Ainda fora da API**: o chat (o frontend ainda simula as mensagens), a recuperação de senha e a conexão de cada ONG com o próprio Mercado Pago.
