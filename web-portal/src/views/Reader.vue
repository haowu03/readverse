<template>
  <div class="reader-page" v-loading="loading">
    <div class="reader-toolbar">
      <div style="display:flex;align-items:center;gap:14px;">
        <el-button text :icon="Back" @click="backToBook">返回详情</el-button>
        <span class="book-name">{{ chapter?.bookTitle || '' }}</span>
        <span v-if="chapter">· 第 {{ chapter.chapterNo }} 章</span>
      </div>
      <div style="display:flex;align-items:center;gap:8px;">
        <el-button text size="small" @click="fontSize = Math.max(14, fontSize - 1)">A-</el-button>
        <span style="font-size:11px;">{{ fontSize }}px</span>
        <el-button text size="small" @click="fontSize = Math.min(24, fontSize + 1)">A+</el-button>
        <el-switch v-model="night" active-text="夜间" style="margin-left:8px;" />
        <el-button text :icon="List" @click="drawer = true">目录</el-button>
      </div>
    </div>

    <template v-if="chapter">
      <article class="reader-content" :class="{ night }" :style="{ fontSize: fontSize + 'px' }">
        <h1>{{ chapter.title }}</h1>
        <div class="chapter-sub">{{ chapter.bookTitle }} · 第 {{ chapter.chapterNo }} 章 · {{ chapter.wordCount }} 字</div>
        <p v-for="(p, idx) in paragraphs" :key="idx">{{ p }}</p>
      </article>
      <div class="reader-bottom">
        <el-button size="large" :disabled="!chapter.prevChapterId" @click="go(chapter.prevChapterId)">← 上一章</el-button>
        <el-button size="large" :disabled="!chapter.nextChapterId" type="primary" @click="go(chapter.nextChapterId)">下一章 →</el-button>
      </div>
    </template>
    <div v-else-if="!loading" class="empty-state">
      <p>{{ error || '章节不存在或暂时无法阅读' }}</p>
      <el-button type="primary" plain @click="backToBook">返回书籍详情</el-button>
    </div>

    <el-drawer v-model="drawer" title="目录" size="360px">
      <div v-for="c in catalog" :key="c.id" class="catalog-item" :style="{ borderColor: c.id === chapter?.id ? '#c98a63' : 'transparent' }" @click="go(c.id)">
        <span style="flex:1;">{{ c.title }}</span>
        <span v-if="c.isVip">🔒</span>
        <span v-else>{{ c.wordCount }} 字</span>
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Back, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { portalApi } from '../api/portal'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const chapter = ref<any>(null)
const catalog = ref<any[]>([])
const loading = ref(false)
const error = ref('')
const drawer = ref(false)
const fontSize = ref(Number(localStorage.getItem('rv_reader_font') || 17))
const night = ref(localStorage.getItem('rv_reader_night') === '1')

const paragraphs = computed(() => {
  const text = chapter.value?.content || ''
  return text.split(/\n+/).map((p: string) => p.trim()).filter(Boolean)
})

async function load(id: string) {
  if (!id) return
  loading.value = true
  error.value = ''
  try {
    const data: any = await portalApi.chapter(id)
    chapter.value = data
    if (route.params.chapterId !== id) {
      await router.replace(`/read/${id}`)
    }
    if (!catalog.value.length || catalog.value[0]?.id !== data.id) {
      await loadCatalog(data.bookId)
    }
    await saveProgress()
    window.scrollTo({ top: 0, behavior: 'smooth' })
  } catch (e: any) {
    chapter.value = null
    error.value = e?.message || '章节加载失败'
  } finally {
    loading.value = false
  }
}
async function loadCatalog(bookId: string) {
  try {
    catalog.value = await portalApi.chapters(bookId)
  } catch (e) {
    catalog.value = []
  }
}
async function saveProgress() {
  if (!store.isLoggedIn || !chapter.value) return
  try {
    await portalApi.saveRecord({
      bookId: chapter.value.bookId,
      chapterId: chapter.value.id,
      chapterNo: chapter.value.chapterNo,
      progress: 0.01,
      readSeconds: 30
    })
  } catch (e) {
    // reading should never be blocked by progress sync
  }
}
function go(id?: string) {
  if (!id) return
  drawer.value = false
  router.push(`/read/${id}`)
}
function backToBook() {
  if (chapter.value?.bookId) router.push(`/book/${chapter.value.bookId}`)
  else router.push('/books')
}

watch(fontSize, (v) => localStorage.setItem('rv_reader_font', String(v)))
watch(night, (v) => localStorage.setItem('rv_reader_night', v ? '1' : '0'))
watch(() => route.params.chapterId, (id) => { if (id) load(String(id)) })

onMounted(() => {
  load(String(route.params.chapterId))
})
</script>
