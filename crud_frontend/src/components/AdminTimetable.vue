<template>
    <div class="student-schedule-page">
        <n-card title="学生选课管理" :bordered="false">
            <template #header-extra>
                <n-space>
                    <n-select
                        v-model:value="targetStudentId"
                        filterable
                        placeholder="搜索学生姓名或学号"
                        :options="studentOptions"
                        :loading="loadingStudent"
                        @search="handleSearchStudent"
                        @update:value="handleStudentChange"
                        style="width: 300px"
                        remote
                        clearable
                    />
                    <n-button @click="loadData" :disabled="!targetStudentId" :loading="loading">
                        <template #icon>
                            <n-icon :component="RefreshOutline" />
                        </template>
                        刷新
                    </n-button>
                </n-space>
            </template>

            <n-alert v-if="!targetStudentId" type="info" style="margin-bottom: 20px;">
                请先选择一名学生，然后点击课表格子为其添加或删除课程
            </n-alert>

            <!-- 课表网格 -->
            <div class="timetable-wrapper" v-if="targetStudentId">
                <table class="timetable">
                    <thead>
                        <tr>
                            <th class="header-cell">节次/周数</th>
                            <th v-for="day in weekDays" :key="day.value" class="header-cell">
                                {{ day.label }}
                            </th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr v-for="section in sections" :key="section">
                            <td class="section-label">第{{ section }}节</td>
                            <td v-for="day in weekDays" :key="day.value"
                                class="schedule-cell"
                                :class="{ 'has-course': getCellSchedule(day.value, section) }"
                                @click="handleCellClick(day.value, section)">
                                <!-- 已选课程块 -->
                                <div v-if="getCellSchedule(day.value, section)"
                                     class="course-block"
                                     :class="{
                                         'is-start': isBlockStart(day.value, section),
                                         'is-middle': isBlockMiddle(day.value, section),
                                         'is-end': isBlockEnd(day.value, section)
                                     }"
                                     :style="getBlockStyle(day.value, section)">
                                    <template v-if="isBlockStart(day.value, section)">
                                        <div class="course-name">{{ getCellSchedule(day.value, section).courseName }}</div>
                                        <div class="course-info">
                                            <div class="teacher-name">{{ getCellSchedule(day.value, section).teacherName }}</div>
                                            <div class="course-detail">{{ getCellSchedule(day.value, section).location }}</div>
                                        </div>
                                    </template>
                                </div>
                                <!-- 空白格子显示可选课程数量 -->
                                <div v-else class="empty-cell">
                                    <div class="available-count" v-if="getAvailableCourses(day.value, section).length > 0">
                                        {{ getAvailableCourses(day.value, section).length }}门可选
                                    </div>
                                    <n-icon :component="AddCircleOutline" class="add-icon" />
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </n-card>

        <!-- 添加课程弹窗 -->
        <n-modal v-model:show="showAddModal" preset="card" title="为学生添加课程" style="width: 600px">
            <n-space vertical>
                <n-text>时间：{{ getWeekDayLabel(selectedCell.weekDay) }} 第{{ selectedCell.section }}节</n-text>
                <n-text type="info">可选课程（{{ availableCoursesForModal.length }}门）：</n-text>

                <n-empty v-if="availableCoursesForModal.length === 0" description="该时间段没有可选课程" />

                <n-list v-else hoverable clickable>
                    <n-list-item
                        v-for="course in availableCoursesForModal"
                        :key="course.id"
                        @click="handleSelectCourse(course)"
                        style="cursor: pointer">
                        <n-thing>
                            <template #header>
                                <n-text strong>{{ course.courseName }}</n-text>
                            </template>
                            <template #description>
                                <n-space>
                                    <n-tag size="small" type="info">{{ course.teacherName }}</n-tag>
                                    <n-tag size="small">{{ course.location }}</n-tag>
                                    <n-tag size="small" type="success">
                                        第{{ course.sectionStart }}-{{ course.sectionEnd }}节
                                    </n-tag>
                                    <n-tag size="small" type="warning">
                                        {{ course.currentCount || 0 }}/{{ course.maxCapacity }}人
                                    </n-tag>
                                </n-space>
                            </template>
                        </n-thing>
                    </n-list-item>
                </n-list>
            </n-space>
        </n-modal>

        <!-- 课程详情弹窗 -->
        <n-modal v-model:show="showDetailModal" preset="card" title="课程详情" style="width: 500px">
            <n-descriptions v-if="selectedSchedule" :column="2" bordered>
                <n-descriptions-item label="课程名称">{{ selectedSchedule.courseName }}</n-descriptions-item>
                <n-descriptions-item label="授课教师">{{ selectedSchedule.teacherName }}</n-descriptions-item>
                <n-descriptions-item label="上课时间">
                    {{ getWeekDayLabel(selectedSchedule.weekDay) }} 第{{ selectedSchedule.sectionStart }}-{{ selectedSchedule.sectionEnd }}节
                </n-descriptions-item>
                <n-descriptions-item label="上课地点">{{ selectedSchedule.location }}</n-descriptions-item>
            </n-descriptions>

            <template #footer>
                <n-space justify="end">
                    <n-button @click="showDetailModal = false">关闭</n-button>
                    <n-button type="error" @click="handleDropCourse" :loading="dropping">
                        退选此课
                    </n-button>
                </n-space>
            </template>
        </n-modal>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
    NCard, NButton, NSpace, NIcon, NModal, NSelect, NAlert, NText, NList, NListItem,
    NThing, NTag, NEmpty, NDescriptions, NDescriptionsItem, useMessage, useDialog
} from 'naive-ui'
import { RefreshOutline, AddCircleOutline } from '@vicons/ionicons5'
import axios from 'axios'

