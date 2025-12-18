<template>
    <div class="student-course-page">
        <!-- 顶部搜索和筛选栏 -->
        <n-card class="search-filter-card">
            <n-space vertical :size="16">
                <n-space align="center" :size="12">
                    <n-input
                        v-model:value="searchKeyword"
                        placeholder="搜索课程名称或教师"
                        clearable
                        style="flex: 1;"
                    >
                        <template #prefix>
                            <n-icon :component="SearchOutline" />
                        </template>
                    </n-input>
                    <n-select
                        v-model:value="filterDay"
                        :options="dayOptions"
                        placeholder="上课时间"
                        clearable
                        style="width: 150px;"
                    />
                    <n-select
                        v-model:value="filterCredit"
                        :options="creditOptions"
                        placeholder="学分"
                        clearable
                        style="width: 120px;"
                    />
                    <n-button type="primary" @click="loadCourses">
                        <template #icon>
                            <n-icon :component="RefreshOutline" />
                        </template>
                        刷新
                    </n-button>
                    <n-button type="info" @click="showMyCoursesDrawer = true">
                        <template #icon>
                            <n-icon :component="CalendarOutline" />
                        </template>
                        我的课表
                    </n-button>
                </n-space>
            </n-space>
        </n-card>

        <!-- 课程卡片网格 -->
        <div class="courses-container">
            <n-spin :show="loading">
                <n-empty v-if="filteredCourses.length === 0" description="暂无可选课程">
                    <template #icon>
                        <n-icon :component="SchoolOutline" size="80" />
                    </template>
                </n-empty>

                <n-grid v-else :x-gap="20" :y-gap="20" :cols="responsive.cols">
                    <n-grid-item v-for="course in filteredCourses" :key="course.id">
                        <n-card
                            hoverable
                            class="course-card"
                            :class="{ 'selected-course': isSelected(course) }"
                        >
                            <template #cover>
                                <div class="course-cover" :style="{ background: getCoverGradient(course.id) }">
                                    <div class="course-id">课程编号: {{ course.id }}</div>
                                </div>
                            </template>

                            <template #header>
                                <n-ellipsis style="max-width: 100%">
                                    {{ course.name }}
                                </n-ellipsis>
                            </template>

                            <template #header-extra>
                                <n-tag :type="getCapacityTagType(course)" size="small">
                                    {{ course.currentCount || 0 }}/{{ course.maxCapacity || 50 }}
                                </n-tag>
                            </template>

                            <n-space vertical :size="12">
                                <n-space align="center" :size="8">
                                    <n-icon :component="PersonOutline" />
                                    <n-text depth="3">教师：</n-text>
                                    <n-text>{{ course.teacherName || '待定' }}</n-text>
                                </n-space>

                                <n-space align="center" :size="8">
                                    <n-icon :component="TimeOutline" />
                                    <n-text depth="3">时间：</n-text>
                                    <n-text>{{ formatCourseTime(course) }}</n-text>
                                </n-space>

                                <n-space align="center" :size="8">
                                    <n-icon :component="LocationOutline" />
                                    <n-text depth="3">地点：</n-text>
                                    <n-text>{{ course.location || '待定' }}</n-text>
                                </n-space>

                                <n-space align="center" :size="8">
                                    <n-icon :component="TrophyOutline" />
                                    <n-text depth="3">学分：</n-text>
                                    <n-tag :bordered="false" type="success" size="small">
                                        {{ course.credit || 0 }}学分
                                    </n-tag>
                                </n-space>

                                <n-divider style="margin: 8px 0" />

                                <n-ellipsis :line-clamp="2" :tooltip="{ width: 300 }">
                                    <n-text depth="3">{{ course.description || '暂无课程简介' }}</n-text>
                                </n-ellipsis>

                                <!-- 选课进度条 -->
                                <n-progress
                                    type="line"
                                    :percentage="getCapacityPercentage(course)"
                                    :color="getProgressColor(course)"
                                    :height="8"
                                    :border-radius="4"
                                />
                            </n-space>

                            <template #footer>
                                <n-space justify="space-between">
                                    <n-button text type="info" @click="showCourseDetail(course)">
                                        查看详情
                                    </n-button>
                                    <n-button
                                        v-if="!isSelected(course)"
                                        type="primary"
                                        :disabled="isFull(course)"
                                        @click="handleSelectCourse(course)"
                                    >
                                        {{ isFull(course) ? '已满' : '选课' }}
                                    </n-button>
                                    <n-button v-else type="error" @click="handleDropCourse(course)">
                                        退课
                                    </n-button>
                                </n-space>
                            </template>
                        </n-card>
                    </n-grid-item>
                </n-grid>
            </n-spin>
        </div>

        <!-- 课程详情抽屉 -->
        <n-drawer v-model:show="showDetailDrawer" :width="600" placement="right">
            <n-drawer-content v-if="selectedCourse" :title="selectedCourse.name">
                <n-space vertical :size="20">
                    <n-descriptions :column="1" bordered>
                        <n-descriptions-item label="课程编号">{{ selectedCourse.id }}</n-descriptions-item>
                        <n-descriptions-item label="授课教师">{{ selectedCourse.teacherName }}</n-descriptions-item>
                        <n-descriptions-item label="上课时间">{{ formatCourseTime(selectedCourse) }}</n-descriptions-item>
                        <n-descriptions-item label="上课地点">{{ selectedCourse.location }}</n-descriptions-item>
                        <n-descriptions-item label="学分">{{ selectedCourse.credit }}学分</n-descriptions-item>
                        <n-descriptions-item label="容量">
                            {{ selectedCourse.currentCount }}/{{ selectedCourse.maxCapacity }}
                        </n-descriptions-item>
                    </n-descriptions>

                    <div>
                        <n-text strong>课程简介：</n-text>
                        <n-p style="margin-top: 12px;">{{ selectedCourse.description || '暂无课程简介' }}</n-p>
                    </div>

                    <div>
                        <n-text strong>课程资料：</n-text>
                        <n-list bordered style="margin-top: 12px;">
                            <n-list-item v-for="resource in courseResources" :key="resource.id">
                                <n-space justify="space-between" align="center">
                                    <span>{{ resource.resourceName }}</span>
                                    <n-button size="small" text type="primary">查看</n-button>
                                </n-space>
                            </n-list-item>
                            <n-empty v-if="courseResources.length === 0" description="暂无课程资料" />
                        </n-list>
                    </div>
                </n-space>

                <template #footer>
                    <n-space justify="end">
                        <n-button @click="showDetailDrawer = false">关闭</n-button>
                        <n-button
                            v-if="!isSelected(selectedCourse)"
                            type="primary"
                            :disabled="isFull(selectedCourse)"
                            @click="handleSelectCourse(selectedCourse)"
                        >
                            {{ isFull(selectedCourse) ? '已满' : '确认选课' }}
                        </n-button>
                    </n-space>
                </template>
            </n-drawer-content>
        </n-drawer>

        <!-- 我的课表抽屉 -->
        <n-drawer v-model:show="showMyCoursesDrawer" :width="800" placement="right">
            <n-drawer-content title="我的课表">
                <n-calendar v-model:value="calendarValue" #="{ year, month, date }">
                    <div v-if="hasCourseOn(year, month, date)" class="calendar-event">
                        <n-tag type="success" size="small" style="width: 100%">
                            {{ getCoursesOn(year, month, date) }}
                        </n-tag>
                    </div>
                </n-calendar>
            </n-drawer-content>
        </n-drawer>
    </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
    NCard, NInput, NSpace, NSelect, NButton, NIcon, NSpin, NEmpty, NGrid, NGridItem,
    NTag, NText, NEllipsis, NDivider, NProgress, NDrawer, NDrawerContent,
    NDescriptions, NDescriptionsItem, NP, NList, NListItem, NCalendar,
    useMessage, useDialog
} from 'naive-ui'
import {
    SearchOutline, RefreshOutline, CalendarOutline, SchoolOutline,
    PersonOutline, TimeOutline, LocationOutline, TrophyOutline
} from '@vicons/ionicons5'
import axios from 'axios'

