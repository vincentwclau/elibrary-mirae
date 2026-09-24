# library-service

The E-Library backend: a Spring Boot 4 REST API over PostgreSQL that owns the
catalogue, the borrowing rules, and authentication. See the
[top-level README](../README.md) for the overall picture and quick start; this
document covers the backend in depth.

## Stack

- Java 21, Spring Boot 4.1, Maven (wrapper included)
- Spring Web (MVC), Spring Security, Spring Data JPA / Hibernate
- PostgreSQL (runtime), H2 (tests only)
- jjwt for JWT signing/parsing, BCrypt for password hashing

## Layout

```
com.mirae.elibrary
├── ElibraryApplication         # entry point; Clock bean for testable time
├── config/                     # AppProperties, SecurityConfig, DataSeeder
├── security/                   # JwtService, JwtAuthenticationFilter, UserDetails
├── domain/                     # User, Book, Loan + Role, LoanStatus enums
├── repository/                 # Spring Data JPA repositories
├── service/                    # AuthService, BookService, LoanService — business rules
├── web/
│   ├── controller/             # Auth, Book, Loan controllers (thin)
│   ├── dto/                    # request/response records
│   └── mapper/                 # entity → DTO mapping
└── exception/                  # custom exceptions + GlobalExceptionHandler
```

The dependency direction is strictly `web → service → repository → domain`.
Controllers are thin: they validate input and delegate. All business logic and
invariants live in the service layer, which is where the interesting decisions are.

## Prerequisites

- Java 21, and PostgreSQL running locally.
- Create an empty database (Hibernate creates the tables via `ddl-auto: update`):
  ```bash
  createdb elibrary
  ```

## Configuration

Everything is driven by `src/main/resources/application.yml` with environment-variable
overrides and sensible localhost defaults:

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/elibrary` | JDBC URL |
| `DB_USERNAME` | `postgres` | DB user |
| `DB_PASSWORD` | `postgres` | DB password |
| `APP_JWT_SECRET` | (Base64 dev default) | HMAC signing key — **set this in any real deployment** |
| `SERVER_PORT` | `8080` | HTTP port |

Application-level knobs live under the `app.*` prefix (bound to `AppProperties`):
CORS allowed origins (default `http://localhost:5173`), JWT expiry (24h), and the
loan policy — `app.loan.max-active-loans: 5`, `app.loan.period-days: 14`.

## Run

```bash
./mvnw spring-boot:run
```

On first start the `DataSeeder` (a `CommandLineRunner`, skipped under the `test`
profile) inserts two demo users and ten books **only if the tables are empty**:

| Email | Password | Role |
|---|---|---|
| `member@demo.io` | `password123` | MEMBER |
| `admin@demo.io` | `password123` | ADMIN |

## API

Base path `/api`. `/api/auth/**` is public; everything else needs
`Authorization: Bearer <token>`.

| Method | Path | Body | Success |
|---|---|---|---|
| POST | `/auth/register` | `{ email, password (≥8), name }` | 201 + `AuthResponse` |
| POST | `/auth/login` | `{ email, password }` | 200 + `AuthResponse` |
| GET | `/books?search=&category=&page=&size=` | — | 200 + `PageResponse<BookResponse>` |
| GET | `/books/{id}` | — | 200 + `BookResponse` |
| GET | `/loans/me?status=ACTIVE` | — | 200 + `LoanResponse[]` |
| POST | `/loans` | `{ bookId }` | 201 + `LoanResponse` |
| POST | `/loans/{id}/return` | — | 200 + `LoanResponse` |

`AuthResponse` carries `{ token, tokenType, expiresInMs, userId, email, name, role }`.
Browsing is paginated (default size 20, sorted by title); `search` matches title or
author, `category` is an exact match.

### Example

```bash
TOKEN=$(curl -s localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"member@demo.io","password":"password123"}' | jq -r .token)

curl -s localhost:8080/api/books -H "Authorization: Bearer $TOKEN"
```

## Domain model & business rules

Three entities:

