<template>
  <div>
    <div class="page-title">
      <div>
        <h1>{{ route.query.keyword ? '搜索结果' : '全部书库' }}</h1>
        <p v-if="route.query.keyword">关键词：{{ route.query.keyword }} · 共 {{ total }} 本</p>
        <p v-else>在 {{ total }} 本示例作品中，找到下一本想读的书</p>
      </div>
      <el-radio-group v-model="sort" @change="reload">
        <el-radio-button label="update">最近更新</el-radio-button>
        <el-radio-button label="new">最新上架</el-radio-button>
        <el-radio-button label="hot">最多阅读</el-radio-button>
        <el-radio-button label="score">评分最高</el-radio-button>
      </el-radio-group>
    </div>

    <div class="chip-row" style="margin-bottom:24px;">
      <button class="chip" :class="{ active: !activeCategory }" @click="selectCategory('')">全部分类</button>
      <button v-for="c in categories" :key="c.id" class="chip" :class="{ active: activeCategory === String(c.id) }" @click="selectCategory(String(c.id))">
        {{ c.name }}
      </button>
    </div>

    <div v-if="loading" class="loading-block">正在翻页…</div>
    <div v-else-if="books.length === 0" class="empty-state">
      <div class="icon">◌</div>
      <p>没有找到匹配的书籍，换个关键词试试</p>
      <el-button type="primary" plain @click="$router.push('/books')">回到全部书库</el-button>
    </div>
    <template v-else>
      <div class="book-grid">
        <BookCard v-for="book in books" :key="book.id" :book="book" />
      </div>
      <div class="pager-row">
        <el-pagination
          v-model:current-page="current"
          :page-size="size"
          background
          layout="prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BookCard from '../components/BookCard.vue'
import { portalApi } from '../api/portal'

const route = useRoute()
const router = useRouter()
const categories = ref<any[]>([])
const books = ref<any[]>([])
const total = ref(0)
const current = ref(1)
const size = 12
const sort = ref('update')
const activeCategory = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params: any = { current: current.value, size, sort: sort.value }
    if (route.query.keyword) params.keyword = route.query.keyword
    if (activeCategory.value) params.categoryId = activeCategory.value
    const page: any = await portalApi.books(params)
    books.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}
function reload() {
  current.value = 1
  load()
}
function selectCategory(id: string) {
  activeCategory.value = id
  const query: any = { ...route.query }
  if (id) query.categoryId = id
  else delete query.categoryId
  router.replace({ path: '/books', query })
  reload()
}
async function loadCategories() {
  categories.value = await portalApi.categories()
  if (route.query.categoryId) activeCategory.value = String(route.query.categoryId)
}

watch(() => route.query.keyword, () => { current.value = 1; load() })
watch(() => route.query.categoryId, (v) => { activeCategory.value = v ? String(v) : ''; current.value = 1; load() })

onMounted(async () => {
  await loadCategories()
  load()
})
</script>
