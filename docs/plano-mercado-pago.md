# Plano: fechar a API de pagamento do Mercado Pago (backend)

> Criado em 04/11/2026. Parte do front: `docs/planos-front/plano-mercado-pago.md`, no repositório do front.
>
> Contrato: `docs/entrega-final/api-mercado-pago-ongs.md` e `api-campanhas-doacoes.md`, também no front. Se este plano e o contrato divergirem, o contrato vale, exceto nos pontos que este plano manda atualizar.

## 1. Objetivo e decisões

O dinheiro das doações deve cair direto na conta Mercado Pago de cada ONG (regras §10.3 e texto do TCC). Hoje isso ainda não acontece.

| Decisão | Escolha |
|---|---|
| Meta para o TCC | Demonstração em sandbox + 1 Pix real de R$ 5, feito uma vez, para provar o crédito na conta |
| Prazo | Menos de 2 semanas: primeiro a doação de ponta a ponta, depois o resto |
| Hospedagem | Começa local, com túnel. Publicar é opcional e só troca as URLs |
| Conta e aplicação no Mercado Pago | João cria (seção 6). Segredos nunca vão para o chat nem para o Git |
| Execução | Claude nos dois repositórios, um bloco por vez, em branch. Push e PR ficam com vocês |

**Avisar o Pedro:** enquanto este plano roda, evitar mudanças em `payment/`, `donation/` e `campaign/`.

## 2. Situação hoje

| Parte | Situação |
|---|---|
| Campanhas, doações, acompanhamento do Pix (`GET /donations/{id}`) | Prontos, mas nunca rodaram com credenciais reais |
| Doação com token único de teste (`MP_DEV_ACCESS_TOKEN`) | **Testável hoje, sem mudar código.** Só falta o token |
| Conectar a conta da ONG (`/api/ngo/mercado-pago`, callback OAuth) | Código pronto, nunca testado de verdade; sem tela no front |
| Doação usando o token de cada ONG | **Não ligada:** `PlaceholderMercadoPagoAccountGateway` trata toda ONG como conectada |
| Webhook definitivo | `payment/MercadoPagoWebhookController` está sem `@RestController`. Quem responde é o de `donation/` |
| Migrações e tabelas | Completas: a 009 já está no Aiven. **Não há migração nova neste plano** |

## 3. Problemas a corrigir

Encontrados na leitura do código, ainda sem execução.

| # | Problema | Onde | Correção | Bloco |
|---|---|---|---|---|
| 1 | **Pode perder o acesso da ONG.** `refreshIfDue` usa `TransactionTemplate`, que entra na transação de `DonationService.create`. Se o PIX falhar depois da renovação, o rollback apaga o `refresh_token` novo, que o Mercado Pago já trocou. O mesmo rollback desfaz um `NEEDS_RECONNECT` | `MercadoPagoOAuthService`, `DonationService` | `PROPAGATION_REQUIRES_NEW` no `TransactionTemplate` do serviço | B1 |
| 2 | Dois clientes da Orders API com corpos diferentes. O de `payment/` não manda `expiration_time` nem `first_name` (`APRO`), então trocar direto quebraria o sandbox. O de `donation/` não tem timeout e registra o corpo das respostas no log | `payment/MercadoPagoClient`, `donation/MercadoPagoPixClient` | Ficar só com o de `payment/` e trazer o que falta | B2 |
| 3 | `isConnected` usa `findById`, que carrega e descriptografa os tokens. Sem a chave, a lista pública de campanhas pode responder 500 | `MercadoPagoOAuthService.isConnected`, `CampaignService.toDtos` | `existsByNgoIdAndStatus`, sem carregar os tokens | B1 |
| 4 | `getById` só trata `MercadoPagoException`. Com a conta da ONG caída, o acompanhamento responderia erro, embora o contrato diga que o PIX gerado continua valendo | `DonationService.getById` | Tratar também a falha de token e responder a doação como está | B1 |
| 5 | O webhook de `payment/` descarta tudo que não for `type=order`. A documentação do Mercado Pago não confirma esse valor, e só o de `donation/` lê o `data.id` do corpo | `payment/MercadoPagoWebhookController` | Tirar o filtro (order desconhecida já é ignorada) e trazer a leitura do corpo | B2 |
| 6 | Acesso revogado não vira `NEEDS_RECONNECT` ao criar ou consultar order, e o site segue mostrando o botão de doar | `DonationService`, `MercadoPagoWebhookService` | Novo `markNeedsReconnect`, **só com HTTP 401**. Não usar `isAuthFailure()`, que também aceita 400: uma order mal formada não é acesso revogado | B2 |
| 7 | `X-Idempotency-Key` é o id puro da doação, que se repete entre bancos diferentes usando a mesma conta | `MercadoPagoPixClient` | UUID na chave e `donation-{id}` no `external_reference`; atualizar o contrato §5.1 | B2 |
| 8 | Sem `JWT_SECRET` nem `MP_STATE_SECRET` no `.env.aiven`, `oauthConfigured()` é falso e conectar sempre responde 503 | `.env.aiven` | Incluir `MP_STATE_SECRET` na seção 6 e no `.env.aiven.example` | M0, B2 |
| 9 | `MP_SANDBOX` vem `true` por padrão, então um deploy sem a variável mandaria dados de teste | `application.properties` | Padrão `false`; `true` explícito no `.env.aiven` | B2 |

