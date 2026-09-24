# E-Library Platform

A small e-library where authenticated members browse a catalogue of books, view
details, borrow and return copies, and see what they currently hold.

The system is split into two independent services:

| Folder | Stack | Role |
|---|---|---|
| [`library-service/`](./library-service) | Java 21 · Spring Boot 4 · Spring Data JPA · PostgreSQL | REST API + business rules |
| [`library-web/`](./library-web) | React 18 · TypeScript · Vite · Tailwind CSS · TanStack Query | Browser client |

The backend is **API-only** and the frontend runs on its own Vite dev server; they
communicate over HTTP with CORS configured between them. Each service has its own
README with setup and design notes — this file is the map and the quick start.

![login-page](img/login-page.png)
![landing-page](img/landing-page.png)
![book-page](img/book-page.png)

## Architecture at a glance

```
┌────────────────┐     JSON / JWT      ┌──────────────────┐     JPA      ┌────────────┐
│  library-web   │  ───────────────▶   │  library-service │  ─────────▶  │ PostgreSQL │
│  (Vite :5173)  │  ◀───────────────   │   (Spring :8080) │  ◀─────────  │            │
└────────────────┘                     └──────────────────┘              └────────────┘
```

- **Stateless JWT auth.** The client obtains a token from `/api/auth/login` or
  `/api/auth/register` and sends it as `Authorization: Bearer <token>` on every call.
- **Layered backend**: `web` (controllers/DTOs/mappers) → `service` (business rules)
  → `repository` (Spring Data JPA) → `domain` (entities). DTOs at the edge so entities
  never leak.
- **Server state on the client** is managed by TanStack Query, with cache
  invalidation on borrow/return so the catalogue and "My Loans" stay consistent.

## Prerequisites

- **Java 21** and **Maven** (a Maven wrapper `./mvnw` is included, so a system Maven
  is optional).
- **Node 20+** and npm.
- **PostgreSQL** running locally (the app uses `ddl-auto: update`, so it creates its
  own tables — you only need to create an empty database).

## Quick start

```bash
# 1. Database — create an empty DB (defaults: localhost:5432, user/pass "postgres")
createdb elibrary

# 2. Backend — starts on :8080 and seeds demo data on first run
cd library-service
./mvnw spring-boot:run

# 3. Frontend — in a second terminal, starts on :5173
cd library-web
npm install
npm run dev
```

Open http://localhost:5173 and sign in with a seeded account (below), or register a
new one.

### Seeded demo accounts

Created automatically on first backend start (only if the tables are empty):

| Email | Password | Role |
|---|---|---|
| `member@demo.io` | `password123` | MEMBER |
| `admin@demo.io` | `password123` | ADMIN |

Ten sample books are seeded alongside them. (The `member@demo.io` credentials are
pre-filled on the login screen for convenience.)

## API summary

Base path `/api`. All endpoints except the two `auth` routes require a Bearer token.

| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/register` | Create an account, returns a JWT |
| POST | `/auth/login` | Authenticate, returns a JWT |
| GET | `/books` | Browse catalogue — `search`, `category`, `page`, `size` (paginated) |
| GET | `/books/{id}` | Book details |
| GET | `/loans/me` | The caller's loans — `?status=ACTIVE` for currently borrowed |
| POST | `/loans` | Borrow — body `{ "bookId": "<uuid>" }` |
| POST | `/loans/{id}/return` | Return a loan the caller owns |

See [`library-service/README.md`](./library-service/README.md) for request/response
shapes, error format, and the full list of enforced business rules.

## Running the tests

```bash
cd library-service && ./mvnw test     # backend: unit + repository + integration
cd library-web    && npm run test     # frontend: Vitest + React Testing Library
```