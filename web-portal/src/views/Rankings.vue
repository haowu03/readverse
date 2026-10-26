<template>
  <div>
    <div class="page-title">
      <div><h1>阅读排行榜</h1><p>Redis Zset 实时累计点击，支持 7 日 / 30 日 / 总榜</p></div>
      <el-radio-group v-model="type" @change="load">
        <el-radio-button label="7d">7 日榜</el-radio-button>
        <el-radio-button label="30d">30 日榜</el-radio-button>
        <el-radio-button label="all">总榜</el-radio-button>
      </el-radio-group>
    </div>

    <div v-if="loading" class="loading-block">榜单计算中…</div>
    <div v-else-if="items.length === 0" class="empty-state">
      <p>这个时间段还没有阅读数据，先去读几章吧</p>
      <el-button type="primary" plain @click="$router.push('/books')">去书库</el-button>
    </div>
    <div v-else class="rank-list">
      <router-link v-for="item in items" :key="item.book.id" :to="`/book/${item.book.id}`" class="rank-row">
        <div class="rank-no" :class="`top${item.rank}`">{{ item.rank }}</div>
        <el-image :src="item.book.coverUrl" fit="cover" class="rank-cover">
          <template #error><div class="rank-cover" style="display:grid;place-items:center;background:#e6d6c6;">{{ (item.book.title || '书')[0] }}</div></template>
        </el-image>
        <div class="rank-info">
          <strong>{{ item.book.title }}</strong>
          <p>{{ item.book.authorName }} · {{ item.book.categoryName }} · {{ item.book.chapterCount }} 章</p>
        </div>
        <div style="text-align:right;">
          <div class="rank-score">{{ Number(item.score || 0).toFixed(0) }}</div>
          <div style="color:#b0a397;font-size:10px;">点击量</div>
        </div>
      </router-link>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { portalApi } from '../api/portal'

const type = ref('all')
const items = ref<any[]>([])
const loading = ref(false)

async function load() {
  loading.value = true
  try { items.value = await portalApi.rankings(type.value, 20) } finally { loading.value = false }
}
onMounted(load)
</script>
