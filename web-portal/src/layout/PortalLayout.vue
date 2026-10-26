<template>
  <div class="portal-shell">
    <header class="portal-header">
      <div class="header-inner">
        <router-link to="/" class="portal-brand">
          <span class="mark">RV</span>
          <span class="name">ReadVerse <em>阅界</em></span>
        </router-link>
        <nav class="portal-nav">
          <router-link to="/" :class="{ active: route.path === '/' }">首页</router-link>
          <router-link to="/books" :class="{ active: route.path.startsWith('/books') || route.path.startsWith('/book/') }">书库</router-link>
          <router-link to="/rankings" :class="{ active: route.path === '/rankings' }">排行榜</router-link>
          <router-link to="/bookshelf" :class="{ active: route.path === '/bookshelf' }">书架</router-link>
        </nav>
        <div class="header-actions">
          <el-input
            v-model="keyword"
            class="header-search"
            placeholder="搜索书名 / 作者"
            :prefix-icon="Search"
            clearable
            @keyup.enter="search"
          />
          <template v-if="store.isLoggedIn">
            <el-dropdown trigger="click" @command="onCommand">
              <div class="portal-user">
                <el-avatar :size="30" :src="store.user?.avatar">{{ store.displayName[0] }}</el-avatar>
                <span>{{ store.displayName }}</span>
                <el-icon><ArrowDown /></el-icon>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人中心</el-dropdown-item>
                  <el-dropdown-item command="shelf">我的书架</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <el-button v-else type="primary" round @click="goLogin">登录 / 注册</el-button>
        </div>
      </div>
    </header>

    <main class="portal-main">
      <router-view />
    </main>

    <footer class="portal-footer">
      <div class="footer-inner">
        <div>
          <strong>ReadVerse · 阅界</strong>
          <p>一个用于演示缓存、消息、调度与积分一致性的原创阅读社区项目。</p>
        </div>
        <div class="footer-links">
          <router-link to="/books">书库</router-link>
          <router-link to="/rankings">排行榜</router-link>
          <router-link to="/login">登录</router-link>
        </div>
        <div class="footer-note">示例内容，仅用于技术演示</div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search, ArrowDown } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const keyword = ref('')

function search() {
  router.push({ path: '/books', query: keyword.value ? { keyword: keyword.value } : {} })
}
function goLogin() {
  router.push({ path: '/login', query: { redirect: route.fullPath } })
}
async function onCommand(command: string) {
  if (command === 'profile') router.push('/profile')
  else if (command === 'shelf') router.push('/bookshelf')
  else if (command === 'logout') {
    await store.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  }
}
</script>
