<template>
  <div>
    <div class="page-title">
      <div><h1>我的书架</h1><p>继续上次的章节，或者整理一下收藏</p></div>
      <el-button text @click="$router.push('/books')">去书库逛逛 →</el-button>
    </div>

    <div v-if="loading" class="loading-block">正在整理书架…</div>
    <div v-else-if="items.length === 0" class="empty-state">
      <div class="icon">◌</div>
      <p>书架还是空的，去挑一本喜欢的书吧</p>
      <el-button type="primary" round @click="$router.push('/books')">去书库</el-button>
    </div>
    <div v-else class="book-grid">
      <div v-for="item in items" :key="item.book.id" class="book-card">
        <router-link :to="`/book/${item.book.id}`">
          <div class="book-cover-wrap">
            <el-image :src="item.book.coverUrl" fit="cover" class="book-cover">
              <template #error><div class="book-cover placeholder"><span>{{ (item.book.title || '书')[0] }}</span></div></template>
            </el-image>
            <span class="book-score">{{ Number(item.book.score || 0).toFixed(1) }}</span>
          </div>
        </router-link>
        <div class="book-body">
          <h3>{{ item.book.title }}</h3>
          <p class="book-author">{{ item.book.authorName }} · {{ item.book.categoryName }}</p>
          <p class="book-intro">读至第 {{ item.lastChapterNo || 1 }} 章</p>
          <div style="display:flex;gap:8px;margin-top:6px;">
            <el-button type="primary" size="small" round @click="continueRead(item)">继续阅读</el-button>
            <el-button size="small" round plain @click="remove(item)">移出</el-button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { portalApi } from '../api/portal'

const router = useRouter()
const items = ref<any[]>([])
const loading = ref(true)

async function load() {
  loading.value = true
  try { items.value = await portalApi.shelf() } finally { loading.value = false }
}
function continueRead(item: any) {
  if (item.lastChapterId) router.push(`/read/${item.lastChapterId}`)
  else router.push(`/book/${item.book.id}`)
}
async function remove(item: any) {
  await portalApi.removeShelf(item.book.id)
  items.value = items.value.filter((x) => x.book.id !== item.book.id)
  ElMessage.success('已移出书架')
}
onMounted(load)
</script>
