<template>
    <div class="top-bar">
        <n-space justify="space-between" align="center" style="height: 100%">
            <div class="left-area">
                <n-text strong style="font-size: 16px">
                    当前时间: {{ currentTime }}
                </n-text>
                <n-button text size="tiny" type="primary" @click="changeTime" style="margin-left: 8px">
                    (修改)
                </n-button>
            </div>

            <div class="right-area">
                <n-dropdown :options="userOptions" @select="handleSelect">
                    <n-button text class="user-btn">
                        <n-avatar round size="small" style="margin-right: 8px; background-color: #2080f0">
                            {{ username?.charAt(0)?.toUpperCase() || 'U' }}
                        </n-avatar>
                        {{ username }} ({{ roleName }})
                    </n-button>
                </n-dropdown>
            </div>
        </n-space>
    </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import {
    useMessage, NIcon,
    // 👇 新增以下引入：
    NSpace,
    NText,
    NButton,
    NDropdown,
    NAvatar
} from 'naive-ui'
import { LogOutOutline, PersonOutline } from '@vicons/ionicons5' // 需安装 @vicons/ionicons5

const router = useRouter()
const message = useMessage()

// 状态
const username = localStorage.getItem('username') || 'User'
const role = localStorage.getItem('role')
const roleName = computed(() => (role === 'admin' || role === 'ADMIN') ? '管理员' : '学生')

// 模拟全局时间
const currentTime = ref('2025-09-01 08:00:00')

const changeTime = () => {
    const t = prompt("【测试功能】请输入模拟时间:", currentTime.value)
    if (t) currentTime.value = t
}

// 下拉菜单配置
const userOptions = [
    { label: '个人中心', key: 'profile', icon: renderIcon(PersonOutline) },
    { label: '退出登录', key: 'logout', icon: renderIcon(LogOutOutline) }
]

function renderIcon(icon) {
    return () => h(NIcon, null, { default: () => h(icon) })
}

import { h } from 'vue'

const handleSelect = (key) => {
    if (key === 'logout') {
        localStorage.clear()
        message.success('已退出登录')
        router.push('/login')
    } else if (key === 'profile') {
        // 跳转个人中心逻辑...
    }
}
</script>

<style scoped>
.top-bar {
    height: 64px;
    padding: 0 24px;
    background: #fff;
    box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
    /* 底部阴影，区分内容区 */
    z-index: 10;
}

.user-btn {
    display: flex;
    align-items: center;
}
</style>
