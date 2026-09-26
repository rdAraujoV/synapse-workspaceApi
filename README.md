# Synapse Workspace API
API de autenticação stateless construída com Spring Boot, Spring Security e JWT. Cobre registro, login, autorização de rotas protegidas e proteção contra força bruta, com testes de integração usando containers reais de PostgreSQL.

## Stack

- **Java 17** + **Spring Boot 4.1.1**
- **Spring Security** — autenticação stateless via filtro JWT customizado
- **Spring Data JPA** + **PostgreSQL**
- **JJWT** (`io.jsonwebtoken`) — geração e validação de tokens
- **BCrypt** — hashing de senha
- **Bucket4j** + **Caffeine** — rate limiting (por IP e por e-mail) com expiração automática de estado
- **Bean Validation** (Jakarta) — validação de entrada nos DTOs
- **Testcontainers** + **JUnit 5** + **MockMvc** — testes de integração com banco real

## Funcionalidades

- Registro de usuário com senha hasheada (BCrypt) e verificação de e-mail duplicado
- Login com emissão de JWT (HMAC-SHA, expiração configurável)
- Autorização de rotas protegidas via filtro JWT (`Authorization: Bearer <token>`)
- Rate limiting em `/auth/login`, por IP e por e-mail, com resposta `429` e header `Retry-After`
- Tratamento de erros centralizado (`@RestControllerAdvice`) com respostas JSON padronizadas
- Validação de entrada (formato de e-mail, tamanho mínimo de senha) nos endpoints de registro e login

## Endpoints

|Método|Rota|Autenticação|Descrição|
|---|---|:-:|---|
|POST|`/auth/register`|Não|Cria um novo usuário|
|POST|`/auth/login`|Não|Autentica e retorna um JWT|
|GET|`/users/me`|Sim (Bearer)|Retorna o e-mail do usuário autenticado|

### Exemplo — registro

```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "senhaSegura123"
}
```

`201 Created` em caso de sucesso, `409 Conflict` se o e-mail já existir, `400 Bad Request` se e-mail/senha forem inválidos.

### Exemplo — login

```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "senhaSegura123"
}
```

Resposta:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

`401 Unauthorized` para credenciais inválidas, `429 Too Many Requests` se o limite de tentativas for excedido.

### Exemplo — rota protegida

```http
GET /users/me
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

`200 OK` com o e-mail do usuário, `403 Forbidden` se o token estiver ausente, inválido ou expirado.

## Rodando localmente

**Pré-requisitos:** Java 17, Maven, uma instância PostgreSQL (ou Docker), e Docker rodando (necessário para os testes de integração via Testcontainers).

1. Defina as variáveis de ambiente (ou use um `.env` na raiz, via `springboot4-dotenv`):

    ```
    POSTGRES_PASSWORD=sua_senhaJWT_SECRET=uma_chave_base64_de_pelo_menos_256_bits
    ```

2. Suba a aplicação:

    ```bash
    ./mvnw spring-boot:run
    ```


## Rodando os testes

```bash
./mvnw test
```

Os testes de integração sobem containers PostgreSQL reais via Testcontainers — é necessário ter Docker em execução.

## Limitações conhecidas / próximos passos

- Sem refresh token — o JWT expira em 24h sem forma de renovação
- Sem revogação de token (logout não invalida um token já emitido)
- Sem verificação de e-mail no cadastro
- CORS não configurado (a definir conforme o frontend consumidor)