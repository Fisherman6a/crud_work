<template>
    <div class="schedule-page">
        <n-card title="📅 排课管理" :bordered="false">
            <template #header-extra>
                <n-space>
                    <n-button type="primary" @click="showAddModal = true">
                        <template #icon>
                            <n-icon :component="AddOutline" />
                        </template>
                        新增排课
                    </n-button>
                    <n-button @click="loadSchedules">
                        <template #icon>
                            <n-icon :component="RefreshOutline" />
                        </template>
                        刷新
                    </n-button>
                </n-space>
            </template>

            <!-- 课表网格 -->
            <div class="timetable-wrapper">
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
                                @click="handleCellClick(day.value, section)">
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
                                            <div class="course-code">({{ getCellSchedule(day.value, section).courseId }})</div>
                                            <div class="teacher-name">({{ getCellSchedule(day.value, section).teacherName }})</div>
                                        </div>
                                        <div class="course-detail">
                                            ({{ getCellSchedule(day.value, section).sectionStart }}-{{ getCellSchedule(day.value, section).sectionEnd }},{{ getCellSchedule(day.value, section).location }})
                                        </div>
                                    </template>
                                </div>
                                <div v-else class="empty-cell">
                                    <n-icon :component="AddCircleOutline" class="add-icon" />
                                </div>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </n-card>

        <!-- 新增/编辑排课弹窗 -->
        <n-modal v-model:show="showAddModal" preset="card" title="新增排课" style="width: 700px">
            <n-form :model="scheduleForm" label-placement="left" label-width="100px">
                <n-form-item label="排课ID" :validation-status="idValidationStatus" :feedback="idValidationMessage">
                    <n-input-number
                        v-model:value="scheduleForm.scheduleId"
                        :min="1"
                        :max="99999999"
                        placeholder="自动填充最大ID+1"
                        style="width: 100%"
                        @blur="checkIdDuplicate"
                    />
                    <n-text depth="3" style="margin-top: 8px; display: block; font-size: 12px">
                        默认为当前最大ID+1，可手动修改。保存前会自动检查是否重复。
                    </n-text>
                </n-form-item>

                <n-form-item label="选择课程">
                    <n-select
                        v-model:value="scheduleForm.courseId"
                        :options="courseOptions"
                        placeholder="请选择课程"
                        filterable
                        @update:value="onCourseChange"
                    />
                </n-form-item>

                <n-form-item label="选择教师">
                    <n-select
                        v-model:value="scheduleForm.teacherId"
                        :options="availableTeachers"
                        placeholder="请选择任课教师"
                        filterable
                        :disabled="!scheduleForm.courseId"
                    />
                </n-form-item>

                <n-form-item label="星期">
                    <n-select
                        v-model:value="scheduleForm.weekDay"
                        :options="weekDays"
                        placeholder="请选择星期"
                    />
                </n-form-item>

                <n-form-item label="节次范围">
                    <n-space>
                        <n-input-number
                            v-model:value="scheduleForm.sectionStart"
                            :min="1"
                            :max="13"
                            placeholder="开始节"
                            style="width: 120px"
                        />
                        <span>至</span>
                        <n-input-number
                            v-model:value="scheduleForm.sectionEnd"
                            :min="scheduleForm.sectionStart || 1"
                            :max="13"
                            placeholder="结束节"
                            style="width: 120px"
                        />
                    </n-space>
                    <n-text depth="3" style="margin-top: 8px; display: block; font-size: 12px">
                        连续多节课请设置范围，例如：1-2节、3-4节、1-3节（三节连上）
                    </n-text>
                </n-form-item>

                <n-form-item label="上课地点">
                    <n-input
                        v-model:value="scheduleForm.location"
                        placeholder="例如：1-16,临港-教208"
                    />
                </n-form-item>

                <n-form-item label="学期">
                    <n-input
                        v-model:value="scheduleForm.semester"
                        placeholder="例如：2025-1"
                    />
                </n-form-item>

                <n-form-item label="最大容量">
                    <n-input-number
                        v-model:value="scheduleForm.maxCapacity"
                        :min="1"
                        placeholder="最大选课人数"
                        style="width: 100%"
                    />
                </n-form-item>
            </n-form>

            <template #footer>
                <n-space justify="end">
                    <n-button @click="showAddModal = false">取消</n-button>
                    <n-button type="primary" @click="handleSaveSchedule" :loading="saving">
                        保存
                    </n-button>
                </n-space>
            </template>
        </n-modal>

        <!-- 查看/编辑课程详情弹窗 -->
        <n-modal v-model:show="showDetailModal" preset="card" title="课程详情" style="width: 600px">
            <n-descriptions v-if="selectedSchedule" :column="2" bordered>
                <n-descriptions-item label="课程名称">{{ selectedSchedule.courseName }}</n-descriptions-item>
                <n-descriptions-item label="课程ID">{{ selectedSchedule.courseId }}</n-descriptions-item>
                <n-descriptions-item label="授课教师">{{ selectedSchedule.teacherName }}</n-descriptions-item>
                <n-descriptions-item label="教师ID">{{ selectedSchedule.teacherId }}</n-descriptions-item>
                <n-descriptions-item label="上课时间">
                    {{ getWeekDayLabel(selectedSchedule.weekDay) }} 第{{ selectedSchedule.sectionStart }}-{{ selectedSchedule.sectionEnd }}节
                </n-descriptions-item>
                <n-descriptions-item label="上课地点">{{ selectedSchedule.location }}</n-descriptions-item>
                <n-descriptions-item label="学期">{{ selectedSchedule.semester }}</n-descriptions-item>
                <n-descriptions-item label="容量">
                    {{ selectedSchedule.currentCount || 0 }}/{{ selectedSchedule.maxCapacity }}
                </n-descriptions-item>
            </n-descriptions>

            <template #footer>
                <n-space justify="end">
                    <n-button @click="showDetailModal = false">关闭</n-button>
                    <n-button type="error" @click="handleDeleteSchedule" :loading="deleting">
                        删除排课
                    </n-button>
                </n-space>
            </template>
        </n-modal>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
    NCard, NButton, NSpace, NIcon, NModal, NForm, NFormItem, NSelect,
    NInput, NInputNumber, NText, NDescriptions, NDescriptionsItem,
    useMessage, useDialog
} from 'naive-ui'
import {
    AddOutline, RefreshOutline, AddCircleOutline
} from '@vicons/ionicons5'
import axios from 'axios'