const API_BASE = 'http://localhost:8080'
const message = useMessage()
const dialog = useDialog()

// 状态管理
const loading = ref(false)
const courses = ref([])
const myCourses = ref([])
const searchKeyword = ref('')
const filterDay = ref(null)
const filterCredit = ref(null)
const showDetailDrawer = ref(false)
const showMyCoursesDrawer = ref(false)
const selectedCourse = ref(null)
const courseResources = ref([])
const calendarValue = ref(Date.now())

// 模拟当前学生ID（实际应从localStorage或store获取）
const studentId = ref(localStorage.getItem('username') || '2021001')

// 筛选选项
const dayOptions = [
    { label: '周一', value: 1 },
    { label: '周二', value: 2 },
    { label: '周三', value: 3 },
    { label: '周四', value: 4 },
    { label: '周五', value: 5 },
    { label: '周六', value: 6 },
    { label: '周日', value: 7 }
]

const creditOptions = [
    { label: '1学分', value: 1 },
    { label: '2学分', value: 2 },
    { label: '3学分', value: 3 },
    { label: '4学分', value: 4 }
]

// 响应式列数
const responsive = computed(() => {
    const width = window.innerWidth
    if (width < 768) return { cols: 1 }
    if (width < 1200) return { cols: 2 }
    if (width < 1600) return { cols: 3 }
    return { cols: 4 }
})

// 过滤后的课程列表
const filteredCourses = computed(() => {
    let result = courses.value

    if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        result = result.filter(course =>
            course.name?.toLowerCase().includes(keyword) ||
            course.teacherName?.toLowerCase().includes(keyword)
        )
    }

    if (filterDay.value !== null) {
        result = result.filter(course => course.weekDay === filterDay.value)
    }

    if (filterCredit.value !== null) {
        result = result.filter(course => course.credit === filterCredit.value)
    }

    return result
})

