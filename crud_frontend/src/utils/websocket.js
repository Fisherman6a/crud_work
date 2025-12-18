// WebSocket连接管理工具
class WebSocketClient {
    constructor() {
        this.socket = null
        this.userId = null
        this.reconnectAttempts = 0
        this.maxReconnectAttempts = 5
        this.reconnectDelay = 3000
        this.heartbeatInterval = null
        this.heartbeatTimeout = 30000
        this.messageHandlers = []
        this.isManualClose = false
    }

    // 连接WebSocket
    connect(userId) {
        if (!userId) {
            console.error('WebSocket: userId is required')
            return
        }

        this.userId = userId
        this.isManualClose = false

        const wsUrl = `ws://localhost:8080/ws/${userId}`

        try {
            this.socket = new WebSocket(wsUrl)

            this.socket.onopen = () => {
                console.log('WebSocket connected')
                this.reconnectAttempts = 0
                this.startHeartbeat()
                this.notifyHandlers({ type: 'connect', data: { userId } })
            }

            this.socket.onmessage = (event) => {
                console.log('WebSocket message:', event.data)
                try {
                    const message = JSON.parse(event.data)
                    this.notifyHandlers({ type: 'message', data: message })
                } catch (e) {
                    // 如果不是JSON，直接传递文本
                    this.notifyHandlers({ type: 'message', data: { content: event.data } })
                }
            }

            this.socket.onerror = (error) => {
                console.error('WebSocket error:', error)
                this.notifyHandlers({ type: 'error', data: error })
            }

            this.socket.onclose = () => {
                console.log('WebSocket closed')
                this.stopHeartbeat()
                this.notifyHandlers({ type: 'close', data: {} })

                // 如果不是手动关闭，则尝试重连
                if (!this.isManualClose && this.reconnectAttempts < this.maxReconnectAttempts) {
                    this.reconnect()
                }
            }
        } catch (error) {
            console.error('WebSocket connection failed:', error)
        }
    }

    // 重新连接
    reconnect() {
        this.reconnectAttempts++
        console.log(`WebSocket reconnecting... Attempt ${this.reconnectAttempts}/${this.maxReconnectAttempts}`)

        setTimeout(() => {
            this.connect(this.userId)
        }, this.reconnectDelay)
    }

    // 发送消息
    send(message) {
        if (this.socket && this.socket.readyState === WebSocket.OPEN) {
            const data = typeof message === 'string' ? message : JSON.stringify(message)
            this.socket.send(data)
        } else {
            console.error('WebSocket is not connected')
        }
    }

    // 断开连接
    disconnect() {
        this.isManualClose = true
        this.stopHeartbeat()

        if (this.socket) {
            this.socket.close()
            this.socket = null
        }
    }

    // 启动心跳检测
    startHeartbeat() {
        this.stopHeartbeat()

        this.heartbeatInterval = setInterval(() => {
            if (this.socket && this.socket.readyState === WebSocket.OPEN) {
                this.send({ type: 'ping' })
            }
        }, this.heartbeatTimeout)
    }

    // 停止心跳检测
    stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval)
            this.heartbeatInterval = null
        }
    }

    // 添加消息处理器
    onMessage(handler) {
        if (typeof handler === 'function') {
            this.messageHandlers.push(handler)
        }
    }

    // 移除消息处理器
    offMessage(handler) {
        const index = this.messageHandlers.indexOf(handler)
        if (index > -1) {
            this.messageHandlers.splice(index, 1)
        }
    }

    // 通知所有处理器
    notifyHandlers(event) {
        this.messageHandlers.forEach(handler => {
            try {
                handler(event)
            } catch (error) {
                console.error('Message handler error:', error)
            }
        })
    }

    // 获取连接状态
    isConnected() {
        return this.socket && this.socket.readyState === WebSocket.OPEN
    }
}

// 创建单例实例
const wsClient = new WebSocketClient()

export default wsClient
