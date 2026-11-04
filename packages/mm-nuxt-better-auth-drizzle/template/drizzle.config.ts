import { defineConfig } from 'drizzle-kit'

{% if database == "postgres" -%}
// biome-ignore format: keep the generated connection URL on a single line
const defaultDatabaseUrl = 'postgresql://postgres:postgres@localhost:5432/{{project_name | replace(from="-", to="_")}}'
{%- else -%}
const defaultDatabaseUrl = './local.db'
{%- endif %}

export default defineConfig({
  {% if database == "postgres" -%}
  dialect: 'postgresql',
  {%- else -%}
  dialect: 'sqlite',
  {%- endif %}
  schema: './server/database/schema.ts',
  out: './drizzle',
  dbCredentials: {
    url: process.env.DATABASE_URL ?? defaultDatabaseUrl,
  },
})
