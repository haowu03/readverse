<template>
  <div>
    <div class="page-head">
      <div>
        <h1>角色权限</h1>
        <p>RBAC 角色与权限点绑定；内置管理员角色不可删除</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('system:role:add')" type="primary" :icon="Plus" @click="openCreate">新增角色</el-button>
      </div>
    </div>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="roles" style="width:100%">
        <el-table-column prop="name" label="角色" min-width="180">
          <template #default="{ row }"><div class="cell-title">{{ row.name }}</div><div class="cell-sub">{{ row.code }}</div></template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="240" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="权限点" width="100">
          <template #default="{ row }"><span class="amount">{{ row.permissionIds?.length || 0 }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('system:role:edit')" text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('system:role:delete') && row.code !== 'ADMIN'" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑角色' : '新增角色'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色编码" required><el-input v-model="form.code" placeholder="例如：EDITOR" :disabled="editingId === adminRoleId" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="排序"><el-input-number v-model="form.sort" :min="0" /></el-form-item>
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
import { roleApi } from '../../api/system'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const roles = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<any>({ code: '', name: '', description: '', sort: 0 })
const can = (code: string) => store.hasPerm(code)
const adminRoleId = computed(() => roles.value.find((r) => r.code === 'ADMIN')?.id)

async function load() {
  loading.value = true
  try { roles.value = await roleApi.list() } finally { loading.value = false }
}
function openCreate() {
  editingId.value = ''
  Object.assign(form, { code: '', name: '', description: '', sort: 0 })
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, { code: row.code, name: row.name, description: row.description || '', sort: row.sort || 0 })
  dialogVisible.value = true
}
async function save() {
  if (!form.name || !form.code) { ElMessage.warning('请填写角色名称与编码'); return }
  saving.value = true
  try {
    if (editingId.value) await roleApi.update(editingId.value, form)
    else await roleApi.create(form)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除角色「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await roleApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