**Limitações aceitas:** order `failed` continua `PENDING` até vencer em 30 minutos, e estorno feito pela ONG não muda a doação.

## 4. O que a documentação do Mercado Pago não confirma

Conferido em 04/11/2026. Cada dúvida já tem um caminho previsto.

| Dúvida | O que a documentação diz | Como decidir |
|---|---|---|
| Qual token cria e consulta orders? | Se contradiz: o OAuth com `test_token=true` gera token `TEST-`; o guia do Pix usa token de teste `APP_USR`; a consulta de order diz não aceitar credenciais de teste | Testar `MP_TEST_TOKEN=true`. Se a order der 401, conectar a conta de teste vendedora com `MP_TEST_TOKEN=false` |
| Token OAuth cria Pix? | Não diz | Gate B. Se falhar de vez: plano B (seção 5) |
| A conta precisa de chave Pix? | Pede chave cadastrada para aceitar Pix, sem detalhar conta de teste | Gate B. Se pedir, cadastrar na conta de teste. Em conta real, é obrigatório |
| `localhost` serve como URL de retorno? | Não diz | Já usar o túnel, sem depender disso |
| O webhook chega para orders do token OAuth? | Não diz | Gate B. Se não chegar: bloco B3 |
| Valor de `type` e formato da notificação de orders | Evento "Order (Mercado Pago)", tópico `orders`. Sem exemplo para orders | Olhar a notificação real no painel do túnel |
| `expiration_time: PT30M` | Opcional, por pagamento; o limite mínimo parece ser 30 minutos, mas esse trecho não foi lido na íntegra | Gate A |
| Status depois do `APRO` | Começa `action_required`/`waiting_transfer`. O final esperado é `processed`/`accredited`, sem confirmação direta | Gate A |
| A aplicação passa por revisão para produção? | Não diz | Ao criar as credenciais de produção, antes do T3 |

## 5. Ordem de execução

`main` não muda até o Gate B passar. **Os PRs do back e do front entram juntos, depois do T2:** sem a tela do front, nenhuma ONG conseguiria conectar a conta nem criar campanha.

| Etapa | Quem | O que entrega | Estimativa |
|---|---|---|---|
| **M0. Credenciais** | João | Aplicação, contas de teste, túnel e `.env.aiven` preenchido (seção 6) | 1 h |
| **Gate A. Orders API** | João + Claude | Doação sandbox pelo token único, no `main` atual (seção 8) | 30 min |
| **B1. Doações na conta da ONG** | Claude | Serviço OAuth implementa o gateway; problemas 1, 3, 4 | 1 dia |
| **B2. Um cliente e um webhook** | Claude | Problemas 2, 5, 6, 7, 8, 9; testes movidos | 1 dia |
| **Gate B. OAuth real** | João + Claude | ONG conecta conta de teste, doa, Pix aprovado, webhook recebido (seção 8) | 1 h |
| **F1 a F3. Front** | Claude | Conectar a conta e bloqueios (doc do front) | 1,5 dia |
| **T2. Ponta a ponta** | João + Claude | Roteiro completo em sandbox no navegador (seção 9) | 2 h |
| **T3. Pix real de R$ 5** | **João paga** | Comprovante do crédito na conta (seção 10) | 1 h |
| **D1. Fechamento** | Claude | Docs atualizados e roteiro da banca (seção 11) | 0,5 dia |

