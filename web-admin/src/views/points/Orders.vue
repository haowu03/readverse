<template>
  <div>
    <div class="page-head">
      <div>
        <h1>兑换订单</h1>
        <p>查看积分兑换流水与权益发放结果</p>
      </div>
      <div class="page-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" plain @click="reconcile">执行对账</el-button>
      </div>
    </div>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="orders" style="width:100%">
        <el-table-column prop="orderNo" label="订单号" min-width="210" />
        <el-table-column prop="userId" label="用户 ID" width="120" />
        <el-table-column prop="goodsId" label="商品 ID" width="110" />
        <el-table-column label="消耗积分" width="110">
          <template #default="{ row }"><span class="amount">{{ row.pointsCost }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag type="success" effect="plain">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="couponCode" label="券码" width="150" />
        <el-table-column prop="createTime" label="创建时间" width="170" />
      </el-table>
      <div class="pager">
        <el-pagination v-model:current-page="query.current" v-model:page-size="query.size" background layout="total, prev, pager, next" :total="total" @current-change="load" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { goodsApi } from '../../api/points'

const loading = ref(false)
const orders = ref<any[]>([])
const total = ref(0)
const query = reactive({ current: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const page: any = await goodsApi.orders(query)
    orders.value = page.records || []
    total.value = page.total || 0
  } finally { loading.value = false }
}
async function reconcile() {
  const diffs = await goodsApi.reconcile()
  if (!diffs.length) ElMessage.success('对账完成：账户余额与流水一致')
  else ElMessageBox.alert(diffs.join('\n'), '发现差异', { type: 'warning' })
}
onMounted(load)
</script>
