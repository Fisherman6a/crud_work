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

        <!-- 左侧课程列表 + 右侧资源管理 -->
        <div class="content-container">
            <div class="left-panel">
                <n-card title="课程列表" :bordered="false">
                    <n-list hoverable clickable>
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
                </n-card>
            </div>

            <div class="right-panel">
                <!-- 搜索结果模式 -->
                <n-card v-if="isSearchMode" title="搜索结果" :bordered="false">
                    <n-space vertical :size="20">
                        <!-- 搜索结果列表（无上传区域）-->
                        <div>
                            <n-space justify="space-between" align="center" style="margin-bottom: 12px">
                                <n-text strong>找到 {{ resourceList.length }} 条资料</n-text>
                            </n-space>

                            <n-data-table
                                :columns="searchColumns"
                                :data="resourceList"
                                :pagination="{ pageSize: 10 }"
                                :bordered="false"
                                striped
                            />
                        </div>
                    </n-space>
                </n-card>

                <!-- 课程模式（包含上传功能）-->
                <n-card v-else-if="currentCourse" :title="`${currentCourse.name} - 课程资源`" :bordered="false">
                    <!-- 上传区域 -->
                    <n-space vertical :size="20">
                        <n-upload
                            multiple
                            directory-dnd
                            :action="`http://localhost:8080/resource/upload?courseId=${currentCourse.id}`"
                            :headers="uploadHeaders"
                            accept=".pdf,.doc,.docx,.ppt,.pptx,.mp4,.mp3,.avi,.mov"
                            @finish="handleUploadFinish"
                            @error="handleUploadError"
                        >
                            <n-upload-dragger>
                                <div style="margin-bottom: 12px">
                                    <n-icon size="48" :depth="3">
                                        <CloudUploadOutline />
                                    </n-icon>
                                </div>
                                <n-text style="font-size: 16px">
                                    点击或拖拽文件到此区域上传
                                </n-text>
                                <n-p depth="3" style="margin: 8px 0 0 0">
                                    支持 Word、PDF、PPT、音视频文件
                                </n-p>
                            </n-upload-dragger>
                        </n-upload>

                        <n-divider />

                        <!-- 资源列表 -->
                        <div>
                            <n-space justify="space-between" align="center" style="margin-bottom: 12px">
                                <n-text strong>已上传资源 ({{ resourceList.length }})</n-text>
                                <n-button size="small" @click="loadResources">
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
                                striped
                            />
                        </div>
                    </n-space>
                </n-card>

                <!-- 空状态 -->
                <n-empty v-else description="请从左侧选择一门课程" style="margin-top: 100px">
                    <template #icon>
                        <n-icon :component="DocumentTextOutline" size="80" />
                    </template>
                </n-empty>
            </div>
        </div>

        <!-- 文件预览组件 -->
        <FilePreview
            v-model:show="showPreview"
            :file="previewFile"
        />
    </div>
</template>

<script setup>
import { ref, h, onMounted } from 'vue'
import {
    NCard, NInput, NSpace, NList, NListItem, NThing, NText, NTag, NUpload,
    NUploadDragger, NP, NDivider, NDataTable, NButton, NIcon, NEmpty,
    useMessage, useDialog
} from 'naive-ui'
import {
    SearchOutline, CloudUploadOutline, RefreshOutline, DocumentTextOutline,
    EyeOutline, DownloadOutline, TrashOutline
} from '@vicons/ionicons5'
import axios from 'axios'
import FilePreview from '@/components/FilePreview.vue'

const API_BASE = 'http://localhost:8080'
const message = useMessage()
const dialog = useDialog()

// 状态管理
const courseList = ref([])
const currentCourse = ref(null)
const resourceList = ref([])
const searchKeyword = ref('')
const showPreview = ref(false)
const previewFile = ref(null)
const isSearchMode = ref(false) // 标识是否处于搜索模式

// 上传请求头（如果需要token）
const uploadHeaders = ref({
    // 'Authorization': 'Bearer ' + localStorage.getItem('token')
})

// 资源表格列定义（课程资源模式）
const resourceColumns = [
    {
        title: '文件名',
        key: 'resourceName',
        ellipsis: { tooltip: true },
        render: (row) => {
            // 如果包含高亮标签，需要渲染HTML
            if (row.resourceName && row.resourceName.includes('<em>')) {
                return h('span', { innerHTML: row.resourceName })
            }
            return row.resourceName
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
            return row.createTime ? new Date(row.createTime).toLocaleString() : '-'
        }
    },
    {
        title: '操作',
        key: 'actions',
        width: 200,
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
                    }),
                    h(NButton, {
                        size: 'small',
                        type: 'error',
                        onClick: () => handleDelete(row)
                    }, {
                        default: () => '删除',
                        icon: () => h(NIcon, { component: TrashOutline })
                    })
                ]
            })
        }
    }
]

