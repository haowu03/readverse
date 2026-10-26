<template>
  <div class="login-page">
    <div class="login-visual">
      <div class="brand">ReadVerse <span>阅界</span></div>
      <h1>让每一本书<br />都有它的回声</h1>
      <p>阅读社区 · 内容运营 · 数据采集 · 积分体系</p>
      <div class="visual-orbit">
        <div class="orbit-card card-a">30 本示例书籍</div>
        <div class="orbit-card card-b">150 个章节</div>
        <div class="orbit-card card-c">RBAC 权限</div>
        <div class="orbit-card card-d">Redis 缓存</div>
      </div>
    </div>
    <div class="login-panel">
      <div class="login-box">
        <h2>管理后台</h2>
        <p class="sub">使用运营或管理员账号登录</p>
        <el-form :model="form" label-position="top" @submit.prevent="submit">
          <el-form-item label="用户名">
            <el-input v-model="form.username" size="large" placeholder="请输入用户名" :prefix-icon="User" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="form.password" type="password" size="large" show-password placeholder="请输入密码" :prefix-icon="Lock" @keyup.enter="submit" />
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="submit">进入后台</el-button>
        </el-form>
        <div class="demo-accounts">
          <span>演示账号</span>
          <button @click="fill('admin', 'Admin@123')">管理员</button>
          <button @click="fill('operator', 'Operator@123')">运营</button>
          <button @click="fill('reader', 'Reader@123')">读者</button>
        </div>
      </div>
      <div class="login-foot">ReadVerse · 原创阅读社区项目</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const form = reactive({ username: 'admin', password: 'Admin@123' })
const loading = ref(false)
const router = useRouter()
const route = useRoute()
const store = useUserStore()

function fill(username: string, password: string) {
  form.username = username
  form.password = password
}

async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await store.login(form.username, form.password)
    ElMessage.success(`欢迎回来，${store.user?.nickname || form.username}`)
    await router.push((route.query.redirect as string) || '/dashboard')
  } catch (e) {
    // interceptor already shows the message
  } finally {
    loading.value = false
  }
}
</script>
