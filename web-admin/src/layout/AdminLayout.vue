<template>
  <el-container class="admin-shell">
    <el-aside :width="collapsed ? '72px' : '232px'" class="admin-aside">
      <div class="brand" :class="{ collapsed }">
        <div class="brand-mark">RV</div>
        <div v-if="!collapsed" class="brand-text">
          <strong>ReadVerse</strong>
          <span>阅界管理后台</span>
        </div>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="route.path"
          :collapse="collapsed"
          :collapse-transition="false"
          router
          class="admin-menu"
        >
          <template v-for="menu in menus" :key="menu.id">
            <el-sub-menu v-if="menu.children && menu.children.length" :index="menu.path">
              <template #title>
                <el-icon><component :is="menu.icon || 'Menu'" /></el-icon>
                <span>{{ menu.name }}</span>
              </template>
              <el-menu-item v-for="child in menu.children" :key="child.id" :index="child.path">
                <el-icon><component :is="child.icon || 'Document'" /></el-icon>
                <span>{{ child.name }}</span>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item v-else :index="menu.path">
              <el-icon><component :is="menu.icon || 'Menu'" /></el-icon>
              <template #title>{{ menu.name }}</template>
            </el-menu-item>
          </template>
        </el-menu>
      </el-scrollbar>
      <div class="aside-bottom" v-if="!collapsed">
        <span class="dot"></span>
        <span>服务运行中</span>
      </div>
    </el-aside>

    <el-container class="admin-main">
      <el-header class="admin-header">
        <div class="header-left">
          <el-button text circle @click="collapsed = !collapsed">
            <el-icon :size="18"><Fold v-if="!collapsed" /><Expand v-else /></el-icon>
          </el-button>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>ReadVerse</el-breadcrumb-item>
            <el-breadcrumb-item>{{ route.meta.title || '管理后台' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-badge :value="unread" :hidden="unread === 0" :max="99">
            <el-button text circle @click="showNotify = true">
              <el-icon :size="18"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-dropdown trigger="click" @command="onCommand">
            <div class="user-chip">
              <el-avatar :size="32" :src="store.user?.avatar">{{ (store.user?.nickname || 'R')[0] }}</el-avatar>
              <div class="user-meta">
                <strong>{{ store.user?.nickname || '未登录' }}</strong>
                <span>{{ roleText }}</span>
              </div>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">账号信息</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="admin-content">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>

    <el-drawer v-model="showNotify" title="站内通知" size="380px">
      <div v-if="notifications.length === 0" class="empty-text">暂时没有新通知</div>
      <div v-for="item in notifications" :key="item.id" class="notify-item" :class="{ unread: !item.isRead }">
        <div class="notify-title">{{ item.title }}</div>
        <div class="notify-content">{{ item.content }}</div>
        <div class="notify-time">{{ item.createTime }}</div>
      </div>
    </el-drawer>

    <el-dialog v-model="profileVisible" title="账号信息" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ store.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ store.user?.nickname }}</el-descriptions-item>
        <el-descriptions-item label="角色">{{ roleText }}</el-descriptions-item>
        <el-descriptions-item label="权限点">{{ store.user?.permissions?.length || 0 }} 个</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { notifyApi } from '../api/system'
import { Bell, ArrowDown, Fold, Expand } from '@element-plus/icons-vue'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const showNotify = ref(false)
const profileVisible = ref(false)
const unread = ref(0)
const notifications = ref<any[]>([])

const menus = computed(() => store.menus)
const roleText = computed(() => {
  const roles = store.user?.roles || []
  if (roles.includes('ADMIN')) return '管理员'
  if (roles.includes('OPERATOR')) return '运营'
  return roles.join(' / ') || '读者'
})

async function loadNotify() {
  try {
    unread.value = await notifyApi.unreadCount()
    const page: any = await notifyApi.page({ current: 1, size: 20 })
    notifications.value = page.records || []
  } catch (e) {
    // notification is non-critical
  }
}

async function onCommand(command: string) {
  if (command === 'logout') {
    await store.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } else if (command === 'profile') {
    profileVisible.value = true
  }
}

onMounted(async () => {
  if (!store.user) {
    try {
      await store.loadProfile()
    } catch (e) {
      // router guard handles it
    }
  }
  loadNotify()
})
</script>
