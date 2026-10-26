<template>
  <div>
    <div class="page-head">
      <div>
        <h1>分类管理</h1>
        <p>支持二级分类树，删除前会校验子分类与书籍引用</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('content:category:add')" type="primary" :icon="Plus" @click="openCreate()">新增分类</el-button>
      </div>
    </div>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="tree" row-key="id" :tree-props="{ children: 'children' }" default-expand-all style="width:100%">
        <el-table-column prop="name" label="分类名称" min-width="220">
          <template #default="{ row }"><span style="font-weight:600;">{{ row.name }}</span></template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="160" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="icon" label="图标" width="120">
          <template #default="{ row }"><span class="cell-sub">{{ row.icon || '—' }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('content:category:add')" text type="primary" size="small" @click="openCreate(row)">新增子分类</el-button>
            <el-button v-if="can('content:category:edit')" text size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('content:category:delete')" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级分类">
          <el-select v-model="form.parentId" style="width:100%" placeholder="顶级分类">
            <el-option label="顶级分类" value="0" />
            <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" :disabled="c.id === editingId" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="分类编码" required><el-input v-model="form.code" placeholder="例如：scifi" /></el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
          <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" style="width:100%" /></el-form-item>
          <el-form-item label="状态">
            <el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" />
          </el-form-item>
        </div>
        <el-form-item label="图标"><el-input v-model="form.icon" placeholder="Element Plus 图标名，可留空" /></el-form-item>
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
import { Plus } from '@element-plus/icons-vue'
import { categoryApi } from '../../api/content'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const tree = ref<any[]>([])
const flat = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<any>({ parentId: '0', name: '', code: '', sort: 0, icon: '', status: 1 })

const can = (code: string) => store.hasPerm(code)
const flatCategories = computed(() => flat.value.filter((c) => c.id !== editingId.value))

function flatten(nodes: any[], result: any[] = []) {
  for (const node of nodes || []) {
    result.push(node)
    if (node.children?.length) flatten(node.children, result)
  }
  return result
}
async function load() {
  loading.value = true
  try {
    tree.value = await categoryApi.tree()
    flat.value = flatten(await categoryApi.all())
  } finally {
    loading.value = false
  }
}
function openCreate(parent?: any) {
  editingId.value = ''
  Object.assign(form, { parentId: parent?.id || '0', name: '', code: '', sort: 0, icon: '', status: 1 })
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, { parentId: String(row.parentId || 0), name: row.name, code: row.code, sort: row.sort || 0, icon: row.icon || '', status: row.status ?? 1 })
  dialogVisible.value = true
}
async function save() {
  if (!form.name || !form.code) { ElMessage.warning('请填写分类名称与编码'); return }
  saving.value = true
  try {
    const payload = { ...form, parentId: form.parentId === '0' ? 0 : form.parentId }
    if (editingId.value) await categoryApi.update(editingId.value, payload)
    else await categoryApi.create(payload)
    ElMessage.success('已保存')
    dialogVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await categoryApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
