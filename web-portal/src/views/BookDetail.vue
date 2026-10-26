<template>
  <div v-loading="loading">
    <template v-if="book">
      <div class="detail-grid">
        <div>
          <el-image :src="book.coverUrl" fit="cover" class="detail-cover">
            <template #error><div class="detail-cover book-cover placeholder"><span>{{ (book.title || '书')[0] }}</span></div></template>
          </el-image>
        </div>
        <div class="detail-info">
          <div class="detail-tags">
            <el-tag effect="plain" type="warning">{{ book.categoryName }}</el-tag>
            <el-tag v-if="book.isVip" effect="plain" type="danger">VIP</el-tag>
          </div>
          <h1>{{ book.title }}</h1>
          <div class="detail-meta">
            <span>{{ book.authorName }}</span>
            <span>·</span>
            <span>{{ book.chapterCount }} 章</span>
            <span>·</span>
            <span>{{ (book.wordCount || 0).toLocaleString() }} 字</span>
            <span>·</span>
            <span>{{ book.visitCount }} 次阅读</span>
          </div>
          <div style="display:flex;align-items:center;gap:12px;margin-bottom:16px;">
            <el-rate :model-value="Number(book.score || 0)" disabled allow-half />
            <strong style="color:#9a5b3d;">{{ Number(book.score || 0).toFixed(1) }}</strong>
            <span style="color:#a3978b;font-size:12px;">{{ book.ratingCount }} 人评分</span>
          </div>
          <p class="detail-intro">{{ book.intro }}</p>
          <div class="detail-actions">
            <el-button type="primary" size="large" round @click="startRead">开始阅读</el-button>
            <el-button v-if="record" size="large" round plain @click="continueRead">继续阅读 · 第 {{ record.chapterNo }} 章</el-button>
            <el-button size="large" round plain :loading="shelfLoading" @click="addShelf">
              {{ inShelf ? '已在书架' : '加入书架' }}
            </el-button>
          </div>
          <div class="detail-stats">
            <div><strong>{{ book.chapterCount }}</strong><span>章节</span></div>
            <div><strong>{{ (book.wordCount || 0).toLocaleString() }}</strong><span>总字数</span></div>
            <div><strong>{{ book.ratingCount }}</strong><span>评分人数</span></div>
            <div><strong>{{ formatVisit(book.visitCount) }}</strong><span>阅读量</span></div>
          </div>
          <div style="display:flex;align-items:center;gap:16px;">
            <span style="color:#8e8276;font-size:13px;">给这本书打分</span>
            <el-rate v-model="myScore" @change="submitRating" />
          </div>
        </div>
      </div>

      <section class="section">
        <div class="section-head">
          <div><h2>目录</h2><p>共 {{ chapters.length }} 章 · 点击章节开始阅读</p></div>
        </div>
        <div class="catalog">
          <div v-for="c in chapters" :key="c.id" class="catalog-item" @click="readChapter(c.id)">
            <span style="flex:1;">{{ c.title }}</span>
            <span v-if="c.isVip">🔒 VIP</span>
            <span v-else>{{ c.wordCount }} 字</span>
          </div>
        </div>
      </section>

      <section class="section">
        <div class="section-head">
          <div><h2>读者评论</h2><p>共 {{ commentTotal }} 条</p></div>
        </div>
        <div class="comment-box">
          <div style="display:flex;gap:12px;margin-bottom:18px;">
            <el-input v-model="commentText" type="textarea" :rows="3" maxlength="1000" show-word-limit placeholder="写下你的阅读感受…" />
            <el-button type="primary" style="align-self:flex-end;" @click="submitComment">发表</el-button>
          </div>
          <div v-for="c in comments" :key="c.id" class="comment-item">
            <div class="comment-head">
              <el-avatar :size="28" :src="c.avatar">{{ (c.nickname || '书')[0] }}</el-avatar>
              <strong>{{ c.nickname }}</strong>
              <span>{{ c.createTime }}</span>
            </div>
            <p class="comment-content">{{ c.content }}</p>
            <div class="comment-actions">
              <button @click="like(c)">♡ {{ c.likeCount }}</button>
              <button v-if="canDelete(c)" @click="removeComment(c)">删除</button>
            </div>
          </div>
          <div v-if="comments.length === 0" class="empty-state" style="padding:30px 0;">还没有评论，来写第一条吧</div>
        </div>
      </section>
    </template>
    <div v-else-if="!loading" class="empty-state">
      <p>书籍不存在或已下架</p>
      <el-button type="primary" plain @click="$router.push('/books')">回到书库</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { portalApi } from '../api/portal'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const book = ref<any>(null)
