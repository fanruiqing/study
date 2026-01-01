import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Session } from '@/types'
import { sessionApi } from '@/api'

export const useSessionStore = defineStore('session', () => {
  const sessions = ref<Session[]>([])
  const currentSessionId = ref<string | null>(null)
  const loading = ref(false)
  const searchQuery = ref('')

  const currentSession = computed(() => 
    sessions.value.find(s => s.id === currentSessionId.value)
  )

  const filteredSessions = computed(() => {
    if (!searchQuery.value) return sessions.value
    const query = searchQuery.value.toLowerCase()
    return sessions.value.filter(s => 
      s.title.toLowerCase().includes(query)
    )
  })

  async function fetchSessions() {
    loading.value = true
    try {
      sessions.value = await sessionApi.getAll()
    } finally {
      loading.value = false
    }
  }

  async function createSession() {
    const session = await sessionApi.create()
    sessions.value.unshift(session)
    currentSessionId.value = session.id
    return session
  }

  async function updateSession(id: string, data: Partial<Session>) {
    const updated = await sessionApi.update(id, data)
    const index = sessions.value.findIndex(s => s.id === id)
    if (index !== -1) {
      sessions.value[index] = updated
    }
    return updated
  }

  async function deleteSession(id: string) {
    await sessionApi.delete(id)
    sessions.value = sessions.value.filter(s => s.id !== id)
    if (currentSessionId.value === id) {
      currentSessionId.value = sessions.value[0]?.id || null
    }
  }

  async function searchSessions(query: string) {
    if (!query) {
      await fetchSessions()
      return
    }
    sessions.value = await sessionApi.search(query)
  }

  function setCurrentSession(id: string | null) {
    currentSessionId.value = id
  }

  return {
    sessions,
    currentSessionId,
    currentSession,
    filteredSessions,
    loading,
    searchQuery,
    fetchSessions,
    createSession,
    updateSession,
    deleteSession,
    searchSessions,
    setCurrentSession
  }
})
