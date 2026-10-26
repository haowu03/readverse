<template>
  <div>
    <div class="page-head">
      <div>
        <h1>数据看板</h1>
        <p>内容、阅读与采集的实时概览 · {{ todayText }}</p>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="loadAll">刷新数据</el-button>
      </div>
    </div>

    <div class="stat-grid">
      <div class="panel stat-card">
        <div class="stat-label">书籍总数</div>
        <div class="stat-value">{{ summary.books }}</div>
        <div class="stat-foot">已上架与待上架内容</div>
        <div class="stat-mark"><el-icon><Notebook /></el-icon></div>
      </div>
      <div class="panel stat-card">
        <div class="stat-label">章节总数</div>
        <div class="stat-value">{{ summary.chapters }}</div>
        <div class="stat-foot">可供阅读的发布章节</div>
        <div class="stat-mark"><el-icon><Document /></el-icon></div>
      </div>
      <div class="panel stat-card">
        <div class="stat-label">注册用户</div>
        <div class="stat-value">{{ summary.users }}</div>
        <div class="stat-foot">读者 / 运营 / 管理员</div>
        <div class="stat-mark"><el-icon><User /></el-icon></div>
      </div>
      <div class="panel stat-card">
        <div class="stat-label">今日阅读</div>
        <div class="stat-value">{{ summary.todayReads }}</div>
        <div class="stat-foot">今日发生阅读行为的记录数</div>
        <div class="stat-mark"><el-icon><Reading /></el-icon></div>
      </div>
    </div>

    <div class="stat-grid" style="grid-template-columns: repeat(2, minmax(0, 1fr));">
      <div class="panel stat-card">
        <div class="stat-label">采集任务完成率</div>
        <div class="stat-value">{{ summary.crawlSuccessRate.toFixed(1) }}<span style="font-size:16px">%</span></div>
        <div class="stat-foot">成功任务 / 全部任务</div>
        <div class="stat-mark"><el-icon><DataAnalysis /></el-icon></div>
      </div>
      <div class="panel stat-card">
        <div class="stat-label">待处理任务</div>
        <div class="stat-value">{{ summary.pendingTasks }}</div>
        <div class="stat-foot">PENDING / RUNNING / RETRYING</div>
        <div class="stat-mark"><el-icon><Timer /></el-icon></div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel chart-panel">
        <h3>新增书籍趋势</h3>
        <div class="chart-sub">最近 7 天入库的书籍数量</div>
        <div ref="bookChartRef" class="chart"></div>
      </div>
      <div class="panel chart-panel">
        <h3>阅读活跃</h3>
        <div class="chart-sub">最近 7 天阅读记录变化</div>
        <div ref="readChartRef" class="chart"></div>
      </div>
    </div>

    <div class="panel table-panel" style="margin-top: 18px;">
      <div style="display:flex;align-items:center;justify-content:space-between;padding:14px 16px 10px;">
        <div>
          <h3 style="margin:0;font-size:15px;">最近更新的书籍</h3>
          <div class="cell-sub">按更新时间倒序</div>
        </div>
        <el-button text type="primary" @click="$router.push('/content/books')">查看全部 →</el-button>
      </div>
      <el-table :data="recentBooks" style="width:100%">
        <el-table-column label="书籍" min-width="220">
          <template #default="{ row }">
            <div style="display:flex;gap:12px;align-items:center;">
              <el-image :src="row.coverUrl" fit="cover" class="cover-thumb" />
              <div>
                <div class="cell-title">{{ row.title }}</div>
                <div class="cell-sub">{{ row.authorName }} · {{ row.categoryName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain" class="status-tag">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节" width="80" />
        <el-table-column prop="visitCount" label="访问" width="100" />
        <el-table-column prop="score" label="评分" width="80" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { Refresh } from '@element-plus/icons-vue'
import { dashboardApi } from '../api/system'
import { bookApi } from '../api/content'

const summary = reactive<any>({
  books: 0, chapters: 0, users: 0, todayReads: 0, pendingTasks: 0, crawlSuccessRate: 0
})
const recentBooks = ref<any[]>([])
const bookChartRef = ref<HTMLDivElement>()
const readChartRef = ref<HTMLDivElement>()
let bookChart: echarts.ECharts | null = null
let readChart: echarts.ECharts | null = null

const todayText = new Intl.DateTimeFormat('zh-CN', { dateStyle: 'full' }).format(new Date())

function statusText(status: string) {
  return ({ DRAFT: '草稿', PENDING: '待审', ON_SHELF: '已上架', OFF_SHELF: '已下架', REJECTED: '已驳回' } as any)[status] || status
}
function statusType(status: string) {
  return ({ DRAFT: 'info', PENDING: 'warning', ON_SHELF: 'success', OFF_SHELF: 'info', REJECTED: 'danger' } as any)[status] || 'info'
}

function lineOption(data: any[], color: string) {
  return {
    grid: { left: 36, right: 16, top: 24, bottom: 28 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.map((p) => p.date.slice(5)),
      axisLine: { lineStyle: { color: '#e6dbcf' } },
      axisLabel: { color: '#9b8f83', fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f0e8df' } },
      axisLabel: { color: '#9b8f83', fontSize: 11 }
    },
    series: [{
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      data: data.map((p) => p.value),
      lineStyle: { width: 2.5, color },
      itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '33' },
          { offset: 1, color: color + '00' }
        ])
      }
    }]
  }
}

async function loadAll() {
  try {
    const [s, trend, activity, books] = await Promise.all([
      dashboardApi.summary(),
      dashboardApi.bookTrend(7),
      dashboardApi.readActivity(7),
      bookApi.page({ current: 1, size: 5 })
    ])
    Object.assign(summary, s)
    recentBooks.value = books.records || []
    await nextTick()
    if (bookChartRef.value) {
      bookChart = bookChart || echarts.init(bookChartRef.value)
      bookChart.setOption(lineOption(trend, '#b06b4b'), true)
    }
    if (readChartRef.value) {
      readChart = readChart || echarts.init(readChartRef.value)
      readChart.setOption(lineOption(activity, '#4b7f6a'), true)
    }
  } catch (e) {
    // interceptor shows message
  }
}

function resize() {
  bookChart?.resize()
  readChart?.resize()
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  bookChart?.dispose()
  readChart?.dispose()
})
</script>