const chapters = ref<any[]>([])
const comments = ref<any[]>([])
const commentTotal = ref(0)
const commentText = ref('')
const myScore = ref(0)
const record = ref<any>(null)
const inShelf = ref(false)
const loading = ref(true)
const shelfLoading = ref(false)

const bookId = computed(() => String(route.params.id))
const canDelete = (c: any) => store.user?.id === c.userId || store.user?.roles?.includes('ADMIN')

async function load() {
  loading.value = true
  try {
    const [detail, chapterList, rating, commentPage] = await Promise.all([
      portalApi.bookDetail(bookId.value),
      portalApi.chapters(bookId.value),
      portalApi.rating(bookId.value),
      portalApi.comments(bookId.value, { current: 1, size: 20 })
    ])
    book.value = detail
    chapters.value = chapterList || []
    myScore.value = rating.myScore || 0
    comments.value = commentPage.records || []
    commentTotal.value = commentPage.total || 0
    if (store.isLoggedIn) {
      record.value = await portalApi.readRecord(bookId.value).catch(() => null)
      const shelf: any[] = await portalApi.shelf().catch(() => [])
      inShelf.value = shelf.some((item) => item.book.id === bookId.value)
    }
  } catch (e) {
    book.value = null
  } finally {
    loading.value = false
  }
}
function startRead() {
  const first = chapters.value[0]
  if (!first) return
  router.push(`/read/${first.id}`)
}
function continueRead() {
  if (record.value?.chapterId) router.push(`/read/${record.value.chapterId}`)
  else startRead()
}
function readChapter(id: string) {
  router.push(`/read/${id}`)
}
function formatVisit(value: number) {
  const n = Number(value || 0)
  return n >= 10000 ? (n / 10000).toFixed(1) + 'w' : String(n)
}
function requireLogin() {
  if (!store.isLoggedIn) {
    router.push({ path: '/login', query: { redirect: route.fullPath } })
    return false
  }
  return true
}
async function addShelf() {
  if (!requireLogin()) return
  if (inShelf.value) {
    await portalApi.removeShelf(bookId.value)
    inShelf.value = false
    ElMessage.success('已移出书架')
    return
  }
  shelfLoading.value = true
  try {
    await portalApi.addShelf(bookId.value)
    inShelf.value = true
    ElMessage.success('已加入书架')
  } finally {
    shelfLoading.value = false
  }
}
async function submitRating() {
  if (!requireLogin()) return
  try {
    const summary = await portalApi.rate(bookId.value, myScore.value)
    book.value.score = summary.average
    book.value.ratingCount = summary.count
    ElMessage.success('评分已更新')
  } catch (e) {
    // interceptor handles
  }
}
async function submitComment() {
  if (!requireLogin()) return
  if (!commentText.value.trim()) {
    ElMessage.warning('先写点什么吧')
    return
  }
  await portalApi.addComment({ bookId: bookId.value, content: commentText.value })
  commentText.value = ''
  const page: any = await portalApi.comments(bookId.value, { current: 1, size: 20 })
  comments.value = page.records || []
  commentTotal.value = page.total || 0
  ElMessage.success('评论已发表')
}
async function like(c: any) {
  await portalApi.likeComment(c.id)
  c.likeCount = (c.likeCount || 0) + 1
}
async function removeComment(c: any) {
  await portalApi.deleteComment(c.id)
  comments.value = comments.value.filter((item) => item.id !== c.id)
  commentTotal.value = Math.max(0, commentTotal.value - 1)
  ElMessage.success('已删除')
}

watch(() => route.params.id, load)
onMounted(load)
</script>
