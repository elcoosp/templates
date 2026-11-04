import { mkdtemp, rm } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { join } from 'node:path'
import { eq } from 'drizzle-orm'
{% if database == "postgres" -%}
import { getTableConfig } from 'drizzle-orm/pg-core'
{%- else -%}
import { getTableConfig } from 'drizzle-orm/sqlite-core'
{%- endif %}
import { describe, expect, test, vi } from 'vitest'

const { db } = await import('../server/database/client')
const { account, session, user, verification } = await import(
  '../server/database/schema'
)

describe('better-auth drizzle schema', () => {
  test('user table exposes the better-auth columns', () => {
    const columns = Object.keys(user)
    for (const column of [
      'id',
      'name',
      'email',
      'emailVerified',
      'image',
      'createdAt',
      'updatedAt',
    ]) {
      expect(columns).toContain(column)
    }
  })

  test('inserts a user with generated timestamps', () => {
    const query = db
      .insert(user)
      .values({ id: 'usr_1', name: 'Ada Lovelace', email: 'ada@example.com' })
      .toSQL()
    expect(query.sql).toContain('insert into "user"')
    expect(query.sql).toContain('"email_verified"')
  })

  test('inserts a session tied to a user', () => {
    const query = db
      .insert(session)
      .values({
        id: 'ses_1',
        expiresAt: new Date('2026-01-01T00:00:00.000Z'),
        token: 'token_1',
        userId: 'usr_1',
      })
      .toSQL()
    expect(query.sql).toContain('insert into "session"')
    expect(query.sql).toContain('"user_id"')
  })

  test('inserts an oauth account tied to a user', () => {
    const query = db
      .insert(account)
      .values({
        id: 'acc_1',
        accountId: 'gh_1',
        providerId: 'github',
        userId: 'usr_1',
        accessToken: 'at_1',
        refreshToken: 'rt_1',
        idToken: 'id_1',
        scope: 'read:user',
      })
      .toSQL()
    expect(query.sql).toContain('insert into "account"')
    expect(query.sql).toContain('"access_token_expires_at"')
  })

  test('inserts a verification value', () => {
    const query = db
      .insert(verification)
      .values({ id: 'ver_1', identifier: 'ada@example.com', value: 'v_1' })
      .toSQL()
    expect(query.sql).toContain('insert into "verification"')
    expect(query.sql).toContain('"expires_at"')
  })

  test('session and account foreign keys reference the user table', () => {
    for (const table of [session, account]) {
      const { foreignKeys } = getTableConfig(table)
      expect(foreignKeys).toHaveLength(1)
      const reference = foreignKeys[0]?.reference()
      expect(reference?.columns.map((column) => column.name)).toEqual([
        'user_id',
      ])
      expect(reference?.foreignColumns[0]).toBe(user.id)
    }
  })

  test('selects users with their sessions', () => {
    const query = db
      .select({ userName: user.name, sessionToken: session.token })
      .from(session)
      .innerJoin(user, eq(session.userId, user.id))
      .where(eq(user.email, 'ada@example.com'))
      .toSQL()
    expect(query.sql).toContain('inner join "user"')
    expect(query.sql).toContain('where')
  })

  test('selects accounts of a user by id', () => {
    const query = db
      .select({ provider: account.providerId })
      .from(account)
      .where(eq(account.userId, 'usr_1'))
      .toSQL()
    expect(query.sql).toContain('from "account"')
    expect(query.sql).toContain('"user_id" =')
  })

  test('deletes a user cascading its sessions', () => {
    const query = db.delete(user).where(eq(user.id, 'usr_1')).toSQL()
    expect(query.sql).toContain('delete from "user"')
  })

  test('falls back to the default database when DATABASE_URL is unset', async () => {
    const sandbox = await mkdtemp(join(tmpdir(), 'auth-db-'))
    const originalCwd = process.cwd()
    const originalUrl = process.env.DATABASE_URL
    process.chdir(sandbox)
    delete process.env.DATABASE_URL
    try {
      vi.resetModules()
      const { db: localDb } = await import('../server/database/client')
      expect(localDb).toBeDefined()
    } finally {
      process.chdir(originalCwd)
      if (originalUrl !== undefined) {
        process.env.DATABASE_URL = originalUrl
      }
      vi.resetModules()
      await rm(sandbox, { recursive: true, force: true })
    }
  })
})
