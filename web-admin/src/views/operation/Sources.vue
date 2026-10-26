<template>
  <div>
    <div class="page-head">
      <div>
        <h1>书源管理</h1>
        <p>仅内置示例源，不抓取真实站点；用于演示同步、幂等与失败重放</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('crawl:source:add')" type="primary" :icon="Plus" @click="openCreate">新增书源</el-button>
      </div>
    </div>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="sources" style="width:100%">
        <el-table-column prop="name" label="书源名称" min-width="200">
          <template #default="{ row }">
            <div class="cell-title">{{ row.name }}</div>
            <div class="cell-sub">{{ row.baseUrl || 'classpath:mock/books.json' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="sourceType" label="类型" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastSyncTime" label="最后同步" width="170">
          <template #default="{ row }"><span class="cell-sub">{{ row.lastSyncTime || '尚未同步' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('crawl:source:test')" text size="small" @click="test(row)">测试</el-button>
            <el-button v-if="can('crawl:task:list')" text type="primary" size="small" @click="trigger(row, 'FULL')">全量同步</el-button>
            <el-button v-if="can('crawl:task:list')" text size="small" @click="trigger(row, 'INCR')">增量同步</el-button>
            <el-button v-if="can('crawl:source:edit')" text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('crawl:source:delete')" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑书源' : '新增书源'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="86px">
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
          <el-form-item label="类型">
            <el-select v-model="form.sourceType" style="width:100%">
              <el-option label="MOCK 示例源" value="MOCK" />
              <el-option label="HTTP JSON" value="HTTP" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
          </el-form-item>
        </div>
        <el-form-item label="Base URL"><el-input v-model="form.baseUrl" placeholder="示例源可留空" /></el-form-item>
        <el-form-item label="配置 JSON"><el-input v-model="form.configJson" type="textarea" :rows="3" placeholder='{"note":"..."}' /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { crawlApi } from '../../api/crawl'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const sources = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<any>({ name: '', sourceType: 'MOCK', baseUrl: '', configJson: '', status: 1 })
const can = (code: string) => store.hasPerm(code)

async function load() {
  loading.value = true
  try { sources.value = await crawlApi.sources() } finally { loading.value = false }
}
function openCreate() {
  editingId.value = ''
  Object.assign(form, { name: '', sourceType: 'MOCK', baseUrl: '', configJson: '', status: 1 })
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, sourceType: row.sourceType, baseUrl: row.baseUrl || '', configJson: row.configJson || '', status: row.status ?? 1 })
  dialogVisible.value = true
}
async function save() {
  if (!form.name) { ElMessage.warning('请填写书源名称'); return }
  saving.value = true
  try {
    if (editingId.value) await crawlApi.updateSource(editingId.value, form)
    else await crawlApi.createSource(form)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function test(row: any) {
  const count = await crawlApi.testSource(row.id)
  ElMessage.success(`书源可用，示例源返回 ${count} 本书`)
}
async function trigger(row: any, type: string) {
  const task: any = await crawlApi.trigger(row.id, type)
  if (task.state === 'SUCCESS') ElMessage.success(`同步完成：${task.message}`)
  else ElMessage.warning(`同步结束：${task.state} · ${task.message || ''}`)
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除书源「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await crawlApi.removeSource(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
