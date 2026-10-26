import http from './http'

export const dashboardApi = {
  summary: () => http.get('/admin/dashboard/summary') as Promise<any>,
  bookTrend: (days = 7) => http.get('/admin/dashboard/book-trend', { params: { days } }) as Promise<any[]>,
  readActivity: (days = 7) => http.get('/admin/dashboard/read-activity', { params: { days } }) as Promise<any[]>
}

export const userApi = {
  page: (params: any) => http.get('/admin/users', { params }) as Promise<any>,
  updateStatus: (id: string, status: number) => http.put(`/admin/users/${id}/status`, { status }) as Promise<any>,
  assignRoles: (id: string, roleIds: string[]) => http.put(`/admin/users/${id}/roles`, { roleIds }) as Promise<any>
}

export const roleApi = {
  list: () => http.get('/admin/roles') as Promise<any[]>,
  create: (data: any) => http.post('/admin/roles', data) as Promise<any>,
  update: (id: string, data: any) => http.put(`/admin/roles/${id}`, data) as Promise<any>,
  remove: (id: string) => http.delete(`/admin/roles/${id}`) as Promise<any>
}

export const notifyApi = {
  unreadCount: () => http.get('/admin/notifications/unread-count') as Promise<number>,
  page: (params: any) => http.get('/admin/notifications', { params }) as Promise<any>
}
