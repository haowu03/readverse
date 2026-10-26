<template>
  <div class="simple-page">
    <div class="simple-code">404</div>
    <h2>页面不存在</h2>
    <p>你访问的地址可能已经移动或被删除。</p>
    <el-button type="primary" @click="$router.push('/dashboard')">回到看板</el-button>
  </div>
</template>
