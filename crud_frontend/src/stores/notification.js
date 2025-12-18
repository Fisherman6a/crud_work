import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'
import wsClient from '@/utils/websocket'

const API_BASE = 'http://localhost:8080'

export const useNotificationStore = defineStore('notification', () => {
    // 状态
    const notifications = ref([])
    const unreadCount = ref(0)
    const isConnected = ref(false)
    const userId = ref(null)

    // 计算属性
    const unreadNotifications = computed(() => {
        return notifications.value.filter(n => !n.isRead)
    })

    const recentNotifications = computed(() => {
        return notifications.value.slice(0, 10)
    })

    // 初始化WebSocket连接
    const initWebSocket = (uid) => {
        if (!uid) {
            console.error('UserId is required for WebSocket')
            return
        }

        userId.value = uid

        // 连接WebSocket
        wsClient.connect(uid)

        // 监听消息
        wsClient.onMessage(handleWebSocketMessage)
    }

    // 处理WebSocket消息
    const handleWebSocketMessage = (event) => {
        if (event.type === 'connect') {
            isConnected.value = true
        } else if (event.type === 'close') {
            isConnected.value = false
        } else if (event.type === 'message') {
            // 收到新通知
            const message = event.data

            // 添加到通知列表（如果是通知消息）
            if (message.title || message.content) {
                const notification = {
                    id: Date.now(),
                    title: message.title || '新消息',
                    content: message.content || message,
                    type: message.type || 'SYSTEM_NOTICE',
                    isRead: false,
                    createTime: new Date().toISOString()
                }

                notifications.value.unshift(notification)
                unreadCount.value++

                // 触发浏览器通知（如果允许）
                showBrowserNotification(notification)
            }

            // 刷新通知列表
            fetchNotifications()
        }
    }

    // 浏览器通知
    const showBrowserNotification = (notification) => {
        if ('Notification' in window && Notification.permission === 'granted') {
            new Notification(notification.title, {
                body: notification.content,
                icon: '/favicon.ico',
                tag: notification.id
            })
        }
    }

    // 请求浏览器通知权限
    const requestNotificationPermission = async () => {
        if ('Notification' in window && Notification.permission === 'default') {
            await Notification.requestPermission()
        }
    }

    // 从服务器获取通知列表
    const fetchNotifications = async (limit = 50) => {
        if (!userId.value) return

        try {
            const res = await axios.get(`${API_BASE}/notification/list/${userId.value}`, {
                params: { limit }
            })

            if (res.data.code === 200) {
                notifications.value = res.data.data || []
            }
        } catch (error) {
            console.error('Failed to fetch notifications:', error)
        }
    }

    // 获取未读数量
    const fetchUnreadCount = async () => {
        if (!userId.value) return

        try {
            const res = await axios.get(`${API_BASE}/notification/unread-count/${userId.value}`)
            if (res.data.code === 200) {
                unreadCount.value = res.data.data || 0
            }
        } catch (error) {
            console.error('Failed to fetch unread count:', error)
        }
    }

    // 标记为已读
    const markAsRead = async (notificationId) => {
        try {
            const res = await axios.put(`${API_BASE}/notification/read/${notificationId}`)

            if (res.data.code === 200) {
                // 更新本地状态
                const notification = notifications.value.find(n => n.id === notificationId)
                if (notification && !notification.isRead) {
                    notification.isRead = true
                    unreadCount.value = Math.max(0, unreadCount.value - 1)
                }
            }

            return res.data.code === 200
        } catch (error) {
            console.error('Failed to mark as read:', error)
            return false
        }
    }

    // 标记所有为已读
    const markAllAsRead = async () => {
        const unread = unreadNotifications.value
        const promises = unread.map(n => markAsRead(n.id))
        await Promise.all(promises)
    }

    // 删除通知
    const deleteNotification = async (notificationId) => {
        try {
            const res = await axios.delete(`${API_BASE}/notification/delete/${notificationId}`)

            if (res.data.code === 200) {
                // 更新本地状态
                const index = notifications.value.findIndex(n => n.id === notificationId)
                if (index > -1) {
                    const notification = notifications.value[index]
                    if (!notification.isRead) {
                        unreadCount.value = Math.max(0, unreadCount.value - 1)
                    }
                    notifications.value.splice(index, 1)
                }
            }

            return res.data.code === 200
        } catch (error) {
            console.error('Failed to delete notification:', error)
            return false
        }
    }

    // 清空所有通知
    const clearAll = async () => {
        const promises = notifications.value.map(n => deleteNotification(n.id))
        await Promise.all(promises)
    }

    // 断开WebSocket
    const disconnect = () => {
        wsClient.disconnect()
        isConnected.value = false
    }

    return {
        // State
        notifications,
        unreadCount,
        isConnected,
        userId,

        // Computed
        unreadNotifications,
        recentNotifications,

        // Actions
        initWebSocket,
        fetchNotifications,
        fetchUnreadCount,
        markAsRead,
        markAllAsRead,
        deleteNotification,
        clearAll,
        disconnect,
        requestNotificationPermission
    }
})
