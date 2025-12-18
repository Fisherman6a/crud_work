<template>
    <div class="page-container" style="padding: 20px;">

        <n-alert :type="canEdit ? 'success' : 'warning'" style="margin-bottom: 15px">
            <template #header>
                {{ roleTitle }}
            </template>
            {{ statusText }}
        </n-alert>

        <n-space style="margin-bottom: 20px" align="center">
            <span style="font-weight: bold;">当前查看学生：</span>
            <n-select v-if="userRole === 'ADMIN'" v-model:value="currentStudentId" filterable placeholder="搜索/选择学生"
                :options="studentOptions" @update:value="loadSchedule" style="width: 200px" />
            <span v-else>{{ currentStudentName }} ({{ currentStudentId }})</span>

            <n-button @click="loadSchedule">刷新课表</n-button>
        </n-space>

        <div class="schedule-grid">
            <div class="grid-header"></div>
            <div v-for="day in days" :key="day" class="grid-header">{{ day }}</div>

            <template v-for="slot in 5" :key="slot">
                <div class="grid-slot-label">第 {{ slot }} 大节</div>
                <div v-for="(day, index) in days" :key="index" class="grid-cell"
                    :class="{ 'has-course': getCourse(day, slot) }" @click="handleCellClick(day, slot)">
                    <div v-if="getCourse(day, slot)" class="course-card">
                        <strong>{{ getCourse(day, slot).courseName }}</strong>
                        <br>
                        <span style="font-size: 12px">{{ getCourse(day, slot).teacher }}</span>
                    </div>
                    <div v-else class="empty-cell-hint">
                        <span v-if="canEdit">+</span>
                    </div>
                </div>
            </template>
        </div>

        <n-modal v-model:show="showCourseSelect" preset="dialog" title="添加课程">
            <n-list hoverable clickable>
                <n-list-item v-for="course in availableCourses" :key="course.id" @click="confirmAddCourse(course)">
                    <n-thing :title="course.name" :description="course.teacherName">
                        <template #header-extra>
                            <n-tag type="success">可选</n-tag>
                        </template>
                    </n-thing>
                </n-list-item>
            </n-list>
        </n-modal>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { NAlert, NSpace, NSelect, NButton, NModal, NList, NListItem, NThing, NTag, useMessage } from 'naive-ui'
import axios from 'axios'

const message = useMessage()

// --- 状态数据 ---
const userRole = localStorage.getItem('role') || 'STUDENT' // ADMIN 或 STUDENT
const currentStudentId = ref('')
const currentStudentName = ref('我自己')
const scheduleData = ref([]) // 存储已选课程
const showCourseSelect = ref(false)
const targetSlot = ref({ day: '', slot: 0 }) // 记录当前点击的格子
const studentOptions = ref([]) // 管理员搜索学生用
const availableCourses = ref([]) // 弹窗里的备选课程

const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

// --- 权限逻辑 (按照你的要求) ---
// 模拟服务器时间配置，实际应从后端获取接口 /system/time
const systemConfig = ref({
    currentTime: new Date().getTime(),
    openStart: new Date('2025-09-01 00:00:00').getTime(),
    openEnd: new Date('2025-09-10 00:00:00').getTime()
})

const canEdit = computed(() => {
    if (userRole === 'ADMIN') return true
    // 学生：判断时间
    const now = systemConfig.value.currentTime
    return now >= systemConfig.value.openStart && now <= systemConfig.value.openEnd
})

const roleTitle = computed(() => userRole === 'ADMIN' ? '管理员模式' : '学生选课系统')
const statusText = computed(() => {
    if (userRole === 'ADMIN') return '拥有最高权限，点击任意格子即可修改/删除课程。'
    return canEdit.value ? '当前是选课开放时间，您可以点击空白格子选课。' : '当前不在选课时间内，仅供查看。'
})

// --- 方法 ---

// 获取格子里的课程
const getCourse = (day, slot) => {
    return scheduleData.value.find(c => c.day === day && c.slot === slot)
}

// 点击格子
const handleCellClick = (day, slot) => {
    if (!canEdit.value) {
        message.warning('当前无法编辑！')
        return
    }

    const existing = getCourse(day, slot)
    if (existing) {
        // 已有课程 -> 删除逻辑
        if (confirm(`确定要退选 ${existing.courseName} 吗？`)) {
            // 调用退选接口
            axios.post('http://localhost:8080/schedule/remove', { id: existing.id }).then(() => {
                message.success('退选成功'); loadSchedule();
            })
        }
    } else {
        // 空白 -> 添加逻辑
        targetSlot.value = { day, slot }
        loadAvailableCourses() // 加载可选课程列表
        showCourseSelect.value = true
    }
}

// 确认选课
const confirmAddCourse = async (course) => {
    try {
        await axios.post('http://localhost:8080/schedule/add', {
            studentId: currentStudentId.value,
            courseId: course.id,
            day: targetSlot.value.day,
            slot: targetSlot.value.slot
        })
        message.success('选课成功')
        showCourseSelect.value = false
        loadSchedule()
    } catch (e) { message.error('冲突或失败') }
}

const loadSchedule = async () => {
    // 调用后端获取某学生的课表
    if (!currentStudentId.value) return;
    const res = await axios.get(`http://localhost:8080/schedule/list?studentId=${currentStudentId.value}`)
    scheduleData.value = res.data.data
}

const loadAvailableCourses = async () => {
    // 获取所有课程供选择
    const res = await axios.get('http://localhost:8080/course/all')
    availableCourses.value = res.data.data
}

// 初始化
onMounted(() => {
    // 如果是学生，自动填入自己的ID
    if (userRole !== 'ADMIN') {
        const user = JSON.parse(localStorage.getItem('user') || '{}')
        currentStudentId.value = user.studentId
        loadSchedule()
    } else {
        // 管理员：加载学生列表供搜索
        // axios.get('/student/all').then(...)
    }
})
</script>

<style scoped>
/* 简单的 CSS Grid 布局课表 */
.schedule-grid {
    display: grid;
    grid-template-columns: 80px repeat(7, 1fr);
    gap: 2px;
    background-color: #eee;
    border: 1px solid #ddd;
}

.grid-header,
.grid-slot-label {
    background: #f5f7fa;
    padding: 10px;
    text-align: center;
    font-weight: bold;
}

.grid-cell {
    background: #fff;
    min-height: 100px;
    padding: 5px;
    cursor: pointer;
    transition: all 0.2s;
    display: flex;
    align-items: center;
    justify-content: center;
}

.grid-cell:hover {
    background: #f9f9f9;
}

.course-card {
    background-color: #e1f3d8;
    color: #18a058;
    padding: 5px;
    border-radius: 4px;
    width: 100%;
    text-align: center;
}

.empty-cell-hint {
    color: #ccc;
    font-size: 20px;
    display: none;
}

.grid-cell:hover .empty-cell-hint {
    display: block;
}
</style>
