<template>
  <div class="simple-page">
    <div class="simple-code">403</div>
    <h2>没有权限访问这个页面</h2>
    <p>当前账号缺少对应的权限点，请联系管理员。</p>
    <el-button type="primary" @click="$router.push('/dashboard')">回到看板</el-button>
  </div>
</template>
