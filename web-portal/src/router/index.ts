import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: () => import('../layout/PortalLayout.vue'),
      children: [
        { path: '', name: 'home', component: () => import('../views/Home.vue'), meta: { title: '首页' } },
        { path: 'books', name: 'books', component: () => import('../views/BookList.vue'), meta: { title: '书库' } },
        { path: 'book/:id', name: 'book', component: () => import('../views/BookDetail.vue'), meta: { title: '书籍详情' } },
        { path: 'read/:chapterId', name: 'reader', component: () => import('../views/Reader.vue'), meta: { title: '阅读' } },
        { path: 'bookshelf', name: 'bookshelf', component: () => import('../views/Bookshelf.vue'), meta: { title: '我的书架', requiresAuth: true } },
        { path: 'rankings', name: 'rankings', component: () => import('../views/Rankings.vue'), meta: { title: '排行榜' } },
        { path: 'profile', name: 'profile', component: () => import('../views/Profile.vue'), meta: { title: '个人中心', requiresAuth: true } },
        { path: 'login', name: 'login', component: () => import('../views/Login.vue'), meta: { title: '登录' } }
      ]
    },
    { path: '/:pathMatch(.*)*', component: () => import('../views/NotFound.vue'), meta: { title: '页面不存在' } }
  ]
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  if (store.token && !store.user) {
    try {
      await store.loadMe()
    } catch (e) {
      store.logoutLocal()
    }
  }
  if (to.meta.requiresAuth && !store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || '阅读'} · ReadVerse 阅界`
})

export default router
