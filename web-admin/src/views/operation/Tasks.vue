<template>
  <div>
    <div class="page-head">
      <div>
        <h1>采集任务</h1>
        <p>查看任务状态、失败重试与作业日志；同一作业重复执行幂等</p>
      </div>
      <div class="page-actions">
        <el-select v-model="triggerSource" placeholder="选择书源" style="width: 220px">
          <el-option v-for="s in sources" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-button type="primary" :disabled="!triggerSource" @click="trigger('FULL')">触发全量</el-button>
        <el-button :disabled="!triggerSource" @click="trigger('INCR')">触发增量</el-button>
      </div>
    </div>

    <div class="panel panel-pad">
      <div class="filter-bar">
        <el-select v-model="query.state" clearable placeholder="全部状态" style="width: 160px" @change="search">
          <el-option label="待执行" value="PENDING" />
          <el-option label="执行中" value="RUNNING" />
          <el-option label="成功" value="SUCCESS" />
          <el-option label="失败" value="FAILED" />
          <el-option label="重试中" value="RETRYING" />
        </el-select>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <div class="spacer"></div>
        <span class="cell-sub">任务 {{ total }} 条</span>
      </div>

      <el-table v-loading="loading" :data="tasks" style="width:100%">
        <el-table-column prop="id" label="任务 ID" width="100" />
        <el-table-column prop="taskType" label="类型" width="100" />
        <el-table-column prop="bookId" label="书籍 ID" width="100">
          <template #default="{ row }">{{ row.bookId || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="stateType(row.state)" effect="plain" class="status-tag">{{ stateText(row.state) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="retryTimes" label="重试" width="80" />
        <el-table-column prop="message" label="结果" min-width="220">
          <template #default="{ row }"><span class="cell-sub">{{ row.message || '—' }}</span></template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始" width="160" />
        <el-table-column prop="endTime" label="结束" width="160" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('crawl:task:retry')" text type="primary" size="small" @click="retry(row)">重放</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" background layout="total, prev, pager, next" :total="total" @current-change="load" />
      </div>
    </div>

    <div class="panel table-panel" style="margin-top:18px;">
      <div style="padding:16px 16px 8px;">
        <h3 style="margin:0;font-size:15px;">作业日志</h3>
        <div class="cell-sub">每次执行的成功/失败数与耗时</div>
      </div>
      <el-table :data="logs" style="width:100%">
        <el-table-column prop="jobName" label="作业" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.state === 'SUCCESS' ? 'success' : row.state === 'FAILED' ? 'danger' : 'warning'" effect="plain">{{ row.state }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="successCount" label="成功" width="80" />
        <el-table-column prop="failCount" label="失败" width="80" />
        <el-table-column prop="costMs" label="耗时(ms)" width="110" />
        <el-table-column prop="startTime" label="开始" width="170" />
        <el-table-column prop="message" label="信息" min-width="200" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { crawlApi } from '../../api/crawl'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const tasks = ref<any[]>([])
const logs = ref<any[]>([])
const sources = ref<any[]>([])
const total = ref(0)
const triggerSource = ref('')
const query = reactive({ current: 1, size: 10, state: '' })
const can = (code: string) => store.hasPerm(code)

const stateText = (s: string) => ({ PENDING: '待执行', RUNNING: '执行中', SUCCESS: '成功', FAILED: '失败', RETRYING: '重试中' } as any)[s] || s
const stateType = (s: string) => ({ PENDING: 'info', RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', RETRYING: 'warning' } as any)[s] || 'info'

async function load() {
  loading.value = true
  try {
    const page: any = await crawlApi.tasks(query)
    tasks.value = page.records || []
    total.value = page.total || 0
    const logPage: any = await crawlApi.jobLogs({ current: 1, size: 8 })
    logs.value = logPage.records || []
  } finally { loading.value = false }
}
function search() { query.current = 1; load() }
async function retry(row: any) {
  const task: any = await crawlApi.retry(row.id)
  ElMessage.success(`重放完成：${task.state} · ${task.message || ''}`)
  load()
}
async function trigger(type: string) {
  const task: any = await crawlApi.trigger(triggerSource.value, type)
  ElMessage.success(`任务结束：${task.state} · ${task.message || ''}`)
  load()
}
onMounted(async () => {
  sources.value = await crawlApi.sources()
  if (sources.value.length) triggerSource.value = sources.value[0].id
  load()
})
</script>
