<template>
    <div class="page-container">
        <slot></slot>
    </div>
</template>

<style scoped>
.page-container {
    /* 限制最大宽度，大屏下不至于太宽 */
    max-width: 1200px;
    /* 上下留白，左右自动居中 */
    margin: 24px auto;
    /* 内部留白 */
    padding: 24px;

    /* 视觉风格：白色背景 + 圆角 + 阴影 */
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.08),
        0 3px 6px 0 rgba(0, 0, 0, 0.06),
        0 5px 12px 4px rgba(0, 0, 0, 0.04);

    /* 最小高度，保证内容少时也不太难看 */
    min-height: calc(100vh - 120px);
}
</style>
