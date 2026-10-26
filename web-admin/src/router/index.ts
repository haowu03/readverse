import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { title: '登录' }
    },
    {
      path: '/',
      component: () => import('../layout/AdminLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', component: () => import('../views/Dashboard.vue'), meta: { title: '数据看板', permission: 'stats:view' } },
        { path: 'content/books', component: () => import('../views/content/Books.vue'), meta: { title: '书籍管理', permission: 'content:book:list' } },
        { path: 'content/chapters', component: () => import('../views/content/Chapters.vue'), meta: { title: '章节管理', permission: 'content:chapter:list' } },
        { path: 'content/authors', component: () => import('../views/content/Authors.vue'), meta: { title: '作者管理', permission: 'content:author:list' } },
        { path: 'content/categories', component: () => import('../views/content/Categories.vue'), meta: { title: '分类管理', permission: 'content:category:list' } },
        { path: 'operation/sources', component: () => import('../views/operation/Sources.vue'), meta: { title: '书源管理', permission: 'crawl:source:list' } },
        { path: 'operation/tasks', component: () => import('../views/operation/Tasks.vue'), meta: { title: '采集任务', permission: 'crawl:task:list' } },
        { path: 'points/goods', component: () => import('../views/points/Goods.vue'), meta: { title: '积分商品', permission: 'points:goods:list' } },
        { path: 'points/orders', component: () => import('../views/points/Orders.vue'), meta: { title: '兑换订单', permission: 'points:order:list' } },
        { path: 'system/users', component: () => import('../views/system/Users.vue'), meta: { title: '用户管理', permission: 'system:user:list' } },
        { path: 'system/roles', component: () => import('../views/system/Roles.vue'), meta: { title: '角色权限', permission: 'system:role:list' } }
      ]
    },
    { path: '/403', component: () => import('../views/Forbidden.vue'), meta: { title: '无权访问' } },
    { path: '/:pathMatch(.*)*', component: () => import('../views/NotFound.vue'), meta: { title: '页面不存在' } }
  ]
})

router.beforeEach(async (to) => {
  const store = useUserStore()
  if (to.path === '/login') {
    return store.token ? '/dashboard' : true
  }
  if (!store.token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (!store.user) {
    try {
      await store.loadProfile()
    } catch (e) {
      await store.logout()
      return '/login'
    }
  }
  if (to.meta.permission && !store.hasPerm(to.meta.permission as string)) {
    return '/403'
  }
  return true
})

router.afterEach((to) => {
  document.title = `${to.meta.title || 'ReadVerse'} · ReadVerse 管理后台`
})

export default router
