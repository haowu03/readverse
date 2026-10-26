<template>
  <div>
    <div class="page-head">
      <div>
        <h1>用户管理</h1>
        <p>查看读者与后台账号，调整启用状态与角色</p>
      </div>
    </div>

    <div class="panel panel-pad">
      <div class="filter-bar">
        <el-input v-model="query.keyword" clearable placeholder="搜索用户名/昵称" style="width: 240px" @keyup.enter="search" />
        <el-select v-model="query.status" clearable placeholder="全部状态" style="width: 130px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button text @click="reset">重置</el-button>
      </div>

      <el-table v-loading="loading" :data="users" style="width:100%">
        <el-table-column label="用户" min-width="220">
          <template #default="{ row }">
            <div style="display:flex;align-items:center;gap:10px;">
              <el-avatar :size="34" :src="row.avatar">{{ (row.nickname || row.username)[0] }}</el-avatar>
              <div><div class="cell-title">{{ row.nickname }}</div><div class="cell-sub">@{{ row.username }}</div></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="180">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" effect="plain" style="margin-right:4px;">{{ roleName(r) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="(v: boolean) => toggle(row, v)" />
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最近登录" width="170">
          <template #default="{ row }"><span class="cell-sub">{{ row.lastLoginTime || '从未登录' }}</span></template>
        </el-table-column>
        <el-table-column prop="createTime" label="注册时间" width="170" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="openRoles(row)">调整角色</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" background layout="total, prev, pager, next" :total="total" @current-change="load" />
      </div>
    </div>

    <el-dialog v-model="roleVisible" title="调整角色" width="420px">
      <div class="cell-sub" style="margin-bottom:12px;">用户：{{ currentUser?.nickname }}（{{ currentUser?.username }}）</div>
      <el-checkbox-group v-model="selectedRoles">
        <el-checkbox v-for="r in roles" :key="r.id" :label="r.id" style="display:block;margin:8px 0;">
          {{ r.name }} <span class="cell-sub">· {{ r.code }}</span>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRoles">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { roleApi, userApi } from '../../api/system'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const loading = ref(false)
const users = ref<any[]>([])
const roles = ref<any[]>([])
const total = ref(0)
const query = reactive<any>({ current: 1, size: 10, keyword: '', status: '' })
const roleVisible = ref(false)
const currentUser = ref<any>(null)
const selectedRoles = ref<string[]>([])

const roleName = (code: string) => ({ ADMIN: '管理员', OPERATOR: '运营', READER: '读者' } as any)[code] || code

async function load() {
  loading.value = true
  try {
    const page: any = await userApi.page(query)
    users.value = page.records || []
    total.value = page.total || 0
  } finally { loading.value = false }
}
function search() { query.current = 1; load() }
function reset() { query.keyword = ''; query.status = ''; query.current = 1; load() }
async function toggle(row: any, value: boolean) {
  await userApi.updateStatus(row.id, value ? 1 : 0)
  row.status = value ? 1 : 0
  ElMessage.success(value ? '账号已启用' : '账号已禁用')
}
function openRoles(row: any) {
  currentUser.value = row
  selectedRoles.value = (row.roles || []).map((code: string) => roles.value.find((r) => r.code === code)?.id).filter(Boolean)
  roleVisible.value = true
}
async function saveRoles() {
  await userApi.assignRoles(currentUser.value.id, selectedRoles.value)
  ElMessage.success('角色已更新')
  roleVisible.value = false
  load()
}
onMounted(async () => {
  roles.value = await roleApi.list()
  load()
})
</script>
