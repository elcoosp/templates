{% if database == "postgres" -%}
import { drizzle } from 'drizzle-orm/node-postgres'
import { Pool } from 'pg'
import * as schema from './schema'

const pool = new Pool({
  connectionString:
    process.env.DATABASE_URL ??
    'postgresql://postgres:postgres@localhost:5432/{{project_name | replace(from="-", to="_")}}',
})

export const db = drizzle(pool, { schema })
{%- else -%}
import Database from 'better-sqlite3'
import { drizzle } from 'drizzle-orm/better-sqlite3'
import * as schema from './schema'

const sqlite = new Database(process.env.DATABASE_URL ?? './local.db')

export const db = drizzle(sqlite, { schema })
{%- endif %}