const API_BASE = 'http://localhost:8080'
const message = useMessage()
const dialog = useDialog()

// 状态
const targetStudentId = ref(null)
const studentOptions = ref([])
const loadingStudent = ref(false)
const loading = ref(false)
const studentSchedules = ref([]) // 学生已选课程
const allSchedules = ref([]) // 所有排课
const showAddModal = ref(false)
const showDetailModal = ref(false)
const selectedCell = ref({ weekDay: null, section: null })
const selectedSchedule = ref(null)
const dropping = ref(false)

// 星期配置
const weekDays = [
    { label: '星期一', value: 1 },
    { label: '星期二', value: 2 },
    { label: '星期三', value: 3 },
    { label: '星期四', value: 4 },
    { label: '星期五', value: 5 },
    { label: '星期六', value: 6 },
    { label: '星期日', value: 7 }
]

// 节次配置（1-13节）
const sections = Array.from({ length: 13 }, (_, i) => i + 1)

// 搜索学生
const handleSearchStudent = async (query) => {
    if (!query) return
    loadingStudent.value = true
    try {
        const res = await axios.get(`${API_BASE}/student/page`, {
            params: { pageSize: 20, name: query }
        })
        if (res.data && res.data.records) {
            studentOptions.value = res.data.records.map(s => ({
                label: `${s.name} (${s.studentNumber})`,
                value: s.studentNumber
            }))
        }
    } catch (error) {
        console.error('搜索学生失败:', error)
    } finally {
        loadingStudent.value = false
    }
}

// 学生改变时加载数据
const handleStudentChange = () => {
    if (targetStudentId.value) {
        loadData()
    }
}

// 加载数据
const loadData = async () => {
    if (!targetStudentId.value) return

    loading.value = true
    try {
        // 加载学生已选课程（后端接口是 /student-course/admin/list/{studentNumber}）
        const studentRes = await axios.get(`${API_BASE}/student-course/admin/list/${targetStudentId.value}`)
        studentSchedules.value = studentRes.data || []
        console.log('学生已选课程:', studentSchedules.value)

        // 加载所有排课
        const allRes = await axios.get(`${API_BASE}/schedule/listDetailed`)
        allSchedules.value = allRes.data || []
        console.log('所有排课:', allSchedules.value)
    } catch (error) {
        console.error('加载数据失败:', error)
        message.error('加载数据失败: ' + (error.response?.data || error.message))
    } finally {
        loading.value = false
    }
}

// 获取星期标签
const getWeekDayLabel = (value) => {
    const day = weekDays.find(d => d.value === value)
    return day ? day.label : ''
}

// 获取某个单元格的学生已选课程
const getCellSchedule = (weekDay, section) => {
    return studentSchedules.value.find(s =>
        s.weekDay === weekDay &&
        section >= s.sectionStart &&
        section <= s.sectionEnd
    )
}

// 获取某个单元格的可选课程列表
const getAvailableCourses = (weekDay, section) => {
    return allSchedules.value.filter(s =>
        s.weekDay === weekDay &&
        section >= s.sectionStart &&
        section <= s.sectionEnd &&
        !studentSchedules.value.find(selected => selected.courseId === s.courseId)
    )
}

// 弹窗中显示的可选课程
const availableCoursesForModal = computed(() => {
    if (!selectedCell.value.weekDay || !selectedCell.value.section) return []
    return getAvailableCourses(selectedCell.value.weekDay, selectedCell.value.section)
})

// 判断是否是课程块的开始节
const isBlockStart = (weekDay, section) => {
    const schedule = getCellSchedule(weekDay, section)
    return schedule && schedule.sectionStart === section
}

// 判断是否是课程块的中间节
const isBlockMiddle = (weekDay, section) => {
    const schedule = getCellSchedule(weekDay, section)
    return schedule && section > schedule.sectionStart && section < schedule.sectionEnd
}

// 判断是否是课程块的结束节
const isBlockEnd = (weekDay, section) => {
    const schedule = getCellSchedule(weekDay, section)
    return schedule && schedule.sectionEnd === section
}

