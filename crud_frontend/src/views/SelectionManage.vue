<template>
    <PageContainer>
        <div class="header">
            <n-h2 style="margin: 0;">📚 课程管理</n-h2>
        </div>

        <n-space justify="space-between" style="margin-bottom: 20px;">
            <n-input-group>
                <n-input v-model:value="searchText" placeholder="输入课程名搜索" clearable @keydown.enter="loadData" />
                <n-button type="primary" @click="loadData">查询</n-button>
            </n-input-group>
            <n-button type="primary" @click="openModal(null)">
                <template #icon>➕</template>
                新增课程
            </n-button>
        </n-space>

        <n-data-table :columns="columns" :data="tableData" :loading="loading" :bordered="true" />

        <n-space justify="end" style="margin-top: 20px;">
            <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
                :item-count="pagination.itemCount" show-size-picker :page-sizes="[10, 20, 50]" @update:page="loadData"
                @update:page-size="loadData" />
        </n-space>

        <!-- 课程编辑弹窗 -->
        <n-modal v-model:show="showModal" preset="card" :title="form.id ? '编辑课程' : '新增课程'" style="width: 600px;">
            <n-form :model="form" label-width="90px" label-placement="left">
                <n-form-item label="课程ID" path="id">
                    <n-input-number v-model:value="form.id" :disabled="!!originalId" placeholder="请输入课程ID"
                        style="width: 100%" :show-button="false" />
                </n-form-item>
                <n-form-item label="课程名称" path="name">
                    <n-input v-model:value="form.name" placeholder="请输入课程名称" />
                </n-form-item>
                <n-form-item label="课程描述" path="description">
                    <n-input v-model:value="form.description" type="textarea" placeholder="请输入课程描述" :rows="3" />
                </n-form-item>
                <n-form-item label="任课老师" path="teacherIds">
                    <n-select v-model:value="form.teacherIds" :options="teacherOptions" multiple
                        placeholder="选择任课老师（可多选）" clearable filterable />
                </n-form-item>
                <n-form-item label="学分" path="credit">
                    <n-input-number v-model:value="form.credit" :min="0" :max="10" placeholder="请输入学分"
                        style="width: 100%" />
                </n-form-item>
            </n-form>
            <template #footer>
                <n-space justify="end">
                    <n-button @click="showModal = false">取消</n-button>
                    <n-button type="primary" @click="handleSubmit">保存</n-button>
                </n-space>
            </template>
        </n-modal>

        <!-- 教师管理弹窗 -->
        <n-modal v-model:show="showTeacherModal" preset="card" title="管理任课老师" style="width: 500px;">
            <n-space vertical>
                <n-text>当前任课老师：</n-text>
                <n-space v-if="currentCourseTeachers.length > 0">
                    <n-tag v-for="teacher in currentCourseTeachers" :key="teacher.id" closable
                        @close="removeTeacherFromCourse(teacher.id)">
                        {{ teacher.name }} ({{ teacher.title }})
                    </n-tag>
                </n-space>
                <n-text v-else depth="3">暂无任课老师</n-text>

                <n-divider />

                <n-text>添加任课老师：</n-text>
                <n-space>
                    <n-select v-model:value="selectedTeacherId" :options="teacherOptions" placeholder="选择教师"
                        style="width: 300px" filterable />
                    <n-button type="primary" @click="addTeacherToCourse">添加</n-button>
                </n-space>
            </n-space>
        </n-modal>
    </PageContainer>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NSpace, NInputGroup, NInput, NButton, NDataTable, NModal, NForm, NFormItem, NH2, NInputNumber, NPagination, NSelect, NTag, NText, NDivider, useMessage, NPopconfirm } from 'naive-ui'
import PageContainer from '../components/PageContainer.vue'
import axios from 'axios'

const message = useMessage()
const loading = ref(false)
const searchText = ref('')
const showModal = ref(false)
const showTeacherModal = ref(false)
const tableData = ref([])
const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0 })
const teacherOptions = ref([])
const selectedTeacherId = ref(null)
const currentCourseId = ref(null)
const currentCourseTeachers = ref([])
const originalId = ref(null) // Track original ID for edit mode

const form = reactive({ id: null, name: '', description: '', teacherIds: [], credit: 0 })

const API_URL = 'http://localhost:8080/course'

