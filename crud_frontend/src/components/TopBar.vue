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
                <n-space :size="16" align="center">
                    <!-- 通知中心 -->
                    <NotificationCenter />

                    <!-- 用户菜单 -->
                    <n-dropdown :options="userOptions" @select="handleSelect">
                        <n-button text class="user-btn">
                            <n-avatar round size="small" style="margin-right: 8px; background-color: #2080f0">
                                {{ username?.charAt(0)?.toUpperCase() || 'U' }}
                            </n-avatar>
                            {{ username }} ({{ roleName }})
                        </n-button>
                    </n-dropdown>
                </n-space>
            </div>
        </n-space>
    </div>
</template>

<script setup>
import { computed, h, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
    useMessage, NIcon,
    NSpace,
    NText,
    NButton,
    NDropdown,
    NAvatar
} from 'naive-ui'
import { LogOutOutline, PersonOutline } from '@vicons/ionicons5'
import { useSystemStore } from '../stores/system'
import { useNotificationStore } from '../stores/notification'
import NotificationCenter from './NotificationCenter.vue'

const router = useRouter()
const message = useMessage()
const systemStore = useSystemStore()
const notificationStore = useNotificationStore()

// 状态
const username = localStorage.getItem('username') || 'User'
const role = localStorage.getItem('role')
const roleName = computed(() => (role === 'admin' || role === 'ADMIN') ? '管理员' : '学生')

// 使用 Pinia store 的时间
const currentTime = computed(() => systemStore.formattedTime)

const changeTime = () => {
    const t = prompt("【测试功能】请输入模拟时间:", currentTime.value)
    if (t) {
        systemStore.updateTime(t)
    }
}

// 下拉菜单配置
const userOptions = [
    { label: '个人中心', key: 'profile', icon: renderIcon(PersonOutline) },
    { label: '退出登录', key: 'logout', icon: renderIcon(LogOutOutline) }
]

function renderIcon(icon) {
    return () => h(NIcon, null, { default: () => h(icon) })
}

const handleSelect = (key) => {
    if (key === 'logout') {
        localStorage.clear()
        message.success('已退出登录')
        router.push('/login')
    } else if (key === 'profile') {
        // 跳转个人中心逻辑...
    }
}

// 初始化时加载配置
onMounted(() => {
    systemStore.loadConfig()

    // 初始化WebSocket和通知
    const userId = localStorage.getItem('username')
    if (userId) {
        notificationStore.initWebSocket(userId)
        notificationStore.fetchNotifications()
        notificationStore.fetchUnreadCount()
        notificationStore.requestNotificationPermission()
    }
})
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
