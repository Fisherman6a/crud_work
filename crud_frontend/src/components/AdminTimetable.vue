<template>
    <div style="padding: 20px;">
        <n-alert type="info" style="margin-bottom: 15px;">
            管理员排课模式：请先在左上角选择一名学生，然后点击格子为其添加或删除课程。
        </n-alert>

        <n-space style="margin-bottom: 20px;" align="center">
            <span>正在编辑学生：</span>
            <n-select v-model:value="targetStudentId" filterable placeholder="输入姓名或学号搜索" :options="studentOptions"
                :loading="loadingStudent" @search="handleSearchStudent" @update:value="loadSchedule"
                style="width: 250px" remote />
            <n-button @click="loadSchedule" :disabled="!targetStudentId">刷新课表</n-button>
        </n-space>

        <div class="timetable">
            <div class="header"></div>
            <div v-for="d in days" :key="d" class="header">{{ d }}</div>

            <template v-for="slot in 5" :key="slot">
                <div class="label">第{{ slot }}大节</div>
                <div v-for="(day, dayIdx) in days" :key="dayIdx" class="cell" @click="handleCellClick(day, slot)">
                    <div v-if="getCourse(day, slot)" class="course-block">
                        <strong>{{ getCourse(day, slot).courseName }}</strong>
                        <div>{{ getCourse(day, slot).teacherName }}</div>
                    </div>
                    <div v-else class="add-hint">+</div>
                </div>
            </template>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { NAlert, NSpace, NSelect, NButton, useMessage } from 'naive-ui'
import axios from 'axios'

const message = useMessage()
const days = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
const targetStudentId = ref(null)
const studentOptions = ref([])
const loadingStudent = ref(false)
const scheduleData = ref([])

// 搜索学生
const handleSearchStudent = async (query) => {
    if (!query) return
    loadingStudent.value = true
    try {
        const res = await axios.get(`http://localhost:8080/student/page?pageSize=20&name=${query}`)
        if (res.data && res.data.records) {
            studentOptions.value = res.data.records.map(s => ({
                label: `${s.name} (${s.studentNumber})`,
                value: s.studentNumber
            }))
        }
    } finally { loadingStudent.value = false }
}

const loadSchedule = async () => {
    if (!targetStudentId.value) return
    const res = await axios.get(`http://localhost:8080/schedule/list?studentId=${targetStudentId.value}`)
    scheduleData.value = res.data || []
}

const getCourse = (day, slot) => {
    return scheduleData.value.find(c => c.day === day && c.slot === slot)
}

const handleCellClick = (day, slot) => {
    if (!targetStudentId.value) return message.warning('请先选择一个学生')
    const existing = getCourse(day, slot)
    if (existing) {
        if (confirm('要删除这节课吗？')) {
            // 这里调用删除接口
            message.info('触发删除逻辑')
        }
    } else {
        message.info(`请在弹窗中选择课程加入 ${day} 第${slot}节`)
    }
}

onMounted(() => {
    handleSearchStudent(' ')
})
</script>

<style scoped>
.timetable {
    display: grid;
    grid-template-columns: 60px repeat(7, 1fr);
    border: 1px solid #ddd;
}

.header,
.label {
    padding: 10px;
    background: #f5f5f5;
    text-align: center;
    border-bottom: 1px solid #ddd;
    border-right: 1px solid #ddd;
    font-weight: bold;
}

.cell {
    height: 100px;
    border-right: 1px solid #eee;
    border-bottom: 1px solid #eee;
    position: relative;
    cursor: pointer;
}

.cell:hover {
    background: #fafafa;
}

/* 修复后的样式 */
.add-hint {
    display: none;
    position: absolute;
    width: 100%;
    text-align: center;
    top: 50%;
    /* 居中 */
    transform: translateY(-50%);
    /* 修正垂直居中 */
    font-size: 24px;
    color: #ccc;
}

.cell:hover .add-hint {
    display: block;
}

.course-block {
    background: #e6f7ff;
    color: #1890ff;
    height: 100%;
    padding: 5px;
    font-size: 12px;
    overflow: hidden;
}
</style>