// 列定义
const columns = [
    { title: '课程ID', key: 'id', width: 120 },
    { title: '课程名称', key: 'name', width: 150 },
    { title: '课程描述', key: 'description', ellipsis: { tooltip: true } },
    {
        title: '任课老师',
        key: 'teachers',
        width: 250,
        render(row) {
            if (!row.teachers || row.teachers.length === 0) {
                return h('span', { style: { color: '#999' } }, '暂无')
            }
            return h(NSpace, { size: 'small' }, {
                default: () => [
                    ...row.teachers.map(t => h(NTag, { size: 'small', type: 'info' }, { default: () => t.name })),
                    h(NButton, {
                        size: 'tiny',
                        type: 'primary',
                        text: true,
                        onClick: () => openTeacherModal(row)
                    }, { default: () => '管理' })
                ]
            })
        }
    },
    { title: '学分', key: 'credit', width: 80 },
    {
        title: '操作',
        key: 'actions',
        width: 180,
        render(row) {
            return h(NSpace, null, {
                default: () => [
                    h(NButton, { size: 'small', onClick: () => openModal(row) }, { default: () => '编辑' }),
                    h(NPopconfirm, { onPositiveClick: () => handleDelete(row.id) }, {
                        trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
                        default: () => '确定要删除这门课程吗？'
                    })
                ]
            })
        }
    }
]

// 加载教师列表
const loadTeachers = async () => {
    try {
        const res = await axios.get('http://localhost:8080/teacher/list')
        teacherOptions.value = res.data.map(t => ({
            label: `${t.name} (${t.title})`,
            value: t.id
        }))
    } catch (e) {
        console.error('加载教师列表失败', e)
    }
}

// 加载课程数据
const loadData = async () => {
    loading.value = true
    try {
        const res = await axios.get(`${API_URL}/page`, {
            params: { pageNum: pagination.page, pageSize: pagination.pageSize, search: searchText.value }
        })
        if (res.data && res.data.records) {
            tableData.value = res.data.records
            pagination.itemCount = res.data.total
        }
    } catch (e) {
        message.error('加载课程失败')
    } finally {
        loading.value = false
    }
}

// 打开编辑/新增弹窗
const openModal = (row) => {
    if (row) {
        // Edit mode
        form.id = row.id
        form.name = row.name
        form.description = row.description
        form.credit = row.credit
        form.teacherIds = row.teachers ? row.teachers.map(t => t.id) : []
        originalId.value = row.id // Store original ID
    } else {
        // Add mode
        form.id = null
        form.name = ''
        form.description = ''
        form.credit = 0
        form.teacherIds = []
        originalId.value = null
    }
    showModal.value = true
}

// 打开教师管理弹窗
const openTeacherModal = (row) => {
    currentCourseId.value = row.id
    currentCourseTeachers.value = row.teachers || []
    selectedTeacherId.value = null
    showTeacherModal.value = true
}

// 为课程添加教师
const addTeacherToCourse = async () => {
    if (!selectedTeacherId.value) {
        message.warning('请选择教师')
        return
    }

    try {
        const res = await axios.post(`${API_URL}/${currentCourseId.value}/teacher/${selectedTeacherId.value}`)
        if (res.data.success) {
            message.success('添加成功')
            selectedTeacherId.value = null
            await loadData()
            // 更新当前课程的教师列表
            const course = tableData.value.find(c => c.id === currentCourseId.value)
            if (course) {
                currentCourseTeachers.value = course.teachers || []
            }
        } else {
            message.error(res.data.message || '添加失败')
        }
    } catch (e) {
        message.error('添加失败')
    }
}

// 从课程删除教师
const removeTeacherFromCourse = async (teacherId) => {
    try {
        const res = await axios.delete(`${API_URL}/${currentCourseId.value}/teacher/${teacherId}`)
        if (res.data.success) {
            message.success('删除成功')
            await loadData()
            // 更新当前课程的教师列表
            const course = tableData.value.find(c => c.id === currentCourseId.value)
            if (course) {
                currentCourseTeachers.value = course.teachers || []
            }
        } else {
            message.error('删除失败')
        }
    } catch (e) {
        message.error('删除失败')
    }
}

// 提交保存
const handleSubmit = async () => {
    // Validate ID
    if (!form.id) {
        message.error('请输入课程ID')
        return
    }

    try {
        // 构建请求数据，包含 originalId 用于区分新增和编辑
        const requestData = {
            ...form,
            originalId: originalId.value  // 新增时为 null，编辑时为原始ID
        }
        const res = await axios.post(`${API_URL}/save`, requestData)
        if (res.data.success) {
            message.success('保存成功')
            showModal.value = false
            await loadData()
        } else {
            message.error(res.data.message || '保存失败')
        }
    } catch (e) {
        message.error('请求异常')
    }
}

// 删除课程
const handleDelete = async (id) => {
    try {
        const res = await axios.delete(`${API_URL}/${id}`)
        if (res.data === true) {
            message.success('已删除')
            await loadData()
        } else {
            message.error('删除失败')
        }
    } catch (e) {
        message.error('删除异常')
    }
}

onMounted(() => {
    loadTeachers()
    loadData()
})
</script>

<style scoped>
.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 24px;
    padding-bottom: 12px;
    border-bottom: 1px solid #f0f0f0;
}
</style>
