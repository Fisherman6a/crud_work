<template>
    <div class="page-container">
        <slot></slot>
    </div>
</template>

<style scoped>
.page-container {
    /* 核心：限制最大宽度，并居中 */
    max-width: 1200px;
    /* 或者 80% */
    margin: 0 auto;
    /* 上下0，左右自动(居中) */
    padding: 20px;
    /* 内边距，防止内容贴边 */

    /* 可选：加个背景色或阴影让它更像一张“纸” */
    /* background-color: #fff; */
    /* min-height: 80vh; */
}
</style>