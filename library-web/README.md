# library-web

The E-Library frontend: a React single-page app for browsing the catalogue,
borrowing and returning books, and reviewing current loans. It talks to
[`library-service`](../library-service) over HTTP. See the
[top-level README](../README.md) for the overall picture.

## Stack

- React 18 + TypeScript, built with Vite 6
- Tailwind CSS v4 (via the `@tailwindcss/vite` plugin)
- TanStack Query v5 for server state, axios for HTTP, React Router v6 for routing
- Vitest + React Testing Library for tests

## Layout

```
src/
├── api/          # axios instance (Bearer interceptor) + auth/books/loans calls
├── auth/         # AuthContext, useAuth, ProtectedRoute
├── components/   # Navbar, SearchBar, BookCard, Pagination, StatusMessage
├── pages/        # Login, Register, Browse, BookDetail, MyLoans
├── types/        # TS types mirroring the backend DTOs
├── test/         # setup + renderWithProviders helper
├── App.tsx       # routes
└── main.tsx      # providers (QueryClient, Router, Auth)
```

## Prerequisites

- Node 20+ and npm.
- The backend running (default `http://localhost:8080`). See `library-service`.

## Configuration

The API base URL comes from `VITE_API_BASE_URL` (see `.env` / `.env.example`),
defaulting to `http://localhost:8080/api`.

## Run

```bash
npm install
npm run dev        # Vite dev server on http://localhost:5173
```

The login screen is pre-filled with the seeded `member@demo.io` / `password123`
account for convenience; you can also register a new account.

Other scripts:

```bash
npm run build      # type-check (tsc -b) + production build
npm run lint       # type-check only (tsc -b --noEmit)
npm run test       # Vitest run
```

## How it works

- **Auth.** `AuthContext` holds the current user and JWT. The token is persisted in
  `localStorage` and attached to every request by an axios request interceptor. A
  response interceptor clears the token and redirects to `/login` on any `401`, so an
  expired session recovers gracefully. `ProtectedRoute` guards the authenticated pages.
- **Server state.** All catalogue/loan data flows through TanStack Query. Borrow and
  return are mutations that invalidate the relevant queries (`book`, `books`, `loans`),
  so availability counts and "My Loans" update immediately without manual refetching.
- **Routing.** `/login` and `/register` are public; `/`, `/books/:id`, and `/my-loans`
  are protected.
- **Errors.** The backend's consistent error body is surfaced to the user via a shared
  `toErrorMessage` helper, so a 409 like "no copies available" is shown inline rather
  than failing silently.

## Tests

```bash
npm run test
```

Vitest + React Testing Library, using a `renderWithProviders` helper that wraps
components in the Router / Query / Auth providers (with query retries disabled so
failures surface immediately). The suite covers the two key flows:

- `BrowsePage` — renders the catalogue from the API and shows the empty state.
- `BookDetailPage` — the borrow action calls the API and confirms, and the button is
  disabled when a book is unavailable.

The API modules are mocked so tests exercise component behaviour without a live
backend.

## Trade-offs

- **Token in `localStorage`.** Simple and works across reloads, at the cost of XSS
  exposure. A hardened deployment would prefer an httpOnly cookie with CSRF
  protection; for this scope `localStorage` keeps the two services cleanly decoupled.
- **Tailwind v4** is used with its Vite plugin and CSS-first config (`@import
  'tailwindcss'`) — no `tailwind.config.js` needed.
- **Focused test coverage.** Tests demonstrate the approach on the highest-value flows
  rather than aiming for exhaustive coverage, in line with the take-home scope.
```

