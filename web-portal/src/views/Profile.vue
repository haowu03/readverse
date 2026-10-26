<template>
  <div>
    <div class="page-title">
      <div><h1>个人中心</h1><p>签到、积分与兑换记录都在这里</p></div>
      <el-button text @click="$router.push('/bookshelf')">我的书架 →</el-button>
    </div>

    <div class="profile-grid">
      <div style="display:flex;flex-direction:column;gap:20px;">
        <div class="panel-card">
          <div style="display:flex;align-items:center;gap:14px;">
            <el-avatar :size="52" :src="store.user?.avatar">{{ store.displayName[0] }}</el-avatar>
            <div>
              <h3 style="margin:0 0 4px;">{{ store.displayName }}</h3>
              <div style="color:#9a8e82;font-size:12px;">@{{ store.user?.username }}</div>
            </div>
          </div>
          <div style="margin-top:18px;padding-top:16px;border-top:1px dashed #e9dfd3;">
            <div class="points-label">当前积分</div>
            <div class="points-balance">{{ account.balance }}</div>
            <div style="color:#a3978b;font-size:11px;">累计获得 {{ account.totalEarned }} · 累计消耗 {{ account.totalSpent }}</div>
          </div>
          <el-button type="primary" round style="width:100%;margin-top:16px;" :disabled="monthData.signedToday" @click="signIn">
            {{ monthData.signedToday ? '今天已签到' : '签到领积分 +5' }}
          </el-button>
        </div>

        <div class="panel-card">
          <div style="display:flex;align-items:center;justify-content:space-between;">
            <div>
              <h3 style="margin:0;font-size:16px;">本月签到</h3>
              <div style="color:#9a8e82;font-size:11px;margin-top:4px;">连续 {{ monthData.continuousDays }} 天</div>
            </div>
            <el-tag type="warning" effect="plain" size="small">{{ monthData.month }}</el-tag>
          </div>
          <div class="calendar-grid">
            <div v-for="d in ['日','一','二','三','四','五','六']" :key="d" class="calendar-cell head">{{ d }}</div>
            <div v-for="(day, idx) in calendarCells" :key="idx" class="calendar-cell" :class="{ signed: day && signedSet.has(day) }">
              {{ day || '' }}
            </div>
          </div>
        </div>
      </div>

      <div style="display:flex;flex-direction:column;gap:20px;">
        <div class="panel-card">
          <div style="display:flex;align-items:center;justify-content:space-between;margin-bottom:14px;">
            <div><h3 style="margin:0;font-size:16px;">积分兑换</h3><div style="color:#9a8e82;font-size:11px;margin-top:4px;">虚拟商品，仅用于演示</div></div>
            <span style="color:#9a5b3d;font-size:12px;">余额 {{ account.balance }}</span>
          </div>
          <div class="goods-grid">
            <div v-for="g in goods" :key="g.id" class="goods-card">
              <h4>{{ g.name }}</h4>
              <p>{{ g.configJson || '兑换后生成示例券码' }}</p>
              <div style="display:flex;align-items:center;justify-content:space-between;">
                <span class="price">{{ g.pointsPrice }} <small style="font-size:10px;font-weight:400;color:#a3978b;">积分</small></span>
                <el-button size="small" round :disabled="account.balance < g.pointsPrice || g.stock <= 0" @click="exchange(g)">兑换</el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="panel-card">
          <el-tabs v-model="tab">
            <el-tab-pane label="积分流水" name="logs">
              <el-table :data="logs" style="width:100%">
                <el-table-column label="变动" width="100">
                  <template #default="{ row }">
                    <span :style="{ color: row.changeAmount > 0 ? '#4b7f6a' : '#b06b4b', fontWeight: 600 }">{{ row.changeAmount > 0 ? '+' : '' }}{{ row.changeAmount }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="bizType" label="类型" width="120" />
                <el-table-column prop="remark" label="说明" />
                <el-table-column prop="balanceAfter" label="余额" width="90" />
                <el-table-column prop="createTime" label="时间" width="170" />
              </el-table>
              <div v-if="logs.length === 0" class="empty-state" style="padding:30px 0;">还没有积分记录</div>
            </el-tab-pane>
            <el-tab-pane label="兑换订单" name="orders">
              <el-table :data="orders" style="width:100%">
                <el-table-column prop="orderNo" label="订单号" min-width="190" />
                <el-table-column prop="pointsCost" label="积分" width="90" />
                <el-table-column prop="couponCode" label="券码" width="140" />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column prop="createTime" label="时间" width="170" />
              </el-table>
              <div v-if="orders.length === 0" class="empty-state" style="padding:30px 0;">还没有兑换记录</div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { portalApi } from '../api/portal'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const account = ref<any>({ balance: 0, totalEarned: 0, totalSpent: 0 })
const monthData = ref<any>({ month: '', signedDays: [], continuousDays: 0, signedToday: false })
const goods = ref<any[]>([])
const logs = ref<any[]>([])
const orders = ref<any[]>([])
const tab = ref('logs')

const calendarCells = computed(() => {
  const month = monthData.value.month || new Date().toISOString().slice(0, 7)
  const [year, m] = month.split('-').map(Number)
  const firstDay = new Date(year, m - 1, 1).getDay()
  const days = new Date(year, m, 0).getDate()
  const cells: (number | null)[] = []
  for (let i = 0; i < firstDay; i++) cells.push(null)
  for (let d = 1; d <= days; d++) cells.push(d)
  return cells
})
const signedSet = computed(() => new Set<number>(monthData.value.signedDays || []))

async function loadAccount() {
  account.value = await portalApi.pointsAccount()
  monthData.value = await portalApi.signMonth()
}
async function loadGoods() {
  goods.value = await portalApi.goods()
}
async function loadRecords() {
  const [logPage, orderPage] = await Promise.all([
    portalApi.pointsLogs({ current: 1, size: 20 }),
    portalApi.orders({ current: 1, size: 20 })
  ])
  logs.value = logPage.records || []
  orders.value = orderPage.records || []
}
async function loadAll() {
  await Promise.all([loadAccount(), loadGoods(), loadRecords()])
}
async function signIn() {
  try {
    const result = await portalApi.signIn()
    ElMessage.success(`签到成功，+${result.reward} 积分，连续 ${result.continuousDays} 天`)
    await loadAccount()
    await loadRecords()
  } catch (e) {
    // interceptor handles duplicate sign-in
  }
}
async function exchange(g: any) {
  await ElMessageBox.confirm(`使用 ${g.pointsPrice} 积分兑换「${g.name}」？`, '兑换确认', { type: 'warning' })
  const result = await portalApi.exchange({ goodsId: g.id })
  ElMessageBox.alert(`兑换成功！券码：${result.couponCode}，订单号：${result.orderNo}`, '兑换成功', { type: 'success' })
  await loadAll()
}
onMounted(loadAll)
</script>
