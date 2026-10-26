import { defineStore } from 'pinia'
import { authApi, menuApi, type UserInfo } from '../api/auth'

export const useUserStore = defineStore('adminUser', {
  state: () => ({
    token: localStorage.getItem('rv_admin_token') || '',
    refreshToken: localStorage.getItem('rv_admin_refresh') || '',
    user: null as UserInfo | null,
    menus: [] as any[],
    loading: false
  }),
  getters: {
    permissions(state): Set<string> {
      return new Set(state.user?.permissions || [])
    },
    isAdmin(state): boolean {
      return !!state.user?.roles?.includes('ADMIN')
    }
  },
  actions: {
    async login(username: string, password: string) {
      const data: any = await authApi.login({ username, password })
      this.token = data.accessToken
      this.refreshToken = data.refreshToken
      localStorage.setItem('rv_admin_token', data.accessToken)
      localStorage.setItem('rv_admin_refresh', data.refreshToken)
      await this.loadProfile()
    },
    async loadProfile() {
      if (!this.token) return
      this.loading = true
      try {
        const [user, menus] = await Promise.all([authApi.me(), menuApi.menus()])
        this.user = user
        this.menus = menus
      } finally {
        this.loading = false
      }
    },
    hasPerm(code?: string) {
      if (!code) return true
      if (this.isAdmin) return true
      return this.permissions.has(code)
    },
    async logout() {
      try {
        await authApi.logout(this.refreshToken)
      } catch (e) {
        // ignore network errors on logout
      }
      this.token = ''
      this.refreshToken = ''
      this.user = null
      this.menus = []
      localStorage.removeItem('rv_admin_token')
      localStorage.removeItem('rv_admin_refresh')
    }
  }
})
