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
                <n-card v-if="currentCourse" :title="`${currentCourse.name} - 课程资源`" :bordered="false">
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

// 上传请求头（如果需要token）
const uploadHeaders = ref({
    // 'Authorization': 'Bearer ' + localStorage.getItem('token')
})

// 资源表格列定义
const resourceColumns = [
    {
        title: '文件名',
        key: 'resourceName',
        ellipsis: { tooltip: true }
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

// 加载所有课程
const loadCourses = async () => {
    try {
        const res = await axios.get(`${API_BASE}/course/all`)
        courseList.value = res.data.data || []
    } catch (error) {
        message.error('加载课程列表失败')
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

    try {
        const res = await axios.get(`${API_BASE}/resource/list`, {
            params: { courseId: currentCourse.value.id }
        })
        resourceList.value = res.data.data || []
    } catch (error) {
        message.error('加载资源列表失败')
    }
}

// 上传完成回调
const handleUploadFinish = ({ event }) => {
    const response = JSON.parse(event.target.response)
    if (response.code === 200) {
        message.success('上传成功')
        loadResources()
    } else {
        message.error(response.message || '上传失败')
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
        const res = await axios.get(`${API_BASE}/course/search`, {
            params: { keyword: searchKeyword.value }
        })

        if (res.data.code === 200 && res.data.data) {
            message.success(`找到 ${res.data.data.length} 条结果`)
            // TODO: 展示搜索结果
        } else {
            message.info('未找到相关资源')
        }
    } catch (error) {
        message.error('搜索失败')
    }
}

// 预览文件
const handlePreview = async (file) => {
    try {
        const res = await axios.get(`${API_BASE}/resource/preview/${file.id}`)
        if (res.data.code === 200) {
            previewFile.value = {
                ...file,
                previewUrl: res.data.data
            }
            showPreview.value = true
        }
    } catch (error) {
        message.error('获取预览链接失败')
    }
}

// 下载文件
const handleDownload = async (file) => {
    try {
        window.open(`${API_BASE}/resource/download/${file.id}`, '_blank')
        message.success('开始下载')
    } catch (error) {
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
                const res = await axios.delete(`${API_BASE}/resource/delete/${file.id}`)
                if (res.data.code === 200) {
                    message.success('删除成功')
                    loadResources()
                } else {
                    message.error(res.data.message || '删除失败')
                }
            } catch (error) {
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
