<template>
    <n-popover
        trigger="click"
        placement="bottom-end"
        :show-arrow="false"
        :width="400"
        @update:show="handlePopoverToggle"
    >
        <template #trigger>
            <n-badge :value="unreadCount" :max="99" :show="unreadCount > 0">
                <n-button text size="large">
                    <template #icon>
                        <n-icon :component="NotificationsOutline" :size="24" />
                    </template>
                </n-button>
            </n-badge>
        </template>

        <div class="notification-center">
            <!-- 头部 -->
            <div class="notification-header">
                <n-space justify="space-between" align="center">
                    <n-text strong>通知中心</n-text>
                    <n-space :size="8">
                        <n-button
                            text
                            size="small"
                            :disabled="unreadCount === 0"
                            @click="handleMarkAllRead"
                        >
                            全部已读
                        </n-button>
                        <n-button
                            text
                            size="small"
                            type="error"
                            :disabled="notifications.length === 0"
                            @click="handleClearAll"
                        >
                            清空
                        </n-button>
                    </n-space>
                </n-space>
            </div>

            <n-divider style="margin: 8px 0" />

            <!-- 通知列表 -->
            <div class="notification-list">
                <n-scrollbar style="max-height: 400px">
                    <n-empty
                        v-if="notifications.length === 0"
                        description="暂无通知"
                        size="small"
                    >
                        <template #icon>
                            <n-icon :component="MailOutline" :size="48" />
                        </template>
                    </n-empty>

                    <div v-else>
                        <div
                            v-for="notification in recentNotifications"
                            :key="notification.id"
                            class="notification-item"
                            :class="{ 'unread': !notification.isRead }"
                            @click="handleNotificationClick(notification)"
                        >
                            <div class="notification-icon">
                                <n-icon
                                    :component="getNotificationIcon(notification.type)"
                                    :size="20"
                                    :color="getNotificationColor(notification.type)"
                                />
                            </div>

                            <div class="notification-content">
                                <div class="notification-title">
                                    <n-text strong>{{ notification.title }}</n-text>
                                    <n-badge
                                        v-if="!notification.isRead"
                                        dot
                                        type="error"
                                        :offset="[-4, 4]"
                                    />
                                </div>

                                <n-ellipsis :line-clamp="2" class="notification-text">
                                    {{ notification.content }}
                                </n-ellipsis>

                                <div class="notification-time">
                                    <n-text depth="3" :style="{ fontSize: '12px' }">
                                        {{ formatTime(notification.createTime) }}
                                    </n-text>
                                </div>
                            </div>

                            <div class="notification-actions">
                                <n-button
                                    text
                                    size="small"
                                    @click.stop="handleDelete(notification.id)"
                                >
                                    <template #icon>
                                        <n-icon :component="CloseOutline" />
                                    </template>
                                </n-button>
                            </div>
                        </div>
                    </div>
                </n-scrollbar>
            </div>

            <!-- 底部 -->
            <n-divider style="margin: 8px 0" />
            <div class="notification-footer">
                <n-button text block @click="handleViewAll">
                    查看全部通知
                </n-button>
            </div>
        </div>
    </n-popover>
</template>

<script setup>
import { computed } from 'vue'
import {
    NPopover, NBadge, NButton, NIcon, NText, NSpace, NDivider,
    NScrollbar, NEmpty, NEllipsis, useMessage, useDialog
} from 'naive-ui'
import {
    NotificationsOutline, MailOutline, CloseOutline,
    InformationCircleOutline, CheckmarkCircleOutline,
    WarningOutline, AlertCircleOutline
} from '@vicons/ionicons5'
import { useNotificationStore } from '@/stores/notification'

const notificationStore = useNotificationStore()
const message = useMessage()
const dialog = useDialog()

// 从store获取数据
const notifications = computed(() => notificationStore.notifications)
const unreadCount = computed(() => notificationStore.unreadCount)
const recentNotifications = computed(() => notificationStore.recentNotifications)

// 获取通知图标
const getNotificationIcon = (type) => {
    const iconMap = {
        'SYSTEM_NOTICE': InformationCircleOutline,
        'COURSE_REMIND': AlertCircleOutline,
        'SELECTION_SUCCESS': CheckmarkCircleOutline,
        'COURSE_CHANGE': WarningOutline
    }
    return iconMap[type] || InformationCircleOutline
}

// 获取通知颜色
const getNotificationColor = (type) => {
    const colorMap = {
        'SYSTEM_NOTICE': '#2080f0',
        'COURSE_REMIND': '#f0a020',
        'SELECTION_SUCCESS': '#18a058',
        'COURSE_CHANGE': '#d03050'
    }
    return colorMap[type] || '#2080f0'
}

// 格式化时间
const formatTime = (timeStr) => {
    if (!timeStr) return ''

    const time = new Date(timeStr)
    const now = new Date()
    const diff = now - time

    const minute = 60 * 1000
    const hour = 60 * minute
    const day = 24 * hour

    if (diff < minute) {
        return '刚刚'
    } else if (diff < hour) {
        return `${Math.floor(diff / minute)}分钟前`
    } else if (diff < day) {
        return `${Math.floor(diff / hour)}小时前`
    } else if (diff < 7 * day) {
        return `${Math.floor(diff / day)}天前`
    } else {
        return time.toLocaleDateString()
    }
}

// Popover切换时刷新数据
const handlePopoverToggle = (show) => {
    if (show) {
        notificationStore.fetchNotifications()
        notificationStore.fetchUnreadCount()
    }
}

// 点击通知项
const handleNotificationClick = async (notification) => {
    if (!notification.isRead) {
        await notificationStore.markAsRead(notification.id)
    }

    // TODO: 根据通知类型跳转到相应页面
    message.info(`查看通知: ${notification.title}`)
}

// 标记所有为已读
const handleMarkAllRead = async () => {
    await notificationStore.markAllAsRead()
    message.success('已全部标记为已读')
}

// 清空所有通知
const handleClearAll = () => {
    dialog.warning({
        title: '确认清空',
        content: '确定要清空所有通知吗？此操作不可恢复。',
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            await notificationStore.clearAll()
            message.success('已清空所有通知')
        }
    })
}

// 删除单条通知
const handleDelete = async (id) => {
    await notificationStore.deleteNotification(id)
    message.success('已删除')
}

// 查看全部通知
const handleViewAll = () => {
    // TODO: 跳转到通知列表页面
    message.info('跳转到通知列表页面')
}
</script>

<style scoped>
.notification-center {
    width: 100%;
}

.notification-header,
.notification-footer {
    padding: 8px 12px;
}

.notification-list {
    padding: 0;
}

.notification-item {
    display: flex;
    align-items: flex-start;
    gap: 12px;
    padding: 12px;
    cursor: pointer;
    transition: background-color 0.2s;
    border-bottom: 1px solid #f0f0f0;
}

.notification-item:hover {
    background-color: #f8f8f8;
}

.notification-item.unread {
    background-color: #f0f9ff;
}

.notification-item.unread:hover {
    background-color: #e6f4ff;
}

.notification-icon {
    flex-shrink: 0;
    margin-top: 2px;
}

.notification-content {
    flex: 1;
    min-width: 0;
}

.notification-title {
    display: flex;
    align-items: center;
    gap: 4px;
    margin-bottom: 4px;
}

.notification-text {
    margin: 4px 0;
    font-size: 14px;
    color: #666;
}

.notification-time {
    margin-top: 4px;
}

.notification-actions {
    flex-shrink: 0;
    opacity: 0;
    transition: opacity 0.2s;
}

.notification-item:hover .notification-actions {
    opacity: 1;
}
</style>
