<template>
    <div class="student-course">
        <n-grid x-gap="12" :cols="2">
            <n-gi>
                <n-card title="可选课程">
                    <n-list hoverable clickable>
                        <n-list-item v-for="course in courses" :key="course.id">
                            <template #prefix>
                                <n-tag type="success">开放</n-tag>
                            </template>
                            <n-thing :title="course.name" :description="course.teacher" />
                            <template #suffix>
                                <n-button type="primary" size="small" @click="selectCourse(course)">
                                    选课
                                </n-button>
                            </template>
                        </n-list-item>
                    </n-list>
                </n-card>

                <n-card title="实时通知 (WebSocket)" style="margin-top: 12px">
                    <n-alert v-if="lastMessage" title="收到新消息" type="info" closable>
                        {{ lastMessage }}
                    </n-alert>
                    <n-empty v-else description="暂无新通知" />
                </n-card>
            </n-gi>

            <n-gi>
                <n-card title="课程表">
                    <n-calendar v-model:value="dateValue" #="{ year, month, date }" @update:value="handleDateChange">
                        <div v-if="isCourseDay(year, month, date)" class="calendar-event">
                            {{ getCourseName(year, month, date) }}
                        </div>
                    </n-calendar>
                </n-card>
            </n-gi>
        </n-grid>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useMessage, useNotification } from 'naive-ui'
import axios from 'axios'

const message = useMessage()
const notification = useNotification()
const API_BASE = 'http://localhost:8080'

// 模拟当前登录学生
const studentId = "2021001"
const dateValue = ref(Date.now())
const lastMessage = ref('')
const socket = ref(null)

// 模拟课程数据
const courses = ref([
    { id: 1, name: '高等数学', teacher: '张教授' },
    { id: 2, name: '大学物理', teacher: '李老师' },
    { id: 3, name: '计算机网络', teacher: '王工' }
])

// 模拟已选课程日历数据 (真实场景应从后端 /student-course/calendar 获取)
const myCalendar = ref({
    '2025-12-15': '高等数学',
    '2025-12-16': '大学物理',
    '2025-12-18': '计算机网络'
})

// --- WebSocket 连接 ---
const initWebSocket = () => {
    if (typeof WebSocket === 'undefined') {
        message.error('您的浏览器不支持WebSocket')
        return
    }

    // 连接后端 WebSocket (注意 ws:// 协议)
    const wsUrl = `ws://localhost:8080/ws/${studentId}`
    socket.value = new WebSocket(wsUrl)

    socket.value.onopen = () => {
        console.log('WebSocket已连接')
    }

    socket.value.onmessage = (msg) => {
        console.log('收到消息:', msg.data)
        lastMessage.value = msg.data
        // 弹出右下角通知
        notification.info({
            content: '站内信提醒',
            meta: msg.data,
            duration: 3000
        })
    }

    socket.value.onclose = () => {
        console.log('WebSocket已关闭')
    }
}

// --- 业务方法 ---

// 选课操作
const selectCourse = async (course) => {
    try {
        const res = await axios.post(`${API_BASE}/student-course/select`, {
            studentId: studentId,
            courseName: course.name,
            phoneNumber: '13800138000' // 模拟手机号，用于接收RabbitMQ短信
        })
        message.success(res.data || '请求已发送')
    } catch (error) {
        message.error('选课请求失败')
    }
}

// 日历辅助方法
const isCourseDay = (year, month, date) => {
    const key = `${year}-${String(month).padStart(2, '0')}-${String(date).padStart(2, '0')}`
    return !!myCalendar.value[key]
}

const getCourseName = (year, month, date) => {
    const key = `${year}-${String(month).padStart(2, '0')}-${String(date).padStart(2, '0')}`
    return myCalendar.value[key]
}

const handleDateChange = (val) => {
    message.info(`您选择了日期: ${new Date(val).toLocaleDateString()}`)
}

// 生命周期
onMounted(() => {
    initWebSocket()
})

onUnmounted(() => {
    if (socket.value) {
        socket.value.close()
    }
})
</script>

<style scoped>
.student-course {
    padding: 20px;
}

.calendar-event {
    font-size: 12px;
    color: white;
    background-color: #2080f0;
    border-radius: 4px;
    padding: 2px 4px;
    margin-top: 4px;
}
</style>
