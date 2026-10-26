import http from './http'

export const crawlApi = {
  sources: () => http.get('/admin/crawl/sources') as Promise<any[]>,
  createSource: (data: any) => http.post('/admin/crawl/sources', data) as Promise<any>,
  updateSource: (id: string, data: any) => http.put(`/admin/crawl/sources/${id}`, data) as Promise<any>,
  removeSource: (id: string) => http.delete(`/admin/crawl/sources/${id}`) as Promise<any>,
  testSource: (id: string) => http.post(`/admin/crawl/sources/${id}/test`) as Promise<number>,
  tasks: (params: any) => http.get('/admin/crawl/tasks', { params }) as Promise<any>,
  trigger: (sourceId: string, type = 'FULL') => http.post('/admin/crawl/tasks/trigger', null, { params: { sourceId, type } }) as Promise<any>,
  retry: (id: string) => http.post(`/admin/crawl/tasks/${id}/retry`) as Promise<any>,
  jobLogs: (params: any) => http.get('/admin/crawl/job-logs', { params }) as Promise<any>
}
