import http from './http'

export const goodsApi = {
  list: () => http.get('/admin/points/goods') as Promise<any[]>,
  create: (data: any) => http.post('/admin/points/goods', data) as Promise<any>,
  update: (id: string, data: any) => http.put(`/admin/points/goods/${id}`, data) as Promise<any>,
  remove: (id: string) => http.delete(`/admin/points/goods/${id}`) as Promise<any>,
  orders: (params: any) => http.get('/admin/points/orders', { params }) as Promise<any>,
  reconcile: () => http.post('/admin/points/reconcile') as Promise<string[]>
}