const API_BASE = 'http://localhost:8080'
const message = useMessage()
const dialog = useDialog()

// 状态
const schedules = ref([])
const courses = ref([])
const teachers = ref([])
const showAddModal = ref(false)
const showDetailModal = ref(false)
const saving = ref(false)
const deleting = ref(false)
const selectedSchedule = ref(null)

// ID验证状态
const idValidationStatus = ref(undefined)
const idValidationMessage = ref('')

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

// 节次配置（1-13节，支持最多13节课）
const sections = Array.from({ length: 13 }, (_, i) => i + 1)

// 表单数据
const scheduleForm = ref({
    scheduleId: null,  // 新增：排课ID
    courseId: null,
    teacherId: null,
    weekDay: null,
    sectionStart: null,
    sectionEnd: null,
    location: '',
    semester: '2025-1',
    maxCapacity: 50,
    currentCount: 0
})

// 课程选项
const courseOptions = computed(() => {
    return courses.value.map(c => ({
        label: `${c.name} (ID: ${c.id})`,
        value: c.id
    }))
})

// 可选教师（根据选中的课程动态过滤）
const availableTeachers = computed(() => {
    if (!scheduleForm.value.courseId) return []

    const selectedCourse = courses.value.find(c => c.id === scheduleForm.value.courseId)
    if (!selectedCourse || !selectedCourse.teachers) return []

    return selectedCourse.teachers.map(t => ({
        label: `${t.name} (${t.title || '教师'})`,
        value: t.id
    }))
})

// 获取星期标签
const getWeekDayLabel = (value) => {
    const day = weekDays.find(d => d.value === value)
    return day ? day.label : ''
}

// 获取某个单元格的排课信息
const getCellSchedule = (weekDay, section) => {
    return schedules.value.find(s =>
        s.weekDay === weekDay &&
        section >= s.sectionStart &&
        section <= s.sectionEnd
    )
}

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

    // 计算颜色（根据课程ID生成不同颜色）
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
    const existing = getCellSchedule(weekDay, section)

    console.log('点击单元格:', { weekDay, section, existing, coursesCount: courses.value.length })

    if (existing) {
        // 已有排课，显示详情
        selectedSchedule.value = existing
        showDetailModal.value = true
    } else {
        // 空白格子，新增排课
        scheduleForm.value = {
            scheduleId: getNextScheduleId(),  // 自动填充最大ID+1
            courseId: null,
            teacherId: null,
            weekDay: weekDay,
            sectionStart: section,
            sectionEnd: section,
            location: '',
            semester: '2025-1',
            maxCapacity: 50,
            currentCount: 0
        }
        idValidationStatus.value = undefined
        idValidationMessage.value = ''
        showAddModal.value = true
        console.log('打开新增排课弹窗，默认ID:', scheduleForm.value.scheduleId)
    }
}

// 课程改变时，重置教师选择
const onCourseChange = () => {
    scheduleForm.value.teacherId = null
}

// 加载所有排课
const loadSchedules = async () => {
    try {
        const res = await axios.get(`${API_BASE}/schedule/listDetailed`)
        schedules.value = res.data
    } catch (error) {
        message.error('加载排课失败')
    }
}

