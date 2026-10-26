<template>
  <div>
    <section class="hero">
      <div>
        <div class="eyebrow">READVERSE · 阅界阅读社区</div>
        <h1>在字里行间，<br />遇见<em>另一个世界</em></h1>
        <p>从一个章节开始，收藏一本书，留下一条评分。ReadVerse 用缓存、异步消息与数据管道，让阅读体验更轻、更快。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round @click="$router.push('/books')">开始阅读</el-button>
          <el-button size="large" round plain @click="$router.push('/rankings')">看看榜单</el-button>
        </div>
        <div class="hero-stats">
          <div><strong>30</strong>示例书籍</div>
          <div><strong>150</strong>章节内容</div>
          <div><strong>{{ data.categories.length || 6 }}</strong>主题分类</div>
        </div>
      </div>
      <div class="hero-visual">
        <img v-if="heroBooks[2]" :src="heroBooks[2].coverUrl" class="hero-book b3" alt="" />
        <img v-if="heroBooks[1]" :src="heroBooks[1].coverUrl" class="hero-book b2" alt="" />
        <img v-if="heroBooks[0]" :src="heroBooks[0].coverUrl" class="hero-book b1" alt="" />
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h2>按兴趣逛逛</h2>
          <p>六个分类，覆盖科幻、都市、历史与文学</p>
        </div>
        <router-link to="/books">全部书库 →</router-link>
      </div>
      <div class="chip-row">
        <button class="chip" @click="$router.push('/books')">全部</button>
        <button v-for="c in data.categories" :key="c.id" class="chip" @click="$router.push({ path: '/books', query: { categoryId: c.id } })">
          {{ c.name }}
        </button>
      </div>
    </section>

    <section class="section">
      <div class="section-head">
        <div>
          <h2>新书上架</h2>
          <p>刚刚入库的示例作品，适合从第一章开始</p>
        </div>
        <router-link to="/books?sort=new">更多新书 →</router-link>
      </div>
      <div class="book-grid">
        <BookCard v-for="book in data.newBooks" :key="book.id" :book="book" />
      </div>
    </section>

    <section class="section">
      <div style="display:grid;grid-template-columns:1.4fr 0.6fr;gap:26px;align-items:start;">
        <div>
          <div class="section-head">
            <div>
              <h2>热门阅读</h2>
              <p>按访问量排序的当下热门</p>
            </div>
            <router-link to="/books?sort=hot">更多热门 →</router-link>
          </div>
          <div class="book-grid five">
            <BookCard v-for="book in data.hotBooks.slice(0, 5)" :key="book.id" :book="book" />
          </div>
        </div>
        <div>
          <div class="section-head">
            <div><h2>阅读榜</h2><p>点击量实时累计</p></div>
            <router-link to="/rankings">完整榜单 →</router-link>
          </div>
          <div class="rank-list">
            <router-link v-for="item in data.rankings" :key="item.book.id" :to="`/book/${item.book.id}`" class="rank-row">
              <div class="rank-no" :class="`top${item.rank}`">{{ item.rank }}</div>
              <img :src="item.book.coverUrl" class="rank-cover" alt="" />
              <div class="rank-info">
                <strong>{{ item.book.title }}</strong>
                <p>{{ item.book.authorName }} · {{ item.book.categoryName }}</p>
              </div>
              <div class="rank-score">{{ Number(item.score || 0).toFixed(0) }}</div>
            </router-link>
            <div v-if="data.rankings.length === 0" class="empty-state">还没有阅读记录，去读一章吧</div>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import BookCard from '../components/BookCard.vue'
import { portalApi } from '../api/portal'

const data = reactive<any>({ categories: [], newBooks: [], hotBooks: [], rankings: [] })
const heroBooks = computed(() => data.newBooks.slice(0, 3))

onMounted(async () => {
  const home: any = await portalApi.home()
  Object.assign(data, home)
})
</script>
