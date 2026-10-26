<template>
  <div class="portal-login">
    <div class="panel-card">
      <h1>{{ mode === 'login' ? '欢迎回来' : '加入阅界' }}</h1>
      <p class="sub">{{ mode === 'login' ? '登录后同步书架、阅读进度与积分' : '注册一个读者账号，开始收藏与签到' }}</p>

      <el-form :model="form" label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" size="large" placeholder="4-30 个字符" :prefix-icon="User" />
        </el-form-item>
        <el-form-item v-if="mode === 'register'" label="昵称">
          <el-input v-model="form.nickname" size="large" placeholder="给自己取个名字" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" size="large" show-password placeholder="至少 6 位" :prefix-icon="Lock" @keyup.enter="submit" />
        </el-form-item>
        <el-button type="primary" size="large" style="width:100%;height:46px;" :loading="loading" @click="submit">
          {{ mode === 'login' ? '登录' : '注册并登录' }}
        </el-button>
      </el-form>

      <div style="margin-top:18px;text-align:center;font-size:12px;color:#9a8e82;">
        <span v-if="mode === 'login'">还没有账号？<a style="color:#9a5b3d;cursor:pointer;" @click="mode = 'register'">立即注册</a></span>
        <span v-else>已经有账号？<a style="color:#9a5b3d;cursor:pointer;" @click="mode = 'login'">去登录</a></span>
      </div>

      <div class="login-demo">
        <span>演示读者</span>
        <button @click="fillDemo">reader / Reader@123</button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const route = useRoute()
const router = useRouter()
const mode = ref<'login' | 'register'>('login')
const loading = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

function fillDemo() {
  form.username = 'reader'
  form.password = 'Reader@123'
}
async function submit() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  if (mode.value === 'register' && !form.nickname) {
    ElMessage.warning('请输入昵称')
    return
  }
  loading.value = true
  try {
    if (mode.value === 'login') await store.login({ username: form.username, password: form.password })
    else await store.register({ username: form.username, password: form.password, nickname: form.nickname })
    ElMessage.success(mode.value === 'login' ? '登录成功' : '注册成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch (e) {
    // interceptor handles
  } finally {
    loading.value = false
  }
}
onMounted(() => {
  if (store.isLoggedIn) router.replace((route.query.redirect as string) || '/')
})
</script>
