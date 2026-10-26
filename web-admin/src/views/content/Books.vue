<template>
  <div>
    <div class="page-head">
      <div>
        <h1>书籍管理</h1>
        <p>维护书籍元数据、封面与上下架状态</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('content:book:add')" type="primary" :icon="Plus" @click="openCreate">新增书籍</el-button>
      </div>
    </div>

    <div class="panel panel-pad">
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="搜索书名" style="width: 220px" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 140px">
          <el-option label="草稿" value="DRAFT" />
          <el-option label="待审核" value="PENDING" />
          <el-option label="已上架" value="ON_SHELF" />
          <el-option label="已下架" value="OFF_SHELF" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
        <el-select v-model="query.categoryId" clearable placeholder="全部分类" style="width: 160px">
          <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
        <div class="spacer"></div>
        <span class="cell-sub">共 {{ total }} 本</span>
      </div>

      <el-table v-loading="loading" :data="books" style="width: 100%">
        <el-table-column label="书籍" min-width="280">
          <template #default="{ row }">
            <div style="display:flex;gap:12px;align-items:center;">
              <el-image :src="row.coverUrl" fit="cover" class="cover-thumb">
                <template #error><div class="cover-thumb" style="display:grid;place-items:center;font-size:11px;">无封面</div></template>
              </el-image>
              <div>
                <div class="cell-title">{{ row.title }}</div>
                <div class="cell-sub">{{ row.subtitle || '—' }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="authorName" label="作者" width="110" />
        <el-table-column prop="categoryName" label="分类" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain" class="status-tag">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chapterCount" label="章节" width="70" />
        <el-table-column prop="wordCount" label="字数" width="90">
          <template #default="{ row }">{{ (row.wordCount || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column prop="visitCount" label="访问" width="90" />
        <el-table-column prop="score" label="评分" width="80">
          <template #default="{ row }"><span style="color:#b06b4b;font-weight:600;">{{ Number(row.score || 0).toFixed(1) }}</span></template>
        </el-table-column>
        <el-table-column label="更新时间" width="150">
          <template #default="{ row }"><span class="cell-sub">{{ row.updateTime }}</span></template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('content:book:edit')" text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('content:book:publish')" text size="small" @click="togglePublish(row)">
              {{ row.status === 'ON_SHELF' ? '下架' : '上架' }}
            </el-button>
            <el-button v-if="can('content:book:delete')" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          v-model:current-page="query.current"
          v-model:page-size="query.size"
          background
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑书籍' : '新增书籍'" width="680px" destroy-on-close>
      <el-form :model="form" label-width="86px" label-position="right">
        <el-form-item label="书名" required>
          <el-input v-model="form.title" placeholder="请输入书名" />
        </el-form-item>
        <el-form-item label="副标题">
          <el-input v-model="form.subtitle" placeholder="可留空" />
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
          <el-form-item label="作者" required>
            <el-select v-model="form.authorId" filterable style="width:100%" placeholder="选择作者">
              <el-option v-for="a in authors" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="分类" required>
            <el-select v-model="form.categoryId" style="width:100%" placeholder="选择分类">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="封面">
          <div style="display:flex;gap:12px;align-items:flex-start;width:100%;">
            <el-image v-if="form.coverUrl" :src="form.coverUrl" fit="cover" style="width:72px;height:96px;border-radius:8px;" />
            <div style="flex:1;display:flex;flex-direction:column;gap:8px;">
              <el-input v-model="form.coverUrl" placeholder="封面图片 URL，或点击右侧上传" />
              <el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*">
                <el-button size="small" :loading="uploading">上传本地封面</el-button>
              </el-upload>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="form.intro" type="textarea" :rows="4" placeholder="一句话或一段简介" />
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
          <el-form-item label="VIP">
            <el-switch v-model="form.isVip" active-text="VIP 书籍" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status" style="width:100%">
              <el-option label="草稿" value="DRAFT" />
              <el-option label="待审核" value="PENDING" />
              <el-option label="已上架" value="ON_SHELF" />
              <el-option label="已下架" value="OFF_SHELF" />
            </el-select>
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
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { authorApi, bookApi, categoryApi, uploadApi } from '../../api/content'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const uploading = ref(false)
const books = ref<any[]>([])
const total = ref(0)
const authors = ref<any[]>([])
const categories = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const query = reactive({ current: 1, size: 10, keyword: '', status: '', categoryId: '' })
const form = reactive<any>({
  title: '', subtitle: '', authorId: '', categoryId: '', coverUrl: '', intro: '', isVip: false, status: 'DRAFT'
})

function can(code: string) { return store.hasPerm(code) }
function statusText(status: string) {
  return ({ DRAFT: '草稿', PENDING: '待审核', ON_SHELF: '已上架', OFF_SHELF: '已下架', REJECTED: '已驳回' } as any)[status] || status
}
function statusType(status: string) {
  return ({ DRAFT: 'info', PENDING: 'warning', ON_SHELF: 'success', OFF_SHELF: 'info', REJECTED: 'danger' } as any)[status] || 'info'
}

async function load() {
  loading.value = true
  try {
    const page: any = await bookApi.page(query)
    books.value = page.records || []
    total.value = page.total || 0
  } finally {
    loading.value = false
  }
}
async function loadOptions() {
  const [a, c] = await Promise.all([authorApi.all(), categoryApi.all()])
  authors.value = a || []
  categories.value = c || []
}
function search() { query.current = 1; load() }
function reset() { query.keyword = ''; query.status = ''; query.categoryId = ''; query.current = 1; load() }

function openCreate() {
  editingId.value = ''
  Object.assign(form, { title: '', subtitle: '', authorId: authors.value[0]?.id || '', categoryId: categories.value[0]?.id || '', coverUrl: '', intro: '', isVip: false, status: 'DRAFT' })
  dialogVisible.value = true
}
async function openEdit(row: any) {
  const detail: any = await bookApi.detail(row.id)
  editingId.value = row.id
  Object.assign(form, {
    title: detail.title, subtitle: detail.subtitle, authorId: detail.authorId, categoryId: detail.categoryId,
    coverUrl: detail.coverUrl, intro: detail.intro, isVip: !!detail.isVip, status: detail.status
  })
  dialogVisible.value = true
}
async function save() {
  if (!form.title || !form.authorId || !form.categoryId) {
    ElMessage.warning('请填写书名、作者与分类')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await bookApi.update(editingId.value, form)
      ElMessage.success('书籍已更新')
    } else {
      await bookApi.create(form)
      ElMessage.success('书籍已创建')
    }
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}
async function togglePublish(row: any) {
  if (row.status === 'ON_SHELF') {
    await bookApi.unpublish(row.id)
    ElMessage.success('已下架')
  } else {
    await bookApi.publish(row.id)
    ElMessage.success('已上架')
  }
  load()
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除《${row.title}》吗？章节也会一并删除。`, '删除确认', { type: 'warning' })
  await bookApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}
async function uploadCover(options: any) {
  uploading.value = true
  try {
    const url = await uploadApi.cover(options.file)
    form.coverUrl = url
    ElMessage.success('封面已上传')
  } finally {
    uploading.value = false
  }
}

onMounted(async () => {
  await Promise.all([loadOptions(), load()])
})
</script>
