<template>
    <div class="course-material-page">
        <!-- 顶部搜索栏 -->
        <n-card class="search-card">
            <n-space vertical>
                <n-input
                    v-model:value="searchKeyword"
                    placeholder="搜索课程资料（支持全文搜索）"
                    clearable
                    @keyup.enter="handleSearch"
                >
                    <template #prefix>
                        <n-icon :component="SearchOutline" />
                    </template>
                    <template #suffix>
                        <n-button text type="primary" @click="handleSearch">搜索</n-button>
                    </template>
                </n-input>
            </n-space>
        </n-card>

        <!-- 左侧课程列表 + 右侧资源查看 -->
        <div class="content-container">
            <div class="left-panel">
                <n-card title="我的课程" :bordered="false">
                    <n-spin :show="loadingCourses">
                        <n-empty v-if="courseList.length === 0" description="暂无选修课程">
                            <template #icon>
                                <n-icon :component="BookOutline" size="60" />
                            </template>
                        </n-empty>
                        <n-list v-else hoverable clickable>
                            <n-list-item
                                v-for="course in courseList"
                                :key="course.id"
                                :class="{ 'active-course': currentCourse?.id === course.id }"
                                @click="selectCourse(course)"
                            >
                                <n-thing>
                                    <template #header>
                                        <n-text strong>{{ course.name }}</n-text>
                                    </template>
                                    <template #description>
                                        <n-space size="small">
                                            <n-tag size="small" type="info">{{ course.teacherName }}</n-tag>
                                            <n-tag size="small" type="success">{{ course.credit }}学分</n-tag>
                                        </n-space>
                                    </template>
                                </n-thing>
                            </n-list-item>
                        </n-list>
                    </n-spin>
                </n-card>
            </div>

            <div class="right-panel">
                <n-card v-if="currentCourse" :title="`${currentCourse.name} - 课程资源`" :bordered="false">
                    <n-space vertical :size="20">
                        <!-- 资源列表 -->
                        <div>
                            <n-space justify="space-between" align="center" style="margin-bottom: 12px">
                                <n-text strong>课程资料 ({{ resourceList.length }})</n-text>
                                <n-button size="small" @click="loadResources" :loading="loadingResources">
                                    <template #icon>
                                        <n-icon :component="RefreshOutline" />
                                    </template>
                                    刷新
                                </n-button>
                            </n-space>

                            <n-data-table
                                :columns="resourceColumns"
                                :data="resourceList"
                                :pagination="{ pageSize: 10 }"
                                :bordered="false"
                                :loading="loadingResources"
                                striped
                            />
                        </div>
                    </n-space>
                </n-card>

                <n-empty v-else description="请从左侧选择一门课程查看资料" style="margin-top: 100px">
                    <template #icon>
                        <n-icon :component="DocumentTextOutline" size="80" />
                    </template>
                </n-empty>
            </div>
        </div>

        <!-- 预览弹窗 -->
        <n-modal
            v-model:show="showPreviewModal"
            preset="card"
            :title="previewTitle"
            style="width: 80%; max-width: 1200px;"
            :bordered="false"
            :segmented="{ content: 'soft' }"
        >
            <div class="preview-container">
                <iframe v-if="previewUrl" :src="previewUrl" frameborder="0" style="width: 100%; height: 600px;"></iframe>
                <n-empty v-else description="无法预览此文件" />
            </div>
        </n-modal>
    </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import {
    NCard, NInput, NSpace, NList, NListItem, NThing, NText, NTag,
    NDivider, NDataTable, NButton, NIcon, NEmpty, NSpin, NModal,
    useMessage
} from 'naive-ui'
import {
    SearchOutline, RefreshOutline, DocumentTextOutline, BookOutline,
    EyeOutline, DownloadOutline
} from '@vicons/ionicons5'
import axios from 'axios'

const API_BASE = 'http://localhost:8080'
const message = useMessage()

// 状态管理
const courseList = ref([])
const currentCourse = ref(null)
const resourceList = ref([])
const searchKeyword = ref('')
const showPreviewModal = ref(false)
const previewUrl = ref('')
const previewTitle = ref('')
const loadingCourses = ref(false)
const loadingResources = ref(false)

const studentId = localStorage.getItem('username') || '2021001'

// 资源表格列定义（学生版 - 无删除操作）
const resourceColumns = [
    {
        title: '文件名',
        key: 'resourceName',
        ellipsis: { tooltip: true },
        render: (row) => {
            // 如果有高亮，显示HTML（搜索结果）
            if (row.resourceName && row.resourceName.includes('<em')) {
                return h('div', { innerHTML: row.resourceName })
            }
            return row.resourceName
        }
    },
    {
        title: '课程',
        key: 'courseName',
        width: 150,
        render: (row) => {
            if (!row.courseName) return '-'
            // 如果有高亮，显示HTML
            if (row.courseName.includes('<em')) {
                return h('div', { innerHTML: row.courseName })
            }
            return row.courseName
        }
    },
    {
        title: '教师',
        key: 'teacherName',
        width: 100,
        render: (row) => {
            if (!row.teacherName) return '-'
            // 如果有高亮，显示HTML
            if (row.teacherName.includes('<em')) {
                return h('div', { innerHTML: row.teacherName })
            }
            return row.teacherName
        }
    },
    {
        title: '文件类型',
        key: 'resourceType',
        width: 100,
        render: (row) => {
            const typeMap = {
                'pdf': { text: 'PDF', type: 'error' },
                'doc': { text: 'Word', type: 'info' },
                'docx': { text: 'Word', type: 'info' },
                'ppt': { text: 'PPT', type: 'warning' },
                'pptx': { text: 'PPT', type: 'warning' },
                'mp4': { text: '视频', type: 'success' },
                'mp3': { text: '音频', type: 'success' },
                'avi': { text: '视频', type: 'success' },
                'mov': { text: '视频', type: 'success' }
            }
            const config = typeMap[row.resourceType?.toLowerCase()] || { text: row.resourceType, type: 'default' }
            return h(NTag, { type: config.type, size: 'small' }, { default: () => config.text })
        }
    },
    {
        title: '上传时间',
        key: 'createTime',
        width: 180,
        render: (row) => {
            return row.createTime ? new Date(row.createTime).toLocaleString('zh-CN') : '-'
        }
    },
    {
        title: '操作',
        key: 'actions',
        width: 160,
        render: (row) => {
            return h(NSpace, null, {
                default: () => [
                    h(NButton, {
                        size: 'small',
                        type: 'primary',
                        onClick: () => handlePreview(row)
                    }, {
                        default: () => '预览',
                        icon: () => h(NIcon, { component: EyeOutline })
                    }),
                    h(NButton, {
                        size: 'small',
                        onClick: () => handleDownload(row)
                    }, {
                        default: () => '下载',
                        icon: () => h(NIcon, { component: DownloadOutline })
                    })
                ]
            })
        }
    }
]

