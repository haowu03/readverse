<template>
  <router-link :to="`/book/${book.id}`" class="book-card">
    <div class="book-cover-wrap">
      <el-image :src="book.coverUrl" fit="cover" class="book-cover" lazy>
        <template #error>
          <div class="book-cover placeholder">
            <span>{{ (book.title || '书')[0] }}</span>
          </div>
        </template>
      </el-image>
      <span v-if="book.isVip" class="vip-badge">VIP</span>
      <span class="book-score">{{ Number(book.score || 0).toFixed(1) }}</span>
    </div>
    <div class="book-body">
      <h3>{{ book.title }}</h3>
      <p class="book-author">{{ book.authorName }} · {{ book.categoryName }}</p>
      <p class="book-intro">{{ book.intro }}</p>
      <div class="book-meta">
        <span>{{ book.chapterCount }} 章</span>
        <span>{{ formatVisit(book.visitCount) }} 阅读</span>
      </div>
    </div>
  </router-link>
</template>

<script setup lang="ts">
defineProps<{ book: any }>()

function formatVisit(value: number) {
  const n = Number(value || 0)
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  return String(n)
}
</script>
