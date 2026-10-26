<template>
  <div>
    <div class="page-head">
      <div>
        <h1>作者管理</h1>
        <p>维护作者资料与审核状态</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('content:author:add')" type="primary" :icon="Plus" @click="openCreate">新增作者</el-button>
      </div>
    </div>

    <div class="panel panel-pad">
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="搜索作者名" style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 150px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="authors" style="width:100%">
        <el-table-column label="作者" min-width="220">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:12px;">
              <el-avatar :size="40" :src="row.avatar">{{ (row.name || '作')[0] }}</el-avatar>
              <div>
                <div class="cell-title">{{ row.name }}</div>
                <div class="cell-sub">{{ row.intro || '暂无简介' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="bookCount" label="作品数" width="90" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain" class="status-tag">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('content:author:edit')" text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('content:author:audit') && row.status !== 'APPROVED'" text type="success" size="small" @click="audit(row, 'APPROVED')">通过</el-button>
            <el-button v-if="can('content:author:audit') && row.status !== 'REJECTED'" text type="warning" size="small" @click="audit(row, 'REJECTED')">驳回</el-button>
            <el-button v-if="can('content:author:delete')" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" background layout="total, prev, pager, next" :total="total" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑作者' : '新增作者'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="70px">
        <el-form-item label="姓名" required><el-input v-model="form.name" placeholder="作者笔名" /></el-form-item>
        <el-form-item label="头像"><el-input v-model="form.avatar" placeholder="头像 URL（可留空）" /></el-form-item>
        <el-form-item label="简介"><el-input v-model="form.intro" type="textarea" :rows="4" placeholder="作者简介" /></el-form-item>
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
import { Plus, Search } from '@element-plus/icons-vue'
import { authorApi } from '../../api/content'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const authors = ref<any[]>([])
const total = ref(0)
const dialogVisible = ref(false)
const editingId = ref('')
const query = reactive({ current: 1, size: 10, keyword: '', status: '' })
const form = reactive({ name: '', avatar: '', intro: '' })

const can = (code: string) => store.hasPerm(code)
const statusText = (s: string) => ({ PENDING: '待审核', APPROVED: '已通过', REJECTED: '已驳回' } as any)[s] || s
const statusType = (s: string) => ({ PENDING: 'warning', APPROVED: 'success', REJECTED: 'danger' } as any)[s] || 'info'

async function load() {
  loading.value = true
  try {
    const page: any = await authorApi.page(query)
    authors.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}
function search() { query.current = 1; load() }
function reset() { query.keyword = ''; query.status = ''; query.current = 1; load() }
function openCreate() {
  editingId.value = ''
  Object.assign(form, { name: '', avatar: '', intro: '' })
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, avatar: row.avatar || '', intro: row.intro || '' })
  dialogVisible.value = true
}
async function save() {
  if (!form.name) { ElMessage.warning('请填写作者名'); return }
  saving.value = true
  try {
    if (editingId.value) await authorApi.update(editingId.value, form)
    else await authorApi.create(form)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function audit(row: any, status: string) {
  let remark = ''
  if (status === 'REJECTED') {
    const result = await ElMessageBox.prompt('请填写驳回原因', '驳回作者', { inputPlaceholder: '例如：资料不完整' })
    remark = result.value
  }
  await authorApi.audit(row.id, { status, remark })
  ElMessage.success(status === 'APPROVED' ? '已通过' : '已驳回')
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除作者「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await authorApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
