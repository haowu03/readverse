import { defineStore } from 'pinia'
import { authApi } from '../api/portal'

export const useUserStore = defineStore('portalUser', {
  state: () => ({
    token: localStorage.getItem('rv_portal_token') || '',
    refreshToken: localStorage.getItem('rv_portal_refresh') || '',
    user: null as any
  }),
  getters: {
    isLoggedIn(state): boolean {
      return !!state.token
    },
    displayName(state): string {
      return state.user?.nickname || state.user?.username || '书友'
    }
  },
  actions: {
    async login(payload: any) {
      const data: any = await authApi.login(payload)
      this.token = data.accessToken
      this.refreshToken = data.refreshToken
      localStorage.setItem('rv_portal_token', data.accessToken)
      localStorage.setItem('rv_portal_refresh', data.refreshToken)
      this.user = data.user
    },
    async register(payload: any) {
      await authApi.register(payload)
      await this.login({ username: payload.username, password: payload.password })
    },
    async loadMe() {
      if (!this.token) return
      try {
        this.user = await authApi.me()
      } catch (e) {
        this.logoutLocal()
      }
    },
    logoutLocal() {
      this.token = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem('rv_portal_token')
      localStorage.removeItem('rv_portal_refresh')
    },
    async logout() {
      try {
        await authApi.logout(this.refreshToken)
      } catch (e) {
        // ignore
      }
      this.logoutLocal()
    }
  }
})