// 工具函数
const getCoverGradient = (id) => {
    const colors = [
        'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
        'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
        'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
        'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
        'linear-gradient(135deg, #30cfd0 0%, #330867 100%)'
    ]
    return colors[id % colors.length]
}

const formatCourseTime = (course) => {
    if (!course.weekDay) return '时间待定'
    const dayMap = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日']
    const day = dayMap[course.weekDay] || '未知'
    const section = course.sectionStart && course.sectionEnd
        ? `第${course.sectionStart}-${course.sectionEnd}节`
        : '节次待定'
    return `${day} ${section}`
}

const getCapacityPercentage = (course) => {
    const current = course.currentCount || 0
    const max = course.maxCapacity || 50
    return Math.min((current / max) * 100, 100)
}

const getCapacityTagType = (course) => {
    const percentage = getCapacityPercentage(course)
    if (percentage >= 100) return 'error'
    if (percentage >= 80) return 'warning'
    return 'success'
}

const getProgressColor = (course) => {
    const percentage = getCapacityPercentage(course)
    if (percentage >= 100) return '#d03050'
    if (percentage >= 80) return '#f0a020'
    return '#18a058'
}

const isFull = (course) => {
    return (course.currentCount || 0) >= (course.maxCapacity || 50)
}

const isSelected = (course) => {
    return myCourses.value.some(c => c.scheduleId === course.id)
}

// 日历相关
const hasCourseOn = (year, month, date) => {
    // TODO: 实现根据实际排课时间判断
    return false
}

const getCoursesOn = (year, month, date) => {
    // TODO: 返回当天的课程名称
    return ''
}

// API调用
const loadCourses = async () => {
    loading.value = true
    try {
        const res = await axios.get(`${API_BASE}/timetable/available`)
        courses.value = res.data.data || []
    } catch (error) {
        message.error('加载课程列表失败')
    } finally {
        loading.value = false
    }
}

const loadMyCourses = async () => {
    try {
        const res = await axios.get(`${API_BASE}/student-course/my-courses`, {
            params: { studentId: studentId.value }
        })
        myCourses.value = res.data.data || []
    } catch (error) {
        console.error('加载我的课程失败', error)
    }
}

const showCourseDetail = async (course) => {
    selectedCourse.value = course
    showDetailDrawer.value = true

    // 加载课程资料
    try {
        const res = await axios.get(`${API_BASE}/resource/list`, {
            params: { courseId: course.courseId }
        })
        courseResources.value = res.data.data || []
    } catch (error) {
        courseResources.value = []
    }
}

const handleSelectCourse = async (course) => {
    dialog.info({
        title: '确认选课',
        content: `确定要选择《${course.name}》吗？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            try {
                const res = await axios.post(`${API_BASE}/timetable/select`, {
                    studentId: studentId.value,
                    scheduleId: course.id
                })
                if (res.data.code === 200) {
                    message.success('选课成功')
                    showDetailDrawer.value = false
                    await loadCourses()
                    await loadMyCourses()
                } else {
                    message.error(res.data.message || '选课失败')
                }
            } catch (error) {
                message.error('选课失败，请稍后重试')
            }
        }
    })
}

const handleDropCourse = async (course) => {
    dialog.warning({
        title: '确认退课',
        content: `确定要退选《${course.name}》吗？`,
        positiveText: '确定',
        negativeText: '取消',
        onPositiveClick: async () => {
            try {
                const res = await axios.post(`${API_BASE}/timetable/drop`, {
                    studentId: studentId.value,
                    scheduleId: course.id
                })
                if (res.data.code === 200) {
                    message.success('退课成功')
                    await loadCourses()
                    await loadMyCourses()
                } else {
                    message.error(res.data.message || '退课失败')
                }
            } catch (error) {
                message.error('退课失败，请稍后重试')
            }
        }
    })
}

// 初始化
onMounted(() => {
    loadCourses()
    loadMyCourses()
})
</script>

<style scoped>
.student-course-page {
    padding: 20px;
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.search-filter-card {
    flex-shrink: 0;
}

.courses-container {
    flex: 1;
    overflow-y: auto;
}

.course-card {
    height: 100%;
    transition: all 0.3s ease;
}

.course-card:hover {
    transform: translateY(-4px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.selected-course {
    border: 2px solid #18a058;
}

.course-cover {
    height: 120px;
    display: flex;
    align-items: flex-end;
    justify-content: flex-end;
    padding: 12px;
}

.course-id {
    background: rgba(255, 255, 255, 0.9);
    padding: 4px 12px;
    border-radius: 4px;
    font-size: 12px;
    color: #333;
}

.calendar-event {
    margin-top: 4px;
}
</style>