As estimativas são aproximadas e não contam espera por revisão.

**Plano B (só se o Gate B falhar de vez):** conta única da plataforma, com repasse às ONGs registrado no painel, como no contrato §6. Exige avisar o orientador e ajustar o texto do TCC, que diz que o valor vai direto para a conta da ONG. Como `main` ainda não mudou nesse ponto, a mudança fica só na branch.

## 6. M0: credenciais (João faz)

**Claude não cria contas nem digita senhas, e nenhum segredo deve ir para o chat.** Os nomes das telas vêm da documentação e podem ter mudado; se algo não bater, é só me perguntar.

1. Em [Mercado Pago Developers](https://www.mercadopago.com.br/developers/panel/app), crie a aplicação **AUcolher**, para pagamentos online.
2. Crie um túnel para a porta 8080 e anote a URL HTTPS. Sugestão: ngrok, que tem um painel para ver as notificações recebidas e, pelo que sei, um endereço fixo no plano gratuito (confirme no site deles).
   - **O túnel expõe a API inteira na internet:** desligue quando não estiver testando.
   - **Se a URL mudar:** atualize a URL de retorno, o webhook e o `MP_REDIRECT_URI`.
3. Nos detalhes da aplicação, cadastre a URL de retorno: `https://<túnel>/api/mercado-pago/oauth/callback`.
4. Em **Integrações > Contas de teste**, crie 2 vendedoras (as ONGs) e 1 compradora, todas do Brasil. O país não muda depois, e o limite é de 15 contas.
5. Em **Webhooks**, no modo de teste, configure a URL `https://<túnel>/api/webhooks/mercadopago`, marque o evento **Order (Mercado Pago)** e copie a assinatura secreta.
6. Gere duas chaves diferentes, rodando duas vezes no Git Bash:

   ```bash
   openssl rand -base64 32
   ```

7. Acrescente no `.env.aiven`, que o Git ignora:

   ```properties
   MP_CLIENT_ID=
   MP_CLIENT_SECRET=
   MP_REDIRECT_URI=https://<túnel>/api/mercado-pago/oauth/callback
   MP_WEBHOOK_SECRET=
   MP_TOKEN_ENCRYPTION_KEY=
   MP_STATE_SECRET=
   MP_TEST_TOKEN=true
   MP_SANDBOX=true
   # Só para o Gate A: o Access Token de teste da aplicação
   MP_DEV_ACCESS_TOKEN=
   ```

8. Reinicie a API com `.\run-aiven.ps1`.

**Guarde a `MP_TOKEN_ENCRYPTION_KEY`:** se ela mudar ou se perder, as contas conectadas precisam conectar de novo.

## 7. Blocos do backend

Branch `feature/mercado-pago-ongs`, a partir do `main`. Cada bloco compila e passa nos testes sozinho.

### B1. Doações na conta da ONG

- `MercadoPagoOAuthService implements MercadoPagoAccountGateway`; apagar `PlaceholderMercadoPagoAccountGateway`.
- `isConnected` passa a usar `existsByNgoIdAndStatus(ngoId, CONNECTED)` (problema 3).
- `accessToken` usa `getValidAccessToken` e converte a falha de token na mensagem do contrato: "Esta ONG não está recebendo doações no momento.".
- `TransactionTemplate` com `PROPAGATION_REQUIRES_NEW` (problema 1). Ele abre uma segunda conexão com o banco durante a renovação, o que é aceitável nesse volume.
- `DonationService.getById` não falha por causa do token (problema 4).
- **Testes:**
  - gerenciador de transação falso, que confere a propagação;
  - `getById` com token indisponível responde a doação `PENDING`;
  - `isConnected` não carrega a entidade.

### B2. Um cliente e um webhook

- Levar para `payment/MercadoPagoClient`: `expiration_time`, sandbox/`APRO`, `MP_BASE_URL` e a recusa de order sem `qr_code`. `MercadoPagoProperties` ganha `baseUrl` e `sandbox`.
- `DonationService` passa a usar esse cliente.
- Reativar `payment/MercadoPagoWebhookController`, sem o filtro de `type` e lendo o `data.id` do corpo (problema 5). Com a assinatura válida, o webhook usa o `MercadoPagoWebhookService`, que já responde 200 quando não há token e 502 em erro temporário.
- Apagar:
  - `donation/MercadoPagoPixClient`;
  - `donation/MercadoPagoException`;
  - `donation/MercadoPagoWebhookValidator`;
  - `donation/MercadoPagoWebhookController`;
  - `DonationService.onWebhook`.
- `markNeedsReconnect` só com 401 (problema 6), UUID na idempotência (problema 7) e padrão de `MP_SANDBOX` (problema 9).
- Migrar `MercadoPagoWebhookValidatorTest` para o `MercadoPagoSignatureVerifierTest` e ajustar `DonationServiceTest` e `DonationControllerTest`.
- Atualizar o comentário do `SecurityConfig` e o `.env.aiven.example` (`MP_WEBHOOK_SECRET`, `MP_SANDBOX`, `FRONTEND_URL`).
- **Depois do Gate B:** tirar `MP_DEV_ACCESS_TOKEN` do `application.properties`, do README e do `.env.aiven.example`.

### B3. Consulta periódica (opcional)

**Só é obrigatório se o webhook não chegar no Gate B.** Sem ele e sem webhook, doação paga com a janela fechada aparece como expirada.

- `@Scheduled` a cada poucos minutos, consultando as orders de doações `PENDING`, inclusive as que acabaram de vencer.

### Reaproveitado sem mudança

`OAuthStateService`, `TokenCryptoConverter`, `MercadoPagoSignatureVerifier`, `MercadoPagoTokenRefreshJob`, `DonationGateway`, `MercadoPagoController` e a migração 009.

### Efeito que vocês vão ver

Ao sair o Placeholder, **todas as campanhas atuais ficam "Doações indisponíveis"** até a ONG conectar a conta, e ONG sem conta não cria campanha. É a regra do contrato. Para a banca, conectem 1 ou 2 ONGs com as contas de teste vendedoras.

## 8. Gates

### Gate A: Orders API, no `main` atual, sem mudar código

Passa pelo token único. As doações de teste ficam no Aiven.

1. Com o M0 feito, entre com uma conta de pessoa e doe R$ 5 em uma campanha.
2. Confira e anote:
   - o QR e o "copia e cola" aparecem;
   - a order aceitou `expiration_time: PT30M`;
   - a doação vira `APPROVED` sozinha, e quanto tempo isso leva;
   - o status final na order;
   - se o webhook chegou, com assinatura válida e qual `type`.

**Se falhar:** corrigir o campo errado antes do B1. O log da API traz o corpo da recusa.

### Gate B: OAuth real, na branch, depois do B2

Sem tela no front, a conexão é feita pela rota da API:

1. Com o token de uma ONG, chame `POST /api/ngo/mercado-pago/connect`.
2. Em uma janela anônima, abra a `authorizationUrl`, entre com a **conta de teste vendedora** e autorize.
3. Confira `?mp=connected` e o `GET /api/ngo/mercado-pago`.
4. Com outra conta, doe em uma campanha dessa ONG.
5. Confira a order criada com o token da ONG, o Pix aprovado e o webhook.

| Resultado | Decisão |
|---|---|
| Tudo funciona | Seguir o contrato. Fazer o front |
| 401 na order | Repetir com `MP_TEST_TOKEN=false` (seção 4) |
| Recusa por chave Pix | Cadastrar a chave na conta de teste e repetir |
| Só o webhook falha | Seguir o contrato e fazer o B3 |
| O token não cria Pix ou o pagamento não se confirma | Plano B (seção 5) |

### Registro dos resultados

Preencher durante os testes, sem colar tokens.

| Item | Resultado |
|---|---|
| Valor de `MP_TEST_TOKEN` que funcionou | |
| Prefixo do token (`TEST-` ou `APP_USR-`) | |
| Status final depois do `APRO` | |
| `expiration_time` aceito? | |
| `type` recebido no webhook | |
| Webhook chega para order do token OAuth? | |
| `localhost` aceito como retorno? | |
| Chave Pix exigida na conta de teste? | |
| Aplicação exige revisão para produção? | |

## 9. T2: ponta a ponta em sandbox

**Testes automáticos:** `mvn test` em um `git worktree` no scratchpad, para não atrapalhar a API na porta 8080, e E2E HTTP em banco temporário (`MP_BASE_URL` permite usar um servidor falso).

**No navegador, com o Mercado Pago de teste:**

- [ ] Conectar a conta da ONG; negar a autorização (`denied`); tentar a mesma conta em outra ONG (`already_linked`).
- [ ] ONG sem conta tentando criar campanha (400).
- [ ] Criar campanha e doar: a janela mostra o QR e confirma sozinha; o total sobe uma vez só.
- [ ] Webhook com assinatura válida aprova; com assinatura errada responde 401.
- [ ] Fechar a janela antes do pagamento: a doação ainda é confirmada.
- [ ] Desconectar com PIX pendente (400) e sem PIX pendente (204): a campanha fica "Doações indisponíveis".
- [ ] `NEEDS_RECONNECT`, forçado no banco: o painel avisa, doar responde 400 e reconectar resolve.
- [ ] Encerrar campanha com PIX pendente, excluir conta da ONG e admin tentando doar continuam funcionando.

## 10. T3: Pix real de R$ 5

**Quem paga é o João, no app do banco. Claude não executa pagamentos.** Só depois do T2.

1. Pegue as credenciais de produção da aplicação. Anote aqui se houver revisão.
2. Use uma conta real com chave Pix. Se a conta dona da aplicação não puder autorizar a si mesma, use a do Pedro.
3. Crie uma ONG e uma campanha só para isso. O doador precisa ter e-mail **diferente** do da conta que recebe.
4. No `.env.aiven`, troque `MP_CLIENT_ID`, `MP_CLIENT_SECRET`, `MP_WEBHOOK_SECRET` (e a URL de webhook de produção) e use `MP_TEST_TOKEN=false` e `MP_SANDBOX=false`. Reinicie a API.
5. Conecte a conta real, doe R$ 5 e pague pelo app do banco.
6. Guarde as evidências: tela de doação confirmada, total da campanha e o crédito no extrato do Mercado Pago, sem expor dados pessoais.
7. **Limpe:** desconecte a conta (apaga os tokens), exclua a campanha e volte o `.env.aiven` para sandbox.

## 11. D1: fechamento e banca

**Docs a atualizar:**
- no back: README (seção de variáveis e "Mercado Pago (provisório)");
- no front: `entrega-final/README.md`, `api-mercado-pago-ongs.md`, `api-campanhas-doacoes.md` e `regras-de-negocios.md` §10.3 e §12.

O texto do TCC já descreve este desenho. Só muda no plano B.

**Roteiro da banca:**
- Antes: 1 ou 2 ONGs conectadas, campanhas criadas, internet e túnel testados.
- Na hora: ONG mostra a conta conectada, pessoa doa, a janela confirma, o total sobe.
- Mostrar o comprovante do T3.
- **Ter um vídeo de reserva**, para o caso de faltar internet ou o Mercado Pago sair do ar.

## 12. Fora do escopo

Só seriam necessários para receber doações reais abertas ao público:
- aprovação manual de ONGs (hoje toda ONG nasce com selo de verificada);
- tratamento de estornos;
- revisão de termos de uso e privacidade.

Também ficam de fora PKCE (opcional no Mercado Pago) e o evento `mp-connect`.

**Se publicarem o site:** trocar `MP_REDIRECT_URI`, URL do webhook, `FRONTEND_URL` e `CORS_ALLOWED_ORIGINS`, e repetir uma conexão e uma doação. Local com túnel continua sendo o plano de reserva.

## 13. Fontes

Conferidas em 04/11/2026:
- [Teste de compra com Pix (Orders)](https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/integration-test/pix)
- [Integração Pix (Orders)](https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/payment-integration/pix)
- [Obter Access Token (OAuth)](https://www.mercadopago.com.br/developers/pt/docs/subscriptions/additional-content/security/oauth/creation.md)
- [Renovar o Access Token](https://www.mercadopago.com.br/developers/pt/docs/security/oauth/renewal)
- [Contas de teste](https://www.mercadopago.com.br/developers/pt/docs/your-integrations/test/accounts)
- [Notificações de orders](https://www.mercadopago.com.br/developers/pt/docs/checkout-api-orders/notifications)
- [Webhooks](https://www.mercadopago.com.br/developers/en/docs/your-integrations/notifications/webhooks)
- [Consultar order](https://www.mercadopago.com.br/developers/en/reference/online-payments/checkout-pro/get-order/get)
