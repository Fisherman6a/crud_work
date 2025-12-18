<template>
    <n-modal
        v-model:show="showModal"
        :style="{ width: modalWidth }"
        preset="card"
        :title="file?.resourceName || '文件预览'"
        :bordered="false"
        :segmented="{ content: 'soft' }"
    >
        <template #header-extra>
            <n-button text @click="handleClose">
                <template #icon>
                    <n-icon :component="CloseOutline" />
                </template>
            </n-button>
        </template>

        <div class="preview-container">
            <!-- PDF预览 -->
            <div v-if="isPDF" class="pdf-preview">
                <iframe
                    :src="file?.previewUrl"
                    frameborder="0"
                    style="width: 100%; height: 600px;"
                ></iframe>
            </div>

            <!-- 视频预览 -->
            <div v-else-if="isVideo" class="video-preview">
                <video
                    controls
                    :src="file?.previewUrl"
                    style="width: 100%; max-height: 600px;"
                    @error="handleMediaError"
                >
                    您的浏览器不支持视频播放
                </video>
            </div>

            <!-- 音频预览 -->
            <div v-else-if="isAudio" class="audio-preview">
                <div class="audio-wrapper">
                    <n-icon :component="MusicalNotesOutline" size="100" color="#18a058" />
                    <audio
                        controls
                        :src="file?.previewUrl"
                        style="width: 100%; margin-top: 20px;"
                        @error="handleMediaError"
                    >
                        您的浏览器不支持音频播放
                    </audio>
                </div>
            </div>

            <!-- Word/PPT文档预览 (使用Office Online Viewer或提示下载) -->
            <div v-else-if="isDocument" class="document-preview">
                <n-result
                    status="info"
                    title="文档预览"
                    description="Word和PPT文件需要下载后查看，或使用在线预览服务"
                >
                    <template #icon>
                        <n-icon :component="DocumentTextOutline" size="80" />
                    </template>
                    <template #footer>
                        <n-space>
                            <n-button type="primary" @click="handleDownload">
                                <template #icon>
                                    <n-icon :component="DownloadOutline" />
                                </template>
                                下载文件
                            </n-button>
                            <n-button @click="openInOfficeViewer">
                                <template #icon>
                                    <n-icon :component="EyeOutline" />
                                </template>
                                使用Office Online预览
                            </n-button>
                        </n-space>
                    </template>
                </n-result>
            </div>

            <!-- 不支持的文件类型 -->
            <div v-else class="unsupported-preview">
                <n-result
                    status="warning"
                    title="不支持预览"
                    description="该文件类型暂不支持在线预览，请下载后查看"
                >
                    <template #icon>
                        <n-icon :component="WarningOutline" size="80" />
                    </template>
                    <template #footer>
                        <n-button type="primary" @click="handleDownload">
                            <template #icon>
                                <n-icon :component="DownloadOutline" />
                            </template>
                            下载文件
                        </n-button>
                    </template>
                </n-result>
            </div>
        </div>
    </n-modal>
</template>

<script setup>
import { computed, watch, ref } from 'vue'
import { NModal, NButton, NIcon, NResult, NSpace, useMessage } from 'naive-ui'
import {
    CloseOutline, DocumentTextOutline, MusicalNotesOutline,
    DownloadOutline, EyeOutline, WarningOutline
} from '@vicons/ionicons5'

const props = defineProps({
    show: {
        type: Boolean,
        default: false
    },
    file: {
        type: Object,
        default: null
    }
})

const emit = defineEmits(['update:show'])
const message = useMessage()

const showModal = computed({
    get: () => props.show,
    set: (val) => emit('update:show', val)
})

// 文件类型判断
const fileType = computed(() => props.file?.resourceType?.toLowerCase() || '')

const isPDF = computed(() => fileType.value === 'pdf')
const isVideo = computed(() => ['mp4', 'avi', 'mov', 'webm'].includes(fileType.value))
const isAudio = computed(() => ['mp3', 'wav', 'ogg', 'm4a'].includes(fileType.value))
const isDocument = computed(() => ['doc', 'docx', 'ppt', 'pptx'].includes(fileType.value))

// 动态Modal宽度
const modalWidth = computed(() => {
    if (isAudio.value) return '600px'
    if (isDocument.value) return '700px'
    return '90vw'
})

// 关闭Modal
const handleClose = () => {
    showModal.value = false
}

// 下载文件
const handleDownload = () => {
    if (props.file?.id) {
        window.open(`http://localhost:8080/resource/download/${props.file.id}`, '_blank')
        message.success('开始下载')
    }
}

// 使用Office Online Viewer预览
const openInOfficeViewer = () => {
    if (props.file?.previewUrl) {
        const viewerUrl = `https://view.officeapps.live.com/op/view.aspx?src=${encodeURIComponent(props.file.previewUrl)}`
        window.open(viewerUrl, '_blank')
    } else {
        message.warning('无法获取文件链接')
    }
}

// 媒体加载错误处理
const handleMediaError = (e) => {
    console.error('Media load error:', e)
    message.error('文件加载失败，请检查文件格式或网络连接')
}
</script>

<style scoped>
.preview-container {
    min-height: 400px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.pdf-preview,
.video-preview {
    width: 100%;
}

.audio-preview {
    width: 100%;
    padding: 40px 0;
}

.audio-wrapper {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.document-preview,
.unsupported-preview {
    width: 100%;
    padding: 20px 0;
}
</style>