// 获取课程块样式
const getBlockStyle = (weekDay, section) => {
    const schedule = getCellSchedule(weekDay, section)
    if (!schedule) return {}

    const spanSections = schedule.sectionEnd - schedule.sectionStart + 1

    // 颜色
    const colors = [
        '#b3d8ff', '#ffd8b3', '#d8ffb3', '#ffb3d8',
        '#d8b3ff', '#b3fff0', '#fff0b3', '#ffb3b3'
    ]
    const colorIndex = schedule.courseId % colors.length
    const bgColor = colors[colorIndex]

    // 如果是起始节，计算跨越高度（60px单元格高度 + 1px边框）
    if (isBlockStart(weekDay, section)) {
        const cellHeight = 60 // 单元格高度
        const borderWidth = 1 // 边框宽度
        const totalHeight = spanSections * cellHeight + (spanSections - 1) * borderWidth

        return {
            backgroundColor: bgColor,
            height: `${totalHeight}px`,
            display: 'flex'
        }
    }

    // 中间和结束节不显示
    return {
        display: 'none'
    }
}

// 点击单元格
const handleCellClick = (weekDay, section) => {
    if (!targetStudentId.value) {
        message.warning('请先选择一名学生')
        return
    }

    const existing = getCellSchedule(weekDay, section)

    if (existing) {
        // 已选课程，显示详情
        selectedSchedule.value = existing
        showDetailModal.value = true
    } else {
        // 空白格子，显示可选课程
        selectedCell.value = { weekDay, section }
        showAddModal.value = true
    }
}

// 选择课程并添加
const handleSelectCourse = async (course) => {
    try {
        // 后端接口是 /student-course/admin/add
        const res = await axios.post(`${API_BASE}/student-course/admin/add`, {
            studentNumber: targetStudentId.value,
            scheduleId: course.id
        })

        // 后端返回 ResponseEntity<String>，成功时返回 "Success"
        if (res.status === 200) {
            message.success('选课成功')
            showAddModal.value = false
            await loadData()
        } else {
            message.error('选课失败')
        }
    } catch (error) {
        console.error('选课失败:', error)
        message.error('选课失败: ' + (error.response?.data || error.message))
    }
}

// 退选课程
const handleDropCourse = () => {
    dialog.warning({
        title: '确认退选',
        content: `确定要退选《${selectedSchedule.value.courseName}》吗？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            dropping.value = true
            try {
                // 后端接口是 /student-course/admin/remove
                const res = await axios.post(`${API_BASE}/student-course/admin/remove`, {
                    studentNumber: targetStudentId.value,
                    scheduleId: selectedSchedule.value.scheduleId
                })

                if (res.status === 200) {
                    message.success('退选成功')
                    showDetailModal.value = false
                    await loadData()
                } else {
                    message.error('退选失败')
                }
            } catch (error) {
                console.error('退选失败:', error)
                message.error('退选失败: ' + (error.response?.data || error.message))
            } finally {
                dropping.value = false
            }
        }
    })
}

// 初始化
onMounted(() => {
    handleSearchStudent(' ')
})
</script>

<style scoped>
.student-schedule-page {
    padding: 20px;
}

.timetable-wrapper {
    overflow-x: auto;
    margin-top: 20px;
}

.timetable {
    width: 100%;
    border-collapse: collapse;
    background: white;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.header-cell {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 12px;
    text-align: center;
    font-weight: bold;
    border: 1px solid #ddd;
}

.section-label {
    background: #f5f7fa;
    padding: 10px;
    text-align: center;
    font-weight: bold;
    border: 1px solid #ddd;
    width: 100px;
}

.schedule-cell {
    border: 1px solid #ddd;
    padding: 0;
    height: 60px;
    vertical-align: top;
    cursor: pointer;
    transition: background-color 0.2s;
    position: relative;
    overflow: visible; /* 允许内容溢出 */
}

.schedule-cell:hover {
    background-color: #f9f9f9;
}

.schedule-cell.has-course:hover {
    background-color: #fff9e6;
}

.course-block {
    width: calc(100% - 2px); /* 减去边框 */
    padding: 8px;
    border-radius: 4px;
    border: 1px solid rgba(0, 0, 0, 0.1);
    box-sizing: border-box;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    text-align: center;
    font-size: 12px;
    line-height: 1.4;
    position: absolute;
    top: 1px; /* 从边框内侧开始 */
    left: 1px;
    z-index: 10; /* 确保在上层 */
    pointer-events: none; /* 让点击事件穿透到单元格 */
}

.course-name {
    font-weight: bold;
    font-size: 14px;
    margin-bottom: 4px;
    color: #333;
}

.course-info {
    margin: 4px 0;
}

.teacher-name,
.course-detail {
    font-size: 11px;
    color: #666;
    margin: 2px 0;
}

.empty-cell {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    color: #ddd;
    position: relative;
}

.available-count {
    position: absolute;
    top: 5px;
    right: 5px;
    font-size: 10px;
    color: #18a058;
    background: #e6f7e6;
    padding: 2px 6px;
    border-radius: 10px;
    font-weight: bold;
}

.add-icon {
    font-size: 24px;
    opacity: 0;
    transition: opacity 0.2s;
}

.schedule-cell:hover .add-icon {
    opacity: 1;
    color: #18a058;
}
</style>