// 加载所有课程（带教师信息）
const loadCourses = async () => {
    try {
        const res = await axios.get(`${API_BASE}/course/page`, {
            params: { pageNum: 1, pageSize: 1000 }
        })
        courses.value = res.data.records || []
        console.log('加载课程成功:', courses.value.length, '门课程', courses.value)
    } catch (error) {
        console.error('加载课程失败:', error)
        message.error('加载课程失败')
    }
}

// 加载所有教师
const loadTeachers = async () => {
    try {
        const res = await axios.get(`${API_BASE}/teacher/list`)
        teachers.value = res.data
    } catch (error) {
        message.error('加载教师失败')
    }
}

// 获取当前最大ID+1
const getNextScheduleId = () => {
    if (schedules.value.length === 0) {
        return 10001
    }
    const maxId = Math.max(...schedules.value.map(s => s.id || 0))
    return maxId + 1
}

// 检查ID是否重复
const checkIdDuplicate = async () => {
    const id = scheduleForm.value.scheduleId
    if (!id) {
        idValidationStatus.value = 'error'
        idValidationMessage.value = 'ID不能为空'
        return false
    }

    // 检查本地已加载的数据
    const isDuplicate = schedules.value.some(s => s.id === id)
    if (isDuplicate) {
        idValidationStatus.value = 'error'
        idValidationMessage.value = `ID ${id} 已存在，请更换`
        return false
    }

    // 验证成功
    idValidationStatus.value = 'success'
    idValidationMessage.value = 'ID可用'
    return true
}

// 保存排课
const handleSaveSchedule = async () => {
    // 验证ID
    if (!scheduleForm.value.scheduleId) {
        message.warning('请填写排课ID')
        return
    }

    // 检查ID是否重复
    const isIdValid = await checkIdDuplicate()
    if (!isIdValid) {
        message.error('ID验证失败，请检查')
        return
    }

    // 验证其他字段
    if (!scheduleForm.value.courseId) {
        message.warning('请选择课程')
        return
    }
    if (!scheduleForm.value.teacherId) {
        message.warning('请选择教师')
        return
    }
    if (!scheduleForm.value.weekDay) {
        message.warning('请选择星期')
        return
    }
    if (!scheduleForm.value.sectionStart || !scheduleForm.value.sectionEnd) {
        message.warning('请设置节次范围')
        return
    }
    if (scheduleForm.value.sectionStart > scheduleForm.value.sectionEnd) {
        message.warning('开始节次不能大于结束节次')
        return
    }

    // 检查时间冲突
    const hasConflict = schedules.value.some(s => {
        if (s.weekDay !== scheduleForm.value.weekDay) return false

        // 检查节次是否重叠
        return !(scheduleForm.value.sectionEnd < s.sectionStart ||
                 scheduleForm.value.sectionStart > s.sectionEnd)
    })

    if (hasConflict) {
        message.error('该时间段已有排课，请选择其他时间')
        return
    }

    saving.value = true
    try {
        // 构建请求数据，包含ID
        const requestData = {
            id: scheduleForm.value.scheduleId,  // 包含ID
            courseId: scheduleForm.value.courseId,
            teacherId: scheduleForm.value.teacherId,
            weekDay: scheduleForm.value.weekDay,
            sectionStart: scheduleForm.value.sectionStart,
            sectionEnd: scheduleForm.value.sectionEnd,
            location: scheduleForm.value.location,
            semester: scheduleForm.value.semester,
            maxCapacity: scheduleForm.value.maxCapacity
        }
        console.log('提交排课数据:', requestData)
        await axios.post(`${API_BASE}/schedule/save`, requestData)
        message.success('排课成功')
        showAddModal.value = false
        await loadSchedules()
    } catch (error) {
        console.error('排课失败详情:', error.response?.data || error.message)
        message.error('排课失败: ' + (error.response?.data?.message || error.message))
    } finally {
        saving.value = false
    }
}

// 删除排课
const handleDeleteSchedule = () => {
    dialog.warning({
        title: '确认删除',
        content: `确定要删除《${selectedSchedule.value.courseName}》的排课吗？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            deleting.value = true
            try {
                await axios.delete(`${API_BASE}/schedule/${selectedSchedule.value.id}`)
                message.success('删除成功')
                showDetailModal.value = false
                await loadSchedules()
            } catch (error) {
                message.error('删除失败')
            } finally {
                deleting.value = false
            }
        }
    })
}

// 初始化
onMounted(() => {
    loadSchedules()
    loadCourses()
    loadTeachers()
})
</script>

<style scoped>
.schedule-page {
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
    position: relative;
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

.course-block.is-start {
    border-top-left-radius: 4px;
    border-top-right-radius: 4px;
}

.course-block.is-end {
    border-bottom-left-radius: 4px;
    border-bottom-right-radius: 4px;
}

.course-block.is-middle {
    border-radius: 0;
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

.course-code,
.teacher-name {
    font-size: 11px;
    color: #666;
    margin: 2px 0;
}

.course-detail {
    font-size: 10px;
    color: #999;
    margin-top: 4px;
}

.empty-cell {
    width: 100%;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #ddd;
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
