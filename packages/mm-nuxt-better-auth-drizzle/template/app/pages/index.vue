<script setup lang="ts">
import { authClient } from '~/utils/auth-client'

const config = useRuntimeConfig()

const name = ref('')
const email = ref('')
const password = ref('')
const status = ref<'idle' | 'loading' | 'success' | 'error'>('idle')
const errorMessage = ref('')

async function signUp() {
  status.value = 'loading'
  errorMessage.value = ''
  const { error } = await authClient.signUp.email({
    name: name.value,
    email: email.value,
    password: password.value,
  })
  if (error) {
    status.value = 'error'
    errorMessage.value = error.message ?? 'Sign up failed'
    return
  }
  status.value = 'success'
}
</script>

<template>
  <main class="page">
    <section class="card">
      <p class="eyebrow">@{{ config.public.org }}</p>
      <h1>{{ config.public.projectName }}</h1>
      <p class="lede">{{ config.public.description }}</p>

      <form class="form" @submit.prevent="signUp">
        <label>
          Name
          <input v-model="name" type="text" required placeholder="Ada Lovelace">
        </label>
        <label>
          Email
          <input v-model="email" type="email" required placeholder="ada@example.com">
        </label>
        <label>
          Password
          <input
            v-model="password"
            type="password"
            required
            minlength="8"
            placeholder="At least 8 characters"
          >
        </label>
        <button type="submit" :disabled="status === 'loading'">
          {{ status === 'loading' ? 'Signing up...' : 'Create account' }}
        </button>
      </form>

      <p v-if="status === 'success'" class="success" role="status">
        Account created. You can now sign in with your credentials.
      </p>
      <p v-if="status === 'error'" class="error" role="alert">{{ errorMessage }}</p>
    </section>
  </main>
</template>
