import http from './http'

export interface UserInfo {
  id: string
  username: string
  nickname: string
  avatar?: string
  roles: string[]
  permissions: string[]
}

export const authApi = {
  login: (payload: { username: string; password: string }) => http.post('/auth/login', payload) as Promise<any>,
  me: () => http.get('/auth/me') as Promise<UserInfo>,
  logout: (refreshToken?: string) => http.post('/auth/logout', { refreshToken }) as Promise<any>
}

export const menuApi = {
  menus: () => http.get('/admin/menus') as Promise<any[]>
}