// 加载学生选修的课程
const loadCourses = async () => {
    loadingCourses.value = true
    try {
        // 获取学生选的所有课程
        const res = await axios.get(`${API_BASE}/api/timetable/student/${studentId}`)
        const schedules = res.data.data || []

        console.log('✅ 学生选课列表:', schedules)

        // 提取唯一的课程信息
        const courseMap = new Map()
        schedules.forEach(item => {
            if (!courseMap.has(item.course_id)) {
                courseMap.set(item.course_id, {
                    id: item.course_id,
                    name: item.course_name,
                    teacherName: item.teacher_name,
                    credit: item.credit
                })
            }
        })

        courseList.value = Array.from(courseMap.values())
        console.log('✅ 课程列表:', courseList.value)

        // 自动选择第一门课程
        if (courseList.value.length > 0 && !currentCourse.value) {
            await selectCourse(courseList.value[0])
        }
    } catch (error) {
        console.error('❌ 加载课程列表失败:', error)
        message.error('加载课程列表失败: ' + (error.message || ''))
    } finally {
        loadingCourses.value = false
    }
}

// 选择课程
const selectCourse = async (course) => {
    currentCourse.value = course
    await loadResources()
}

// 加载资源列表
const loadResources = async () => {
    if (!currentCourse.value) return

    loadingResources.value = true
    try {
        const res = await axios.get(`${API_BASE}/resource/list/${currentCourse.value.id}`)
        // 后端直接返回数组，不是 { data: [...] } 格式
        resourceList.value = res.data || []
        console.log('✅ 加载资源成功:', resourceList.value.length, '个资源')
    } catch (error) {
        console.error('❌ 加载资源列表失败:', error)
        message.error('加载资源列表失败')
        resourceList.value = []
    } finally {
        loadingResources.value = false
    }
}

// 全文搜索
const handleSearch = async () => {
    if (!searchKeyword.value.trim()) {
        message.warning('请输入搜索关键词')
        return
    }

    loadingResources.value = true
    try {
        const res = await axios.get(`${API_BASE}/course/search-resources`, {
            params: { keyword: searchKeyword.value }
        })

        if (res.data.code === 200 && res.data.data) {
            // 清空当前课程，显示搜索结果
            currentCourse.value = null
            resourceList.value = res.data.data.map(item => ({
                id: item.id,
                resourceName: item.highlights?.resourceName || item.resourceName,
                courseName: item.highlights?.courseName || item.courseName,
                teacherName: item.highlights?.teacherName || item.teacherName,
                resourceType: item.resourceType,
                createTime: item.createTime,
                score: item.score // 相关度评分
            }))

            message.success(`找到 ${res.data.total} 条相关资料`)
        } else {
            message.info('未找到相关资源')
            resourceList.value = []
        }
    } catch (error) {
        console.error('❌ 搜索失败:', error)
        message.error('搜索失败: ' + (error.message || ''))
        resourceList.value = []
    } finally {
        loadingResources.value = false
    }
}

// 预览文件
const handlePreview = async (file) => {
    try {
        const res = await axios.get(`${API_BASE}/resource/preview/${file.id}`)
        if (res.data && res.data.url) {
            previewTitle.value = file.resourceName
            previewUrl.value = res.data.url
            showPreviewModal.value = true
        } else {
            message.error('获取预览链接失败')
        }
    } catch (error) {
        console.error('获取预览链接失败:', error)
        message.error('获取预览链接失败')
    }
}

// 下载文件
const handleDownload = async (file) => {
    try {
        const res = await axios.get(`${API_BASE}/resource/preview/${file.id}`)
        if (res.data && res.data.url) {
            window.open(res.data.url, '_blank')
            message.success('开始下载')
        } else {
            message.error('获取下载链接失败')
        }
    } catch (error) {
        console.error('下载失败:', error)
        message.error('下载失败')
    }
}

// 初始化
onMounted(() => {
    loadCourses()
})
</script>

<style scoped>
.course-material-page {
    padding: 20px;
    height: 100%;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.search-card {
    flex-shrink: 0;
}

.content-container {
    display: flex;
    gap: 20px;
    flex: 1;
    overflow: hidden;
}

.left-panel {
    width: 320px;
    flex-shrink: 0;
    overflow-y: auto;
}

.right-panel {
    flex: 1;
    overflow-y: auto;
}

.active-course {
    background-color: rgba(24, 160, 88, 0.1);
}

.preview-container {
    width: 100%;
    min-height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
}
</style>
