<template>
    <div class="course-manager">
        <n-card title="课程资料管理">
            <n-space vertical size="large">

                <n-upload multiple directory-dnd :action="uploadUrl" :headers="{ 'Authorization': token }"
                    :data="{ courseId: currentCourseId }" @finish="handleUploadFinish" @error="handleUploadError">
                    <n-upload-dragger>
                        <div style="margin-bottom: 12px">
                            <n-icon size="48" :depth="3">
                                <archive-outline />
                            </n-icon>
                        </div>
                        <n-text style="font-size: 16px">
                            点击或拖拽文件到此处上传
                        </n-text>
                        <n-p depth="3" style="margin: 8px 0 0 0">
                            支持 Word, PDF, PPT, MP4 等格式，上传后自动建立全文索引
                        </n-p>
                    </n-upload-dragger>
                </n-upload>

                <n-divider />

                <n-input-group>
                    <n-input v-model:value="searchKeyword" placeholder="输入关键词搜索课件内容（支持全文检索）"
                        @keydown.enter="handleSearch" />
                    <n-button type="primary" ghost @click="handleSearch">
                        搜索
                    </n-button>
                </n-input-group>

                <n-data-table :columns="columns" :data="fileList" :pagination="pagination" :loading="loading" />

            </n-space>
        </n-card>

        <n-modal v-model:show="showPreview" preset="card" style="width: 80%; height: 80vh" title="课件预览">
            <div class="preview-container">
                <video v-if="previewType === 'video'" controls :src="previewUrl"
                    style="width: 100%; height: 100%"></video>
                <iframe v-else :src="previewUrl" style="width: 100%; height: 100%; border: none;"></iframe>
            </div>
        </n-modal>
    </div>
</template>

<script setup>
import { ref, h } from 'vue'
import { ArchiveOutline } from '@vicons/ionicons5'
import { NButton, useMessage } from 'naive-ui'
import axios from 'axios'

const message = useMessage()
// 假设后端地址，请根据实际情况修改
const API_BASE = 'http://localhost:8080'

// 状态定义
const token = localStorage.getItem('token') // 假设登录后token存在这里
const currentCourseId = ref(101) // 模拟当前课程ID
const uploadUrl = `${API_BASE}/course/upload`
const searchKeyword = ref('')
const fileList = ref([])
const loading = ref(false)
const showPreview = ref(false)
const previewUrl = ref('')
const previewType = ref('')

// 表格列定义
const columns = [
    { title: '文件名', key: 'title' },
    { title: '课程ID', key: 'courseId', width: 100 },
    {
        title: '操作',
        key: 'actions',
        render(row) {
            return h(
                NButton,
                {
                    size: 'small',
                    onClick: () => openPreview(row)
                },
                { default: () => '预览' }
            )
        }
    }
]

const pagination = { pageSize: 10 }

// --- 方法 ---

// 上传完成
const handleUploadFinish = ({ file, event }) => {
    message.success(`文件 ${file.name} 上传成功并已索引`)
    handleSearch() // 刷新列表
}

const handleUploadError = () => {
    message.error('上传失败')
}

// 搜索文件
const handleSearch = async () => {
    if (!searchKeyword.value) return
    loading.value = true
    try {
        const res = await axios.get(`${API_BASE}/course/search`, {
            params: { keyword: searchKeyword.value }
        })
        fileList.value = res.data
    } catch (error) {
        message.error('搜索失败')
    } finally {
        loading.value = false
    }
}

// 打开预览
const openPreview = async (row) => {
    try {
        const res = await axios.get(`${API_BASE}/course/preview/${row.fileUrl}`)
        const url = res.data.url
        previewUrl.value = url

        // 简单判断文件类型
        const ext = row.title.split('.').pop().toLowerCase()
        if (['mp4', 'webm', 'ogg', 'mp3'].includes(ext)) {
            previewType.value = 'video'
        } else {
            previewType.value = 'iframe' // PDF, 图片等浏览器原生支持的格式
        }

        showPreview.value = true
    } catch (error) {
        message.error('获取预览链接失败')
    }
}
</script>

<style scoped>
.course-manager {
    padding: 20px;
}

.preview-container {
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
}
</style>