// 搜索结果表格列定义（包含课程和教师信息）
const searchColumns = [
    {
        title: '文件名',
        key: 'resourceName',
        ellipsis: { tooltip: true },
        render: (row) => {
            // 如果包含高亮标签，需要渲染HTML
            if (row.resourceName && row.resourceName.includes('<em>')) {
                return h('span', { innerHTML: row.resourceName })
            }
            return row.resourceName
        }
    },
    {
        title: '所属课程',
        key: 'courseName',
        width: 150,
        ellipsis: { tooltip: true },
        render: (row) => {
            if (row.courseName && row.courseName.includes('<em>')) {
                return h('span', { innerHTML: row.courseName })
            }
            return row.courseName || '-'
        }
    },
    {
        title: '任课教师',
        key: 'teacherName',
        width: 100,
        render: (row) => {
            if (row.teacherName && row.teacherName.includes('<em>')) {
                return h('span', { innerHTML: row.teacherName })
            }
            return row.teacherName || '-'
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
        title: '操作',
        key: 'actions',
        width: 180,
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
                    }),
                    h(NButton, {
                        size: 'small',
                        type: 'error',
                        onClick: () => handleDelete(row)
                    }, {
                        default: () => '删除',
                        icon: () => h(NIcon, { component: TrashOutline })
                    })
                ]
            })
        }
    }
]

// 加载所有课程
const loadCourses = async () => {
    try {
        const res = await axios.get(`${API_BASE}/course/all`)
        // 后端直接返回数组，不是 { data: [...] } 格式
        courseList.value = res.data || []
    } catch (error) {
        console.error('加载课程列表失败:', error)
        message.error('加载课程列表失败')
    }
}

// 选择课程
const selectCourse = async (course) => {
    currentCourse.value = course
    isSearchMode.value = false // 退出搜索模式
    await loadResources()
}

// 加载资源列表
const loadResources = async () => {
    if (!currentCourse.value) return

    try {
        // 后端接口是 /resource/list/{courseId}，返回直接数组
        const res = await axios.get(`${API_BASE}/resource/list/${currentCourse.value.id}`)
        resourceList.value = res.data || []
        console.log('加载资源成功:', resourceList.value.length, '个资源')
    } catch (error) {
        console.error('加载资源列表失败:', error)
        message.error('加载资源列表失败')
    }
}

// 上传完成回调
const handleUploadFinish = ({ event }) => {
    try {
        const response = JSON.parse(event.target.response)
        console.log('上传响应:', response)
        if (response.success) {
            message.success('上传成功')
            loadResources()
        } else {
            message.error(response.message || '上传失败')
        }
    } catch (error) {
        console.error('解析上传响应失败:', error)
        message.error('上传失败')
    }
}

// 上传错误回调
const handleUploadError = () => {
    message.error('上传失败，请稍后重试')
}

// 全文搜索
const handleSearch = async () => {
    if (!searchKeyword.value.trim()) {
        message.warning('请输入搜索关键词')
        return
    }

    try {
        const res = await axios.get(`${API_BASE}/course/search-resources`, {
            params: { keyword: searchKeyword.value }
        })

        if (res.data.code === 200 && res.data.data) {
            // 进入搜索模式，清空当前选中的课程
            isSearchMode.value = true
            currentCourse.value = null

            // 将搜索结果映射到资源列表，优先使用高亮字段
            resourceList.value = res.data.data.map(item => ({
                id: item.id,
                resourceName: item.highlights?.resourceName || item.resourceName,
                courseName: item.highlights?.courseName || item.courseName,
                teacherName: item.highlights?.teacherName || item.teacherName,
                resourceType: item.resourceType,
                createTime: item.createTime,
                courseId: item.courseId,
                score: item.score
            }))

            message.success(`找到 ${res.data.total} 条相关资料`)
        } else {
            message.info('未找到相关资源')
        }
    } catch (error) {
        message.error('搜索失败: ' + (error.response?.data?.msg || error.message))
    }
}

// 预览文件
const handlePreview = async (file) => {
    try {
        // 后端接口返回 { url, fileName, fileType }
        const res = await axios.get(`${API_BASE}/resource/preview/${file.id}`)
        if (res.data && res.data.url) {
            previewFile.value = {
                ...file,
                previewUrl: res.data.url
            }
            showPreview.value = true
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
        // 使用预览链接也可以下载
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

// 删除文件
const handleDelete = (file) => {
    dialog.warning({
        title: '确认删除',
        content: `确定要删除 "${file.resourceName}" 吗？此操作不可恢复。`,
        positiveText: '删除',
        negativeText: '取消',
        onPositiveClick: async () => {
            try {
                // 后端接口是 DELETE /resource/{resourceId}，返回 { success: boolean }
                const res = await axios.delete(`${API_BASE}/resource/${file.id}`)
                if (res.data && res.data.success) {
                    message.success('删除成功')
                    loadResources()
                } else {
                    message.error('删除失败')
                }
            } catch (error) {
                console.error('删除失败:', error)
                message.error('删除失败')
            }
        }
    })
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

:deep(.n-upload-trigger) {
    width: 100%;
}
</style>
