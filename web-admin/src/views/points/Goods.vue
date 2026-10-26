<template>
  <div>
    <div class="page-head">
      <div>
        <h1>积分商品</h1>
        <p>维护兑换商品、积分价格与库存</p>
      </div>
      <div class="page-actions">
        <el-button v-if="can('points:goods:add')" type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
      </div>
    </div>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="goods" style="width:100%">
        <el-table-column prop="name" label="商品名称" min-width="200">
          <template #default="{ row }"><div class="cell-title">{{ row.name }}</div><div class="cell-sub">{{ row.configJson || '—' }}</div></template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="160" />
        <el-table-column label="积分价格" width="110">
          <template #default="{ row }"><span class="amount" style="color:#b06b4b;font-weight:600;">{{ row.pointsPrice }}</span></template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" effect="plain">{{ row.status === 1 ? '上架' : '下架' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="can('points:goods:edit')" text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="can('points:goods:delete')" text type="danger" size="small" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑商品' : '新增商品'" width="520px" destroy-on-close>
      <el-form :model="form" label-width="90px">
        <el-form-item label="商品名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型" required>
          <el-select v-model="form.type" style="width:100%">
            <el-option label="章节解锁券 (CHAPTER_UNLOCK)" value="CHAPTER_UNLOCK" />
            <el-option label="书籍畅读券 (BOOK_VIP)" value="BOOK_VIP" />
            <el-option label="普通券码 (COUPON)" value="COUPON" />
          </el-select>
        </el-form-item>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
          <el-form-item label="积分价格"><el-input-number v-model="form.pointsPrice" :min="1" style="width:100%" /></el-form-item>
          <el-form-item label="库存"><el-input-number v-model="form.stock" :min="0" style="width:100%" /></el-form-item>
        </div>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="上架" /></el-form-item>
        <el-form-item label="配置"><el-input v-model="form.configJson" type="textarea" :rows="3" placeholder='{"note":"..."}' /></el-form-item>
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
import { goodsApi } from '../../api/points'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const saving = ref(false)
const goods = ref<any[]>([])
const dialogVisible = ref(false)
const editingId = ref('')
const form = reactive<any>({ name: '', type: 'CHAPTER_UNLOCK', pointsPrice: 30, stock: 100, status: 1, configJson: '' })
const can = (code: string) => store.hasPerm(code)

async function load() {
  loading.value = true
  try { goods.value = await goodsApi.list() } finally { loading.value = false }
}
function openCreate() {
  editingId.value = ''
  Object.assign(form, { name: '', type: 'CHAPTER_UNLOCK', pointsPrice: 30, stock: 100, status: 1, configJson: '' })
  dialogVisible.value = true
}
function openEdit(row: any) {
  editingId.value = row.id
  Object.assign(form, { name: row.name, type: row.type, pointsPrice: row.pointsPrice, stock: row.stock, status: row.status, configJson: row.configJson || '' })
  dialogVisible.value = true
}
async function save() {
  if (!form.name) { ElMessage.warning('请填写商品名称'); return }
  saving.value = true
  try {
    if (editingId.value) await goodsApi.update(editingId.value, form)
    else await goodsApi.create(form)
    ElMessage.success('已保存')
    dialogVisible.value = false
    load()
  } finally { saving.value = false }
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确定删除「${row.name}」吗？`, '删除确认', { type: 'warning' })
  await goodsApi.remove(row.id)
  ElMessage.success('已删除')
  load()
}
onMounted(load)
</script>
