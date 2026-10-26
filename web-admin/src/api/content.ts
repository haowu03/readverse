import http from './http'

export const bookApi = {
  page: (params: any) => http.get('/admin/books', { params }) as Promise<any>,
  detail: (id: string) => http.get(`/admin/books/${id}`) as Promise<any>,
  create: (data: any) => http.post('/admin/books', data) as Promise<any>,
  update: (id: string, data: any) => http.put(`/admin/books/${id}`, data) as Promise<any>,
  remove: (id: string) => http.delete(`/admin/books/${id}`) as Promise<any>,
  publish: (id: string) => http.put(`/admin/books/${id}/publish`) as Promise<any>,
  unpublish: (id: string) => http.put(`/admin/books/${id}/unpublish`) as Promise<any>
}

export const chapterApi = {
  list: (bookId?: string) => http.get('/admin/chapters', { params: { bookId } }) as Promise<any[]>,
  detail: (id: string) => http.get(`/admin/chapters/${id}`) as Promise<any>,
  create: (data: any) => http.post('/admin/chapters', data) as Promise<any>,
  update: (id: string, data: any) => http.put(`/admin/chapters/${id}`, data) as Promise<any>,
  remove: (id: string) => http.delete(`/admin/chapters/${id}`) as Promise<any>,
  publish: (id: string) => http.put(`/admin/chapters/${id}/publish`) as Promise<any>
}

export const authorApi = {
  page: (params: any) => http.get('/admin/authors', { params }) as Promise<any>,
  all: () => http.get('/admin/authors/all') as Promise<any[]>,
  create: (data: any) => http.post('/admin/authors', data) as Promise<any>,
  update: (id: string, data: any) => http.put(`/admin/authors/${id}`, data) as Promise<any>,
  remove: (id: string) => http.delete(`/admin/authors/${id}`) as Promise<any>,
  audit: (id: string, data: any) => http.put(`/admin/authors/${id}/audit`, data) as Promise<any>
}

export const categoryApi = {
  tree: () => http.get('/admin/categories') as Promise<any[]>,
  all: () => http.get('/admin/categories/all') as Promise<any[]>,
  create: (data: any) => http.post('/admin/categories', data) as Promise<any>,
  update: (id: string, data: any) => http.put(`/admin/categories/${id}`, data) as Promise<any>,
  remove: (id: string) => http.delete(`/admin/categories/${id}`) as Promise<any>
}

export const uploadApi = {
  cover: (file: File) => {
    const form = new FormData()
    form.append('file', file)
    return http.post('/admin/upload/cover', form, {
      headers: { 'Content-Type': 'multipart/form-data' }
    }) as Promise<string>
  }
}
