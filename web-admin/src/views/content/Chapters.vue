<template>
  <div>
    <div class="page-head">
      <div>
        <h1>章节管理</h1>
        <p>按书籍维护目录、正文与发布状态</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('content:chapter:add')" type="primary" :icon="Plus" :disabled="!selectedBook" @click="openCreate">新增章节</el-button>
      </div>
    </div>

    <div class="panel panel-pad">
      <div class="filter-bar">
        <el-select v-model="selectedBook" filterable placeholder="选择书籍" style="width: 320px" @change="loadChapters">
          <el-option v-for="b in books" :key="b.id" :label="`${b.title} · ${b.authorName}`" :value="b.id" />
        </el-select>
        <span class="cell-sub" v-if="selectedBook">共 {{ chapters.length }} 章</span>
        <div class="spacer"></div>
        <el-button :icon="Refresh" @click="loadChapters">刷新</el-button>
      </div>

      <el-table v-loading="loading" :data="chapters" style="width: 100%">
        <el-table-column prop="chapterNo" label="章节号" width="80" />
        <el-table-column label="标题" min-width="260">
          <template #default="{ row }">
            <div class="cell-title">{{ row.title }}</div>
            <div class="cell-sub">{{ (row.content || '').slice(0, 46) }}{{ (row.content || '').length > 46 ? '…' : '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="wordCount" label="字数" width="90" />
        <el-table-column label="VIP" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isVip" type="warning" effect="plain" size="small">VIP</el-tag>
            <span v-else class="cell-sub">免费</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" effect="plain" class="status-tag">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }"><span class="cell-sub">{{ row.publishTime || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('content:chapter:edit')" text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('content:chapter:publish')" text size="small" @click="publish(row)">发布</el-button>
            <el-button v-if="can('content:chapter:delete')" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑章节' : '新增章节'" width="820px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <div style="display:grid;grid-template-columns:160px 1fr;gap:16px;">
          <el-form-item label="章节号" required>
            <el-input-number v-model="form.chapterNo" :min="1" :max="9999" style="width:100%" />
          </el-form-item>
          <el-form-item label="标题" required>
            <el-input v-model="form.title" placeholder="例如：第三章 风起" />
          </el-form-item>
        </div>
        <el-form-item label="正文" required>
          <el-input v-model="form.content" type="textarea" :rows="14" placeholder="粘贴或输入章节正文" />
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
          <el-form-item label="VIP">
            <el-switch v-model="form.isVip" active-text="VIP 章节" />
          </el-form-item>
          <el-form-item label="字数">
            <span class="cell-sub">约 {{ (form.content || '').replace(/\s/g, '').length }} 字</span>
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh } from '@element-plus/icons-vue'
import { bookApi, chapterApi } from '../../api/content'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const books = ref<any[]>([])
const selectedBook = ref('')
const chapters = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<any>({ chapterNo: 1, title: '', content: '', isVip: false, status: 'PUBLISHED' })

const selectedBookTitle = computed(() => books.value.find((b) => b.id === selectedBook.value)?.title || '')
const can = (code: string) => store.hasPerm(code)

async function loadBooks() {
  const page: any = await bookApi.page({ current: 1, size: 100 })
  books.value = page.records || []
  if (!selectedBook.value && books.value.length) {
    selectedBook.value = books.value[0].id
    await loadChapters()
  }
}
async function loadChapters() {
  if (!selectedBook.value) return
  loading.value = true
  try {
    chapters.value = await chapterApi.list(selectedBook.value)
  } finally {
    loading.value = false
  }
}
function openCreate() {
  editingId.value = ''
  const nextNo = chapters.value.length ? Math.max(...chapters.value.map((c) => c.chapterNo || 0)) + 1 : 1
  Object.assign(form, { chapterNo: nextNo, title: '', content: '', isVip: false, status: 'PUBLISHED' })
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, {
    chapterNo: row.chapterNo, title: row.title, content: row.content || '',
    isVip: !!row.isVip, status: row.status || 'PUBLISHED'
  })
  dialogVisible.value = true
}
async function save() {
  if (!form.title || !form.content || !form.chapterNo) {
    ElMessage.warning('请填写章节号、标题与正文')
    return
  }
  saving.value = true
  try {
    const payload = { ...form, bookId: selectedBook.value }
    if (editingId.value) {
      await chapterApi.update(editingId.value, payload)
      ElMessage.success('章节已更新')
    } else {
      await chapterApi.create(payload)
      ElMessage.success('章节已创建')
    }
    dialogVisible.value = false
    await loadChapters()
  } finally {
    saving.value = false
  }
}
async function publish(row: any) {
  await chapterApi.publish(row.id)
  ElMessage.success('章节已发布')
  loadChapters()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除确认', { type: 'warning' })
  await chapterApi.remove(row.id)
  ElMessage.success('已删除')
  loadChapters()
}

onMounted(loadBooks)
</script>