- **User** — `id (UUID)`, unique `email`, `passwordHash`, `name`, `role`, `createdAt`.
- **Book** — `id (UUID)`, `title`, `author`, unique `isbn`, `description`, `category`,
  `publishedYear`, `totalCopies`, `availableCopies`, `@Version version`, `createdAt`.
- **Loan** — `id (UUID)`, `user`, `book`, `borrowedAt`, `dueAt`, `returnedAt`
  (nullable), `status` (`ACTIVE`/`RETURNED`).

Rules enforced in `LoanService` (each documented assumption where the brief was open):

- Borrowing requires `availableCopies > 0`; otherwise **409**.
- A member cannot hold two **active** loans of the same book; otherwise **409**.
- A member may hold at most **5** active loans (configurable); otherwise **409**.
- Borrowing decrements `availableCopies`, creates an `ACTIVE` loan, and sets
  `dueAt = now + 14 days`.
- Only the borrower can return a loan; a loan the caller doesn't own reads as **404**
  (we don't reveal others' loan IDs).
- Returning increments `availableCopies` and sets `status = RETURNED`; returning an
  already-returned loan is a **409**.
- "Currently borrowed" = loans with `status = ACTIVE`.

**Concurrency**: `Book.version` (JPA optimistic locking) guards the availability
counter, so two simultaneous borrows of the last copy can't both succeed.

**Time** is injected via a `Clock` bean rather than `Instant.now()` calls, which keeps
due-date logic deterministic in tests.

## Error handling

A single `GlobalExceptionHandler` (`@RestControllerAdvice`) produces one consistent
body for every failure:

```json
{
  "timestamp": "2026-09-24T10:00:00Z",
  "status": 409,
  "error": "Conflict",
  "message": "No copies of this book are currently available.",
  "path": "/api/loans",
  "fieldErrors": [ { "field": "password", "message": "size must be between 8 and ..." } ]
}
```

`fieldErrors` appears only for validation failures. Mappings:

| Exception / condition | Status |
|---|---|
| `ResourceNotFoundException` | 404 |
| `BusinessRuleException` (availability, limits, duplicates) | 409 |
| Bean Validation failure (`@Valid`) | 400 |
| Missing/invalid token | 401 |
| Authenticated but not allowed | 403 |
| Anything else | 500 |

Security-layer failures (401/403) are written in the **same** shape via a custom
`AuthenticationEntryPoint` / `AccessDeniedHandler`, so clients only ever parse one
error format.

## Security

Stateless `SecurityFilterChain`: CSRF disabled (token auth), CORS enabled, session
policy `STATELESS`. `JwtAuthenticationFilter` reads the Bearer token, validates it,
and populates the `SecurityContext`. Passwords are BCrypt-hashed. The JWT subject is
the user ID, with email and role as claims.

## Tests

```bash
./mvnw test
```

Comprehensive coverage across three levels (37 tests):

- **Unit** (Mockito) — `LoanServiceTest`, `BookServiceTest`, `AuthServiceTest` exercise
  every business rule: no copies, duplicate active loan, over limit, not found,
  return-by-non-owner, already-returned. Uses a fixed `Clock`.
- **Repository** (`@DataJpaTest` on H2 in PostgreSQL-compat mode) —
  `BookRepositoryTest`, `LoanRepositoryTest` verify the custom search query and the
  loan finders (including `@EntityGraph` fetching to avoid lazy-loading surprises with
  `open-in-view: false`).
- **Integration** (`@SpringBootTest` + MockMvc) — `ElibraryIntegrationTest` runs the
  full flow register → browse → borrow → return end to end, plus the 400/401/404/409
  error cases, against an H2 database under the `test` profile.

## Notable trade-offs

- **`ddl-auto: update`** keeps the take-home simple; production would use Flyway.
- **No MapStruct** — plain hand-written mappers are enough at this size and avoid an
  annotation-processor dependency.
- **`open-in-view: false`** (the sensible default): lazy associations are fetched
  explicitly with `@EntityGraph` rather than relying on an open session in the view
  layer.
- **Jackson 3** — Spring Boot 4 ships Jackson 3 (`tools.jackson.*`), which is used for
  the error-body serialization in the security filters.
```

