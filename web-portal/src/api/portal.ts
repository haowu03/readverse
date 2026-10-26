import http from './http'

export const portalApi = {
  home: () => http.get('/portal/home') as Promise<any>,
  categories: () => http.get('/portal/categories/tree') as Promise<any[]>,
  books: (params: any) => http.get('/portal/books', { params }) as Promise<any>,
  search: (params: any) => http.get('/portal/search', { params }) as Promise<any>,
  bookDetail: (id: string) => http.get(`/portal/books/${id}`) as Promise<any>,
  chapters: (bookId: string) => http.get(`/portal/books/${bookId}/chapters`) as Promise<any[]>,
  chapter: (id: string) => http.get(`/portal/chapters/${id}`) as Promise<any>,
  rankings: (type: string, limit = 20) => http.get('/portal/rankings', { params: { type, limit } }) as Promise<any[]>,
  rating: (bookId: string) => http.get(`/portal/ratings/${bookId}`) as Promise<any>,
  rate: (bookId: string, score: number) => http.post(`/portal/ratings/${bookId}`, { score }) as Promise<any>,
  comments: (bookId: string, params: any) => http.get('/portal/comments', { params: { bookId, ...params } }) as Promise<any>,
  addComment: (data: any) => http.post('/portal/comments', data) as Promise<any>,
  deleteComment: (id: string) => http.delete(`/portal/comments/${id}`) as Promise<any>,
  likeComment: (id: string) => http.post(`/portal/comments/${id}/like`) as Promise<any>,
  shelf: () => http.get('/portal/bookshelf') as Promise<any[]>,
  addShelf: (bookId: string) => http.post('/portal/bookshelf', { bookId }) as Promise<any>,
  removeShelf: (bookId: string) => http.delete(`/portal/bookshelf/${bookId}`) as Promise<any>,
  readRecord: (bookId: string) => http.get(`/portal/read-record/${bookId}`) as Promise<any>,
  saveRecord: (data: any) => http.put('/portal/read-record', data) as Promise<any>,
  signIn: () => http.post('/portal/sign-in') as Promise<any>,
  signMonth: () => http.get('/portal/sign-in/month') as Promise<any>,
  pointsAccount: () => http.get('/portal/points/account') as Promise<any>,
  pointsLogs: (params: any) => http.get('/portal/points/logs', { params }) as Promise<any>,
  goods: () => http.get('/portal/points/goods') as Promise<any[]>,
  exchange: (data: any) => http.post('/portal/points/exchange', data) as Promise<any>,
  orders: (params: any) => http.get('/portal/points/orders', { params }) as Promise<any>
}

export const authApi = {
  login: (data: any) => http.post('/auth/login', data) as Promise<any>,
  register: (data: any) => http.post('/auth/register', data) as Promise<any>,
  me: () => http.get('/auth/me') as Promise<any>,
  logout: (refreshToken?: string) => http.post('/auth/logout', { refreshToken }) as Promise<any>
}
