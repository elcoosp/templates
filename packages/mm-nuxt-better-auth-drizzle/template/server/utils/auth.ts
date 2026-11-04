import { betterAuth } from 'better-auth'
import { drizzleAdapter } from 'better-auth/adapters/drizzle'
import { db } from '../database/client'
import * as schema from '../database/schema'

export const auth = betterAuth({
  database: drizzleAdapter(db, {
    {% if database == "postgres" -%}
    provider: 'pg',
    {%- else -%}
    provider: 'sqlite',
    {%- endif %}
    schema,
  }),
  emailAndPassword: {
    enabled: true,
  },
})
