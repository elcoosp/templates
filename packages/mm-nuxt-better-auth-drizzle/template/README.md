# {{project_name}}

{{description}}

A [Nuxt](https://nuxt.com) 4 application with
[Better Auth](https://www.better-auth.com) for authentication and
[Drizzle ORM](https://orm.drizzle.team) for type-safe database access
(dialect: `{{database}}`).

## Requirements

- Node.js `>= 24`
- [pnpm](https://pnpm.io) `>= 12`

## Getting started

1. Copy the environment example and adjust the values:

   ```sh
   cp .env.example .env
   ```

2. Install dependencies:

   ```sh
   pnpm install
   ```

3. Generate and apply the database migrations:

   ```sh
   pnpm db:generate
   pnpm db:migrate
   ```

4. Start the development server:

   ```sh
   pnpm dev
   ```

The app is served at http://localhost:3000 and exposes the Better Auth
endpoints under `/api/auth/**`.

## Scripts

| Script             | Description                                   |
| ------------------ | --------------------------------------------- |
| `pnpm dev`         | Start the development server                  |
| `pnpm build`       | Build the production bundle                   |
| `pnpm generate`    | Generate a static build                       |
| `pnpm preview`     | Preview the production build locally          |
| `pnpm typecheck`   | Run `vue-tsc` against the project             |
| `pnpm check`       | Run Biome (formatter, linter, import sorting) |
| `pnpm fix`         | Apply Biome fixes                             |
| `pnpm test`        | Run the unit tests with Vitest                |
| `pnpm db:generate` | Generate Drizzle migrations from the schema   |
| `pnpm db:migrate`  | Apply Drizzle migrations                      |
| `pnpm db:studio`   | Open Drizzle Studio to browse the database    |

## Project layout

```text
app/
  assets/css/     Global styles
  pages/          Application routes (file based routing)
  utils/          Client utilities (auth client, helpers)
server/
  api/auth/       Better Auth catch-all route handler
  database/       Drizzle schema and client
  utils/          Server utilities (auth instance)
drizzle/          Generated migrations
tests/            Unit tests
```

## Authentication

Better Auth is configured in `server/utils/auth.ts` with the Drizzle
adapter pointing at the generated schema. Email and password sign up is
enabled; additional providers and plugins can be added to the
`betterAuth` configuration.

On the client, `app/utils/auth-client.ts` exports the Vue auth client
used by the demo sign up form in `app/pages/index.vue`.

## Database

The Drizzle schema mirrors the Better Auth tables (`user`, `session`,
`account`, `verification`) in `server/database/schema.ts`. Update the
schema, then regenerate migrations with `pnpm db:generate` and apply
them with `pnpm db:migrate`.
