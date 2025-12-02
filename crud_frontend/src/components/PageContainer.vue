<template>
    <div class="page-container">
        <slot></slot>
    </div>
</template>

<style scoped>
.page-container {
    /* 核心：限制宽度 + 居中 */
    max-width: 1200px;
    margin: 20px auto;
    padding: 24px;

    /* Naive UI 风格的卡片背景 */
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.16),
        0 3px 6px 0 rgba(0, 0, 0, 0.12),
        0 5px 12px 4px rgba(0, 0, 0, 0.09);
    min-height: 80vh;
}
</style>
