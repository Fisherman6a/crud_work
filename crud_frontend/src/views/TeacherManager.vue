<template>
    <PageContainer>
        <div class="header">
            <n-h2 style="margin: 0;">👨‍🏫 教师信息管理</n-h2>
        </div>

        <n-space justify="space-between" style="margin-bottom: 20px;">
            <n-input-group>
                <n-input v-model:value="searchText" placeholder="输入教师姓名搜索" clearable @keydown.enter="loadData" />
                <n-button type="primary" @click="loadData">查询</n-button>
            </n-input-group>

            <n-button type="primary" @click="openModal(null)">
                <template #icon>➕</template>
                新增教师
            </n-button>
        </n-space>

        <n-data-table :columns="columns" :data="tableData" :loading="loading" :bordered="true" />

        <n-space justify="end" style="margin-top: 20px;">
            <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
                :item-count="pagination.itemCount" show-size-picker :page-sizes="[10, 20, 50]" @update:page="loadData"
                @update:page-size="loadData" />
        </n-space>

        <n-modal v-model:show="showModal" preset="card" :title="form.id ? '编辑教师' : '新增教师'" style="width: 500px;">
            <n-form ref="formRef" :model="form" label-placement="left" label-width="80px">
                <n-form-item label="教师ID" path="id">
                    <n-input-number v-model:value="form.id" :disabled="!!originalId" placeholder="请输入教师ID"
                        style="width: 100%" :show-button="false" />
                </n-form-item>
                <n-form-item label="姓名" path="name">
                    <n-input v-model:value="form.name" placeholder="请输入姓名" />
                </n-form-item>
                <n-form-item label="职称" path="title">
                    <n-select v-model:value="form.title" :options="titleOptions" />
                </n-form-item>
                <n-form-item label="电话" path="phone">
                    <n-input v-model:value="form.phone" placeholder="请输入电话" />
                </n-form-item>
            </n-form>
            <template #footer>
                <n-space justify="end">
                    <n-button @click="showModal = false">取消</n-button>
                    <n-button type="primary" @click="handleSubmit">确定</n-button>
                </n-space>
            </template>
        </n-modal>
    </PageContainer>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { NSpace, NInputGroup, NInput, NButton, NDataTable, NModal, NForm, NFormItem, NH2, NInputNumber, NPagination, NSelect, useMessage, NPopconfirm } from 'naive-ui'
import PageContainer from '../components/PageContainer.vue'
import axios from 'axios'

const message = useMessage()
const loading = ref(false)
const searchText = ref('')
const showModal = ref(false)
const tableData = ref([])
const originalId = ref(null) // Track original ID for edit mode

const form = reactive({ id: null, name: '', title: '讲师', phone: '' })
const titleOptions = [
    { label: '助教', value: '助教' },
    { label: '讲师', value: '讲师' },
    { label: '副教授', value: '副教授' },
    { label: '教授', value: '教授' }
]

const pagination = reactive({
    page: 1, pageSize: 10, itemCount: 0,
    onChange: (p) => { pagination.page = p; loadData() }
})

const columns = [
    { title: 'ID', key: 'id', width: 120 },
    { title: '姓名', key: 'name' },
    { title: '职称', key: 'title' },
    { title: '联系电话', key: 'phone' },
    {
        title: '操作',
        key: 'actions',
        render(row) {
            return h(NSpace, null, {
                default: () => [
                    h(NButton, { size: 'small', onClick: () => openModal(row) }, { default: () => '编辑' }),
                    h(NPopconfirm, {
                        onPositiveClick: () => handleDelete(row),
                        default: () => '确定删除该教师吗？'
                    }, { trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }) })
                ]
            })
        }
    }
]

// API 基础路径
const API_URL = 'http://localhost:8080/teacher'

const loadData = async () => {
    loading.value = true
    try {
        // TeacherController 返回的是直接的 Page 对象，没有 code 包装
        const res = await axios.get(`${API_URL}/page`, {
            params: { pageNum: pagination.page, pageSize: pagination.pageSize, search: searchText.value }
        })
        // 适配逻辑：检查 records 是否存在
        if (res.data && res.data.records) {
            tableData.value = res.data.records
            pagination.itemCount = res.data.total
        }
    } catch (e) {
        message.error('加载失败')
    } finally { loading.value = false }
}

const openModal = (row) => {
    if (row) {
        Object.assign(form, row)
        originalId.value = row.id // Store original ID for edit mode
    } else {
        Object.assign(form, { id: null, name: '', title: '讲师', phone: '' })
        originalId.value = null
    }
    showModal.value = true
}

const handleSubmit = async () => {
    // Validate ID
    if (!form.id) {
        message.error('请输入教师ID')
        return
    }

    try {
        // 构建请求数据，包含 originalId 用于区分新增和编辑
        const requestData = {
            ...form,
            originalId: originalId.value  // 新增时为 null，编辑时为原始ID
        }
        // TeacherController /save 接口现在返回 boolean 或 Map
        const res = await axios.post(`${API_URL}/save`, requestData)
        if (res.data === true || res.data.success === true) {
            message.success('操作成功')
            showModal.value = false
            loadData()
        } else if (res.data.success === false) {
            message.error(res.data.message || '操作失败')
        } else {
            message.error('操作失败')
        }
    } catch (e) { message.error('请求错误') }
}

const handleDelete = async (row) => {
    try {
        const res = await axios.delete(`${API_URL}/${row.id}`)
        if (res.data === true) {
            message.success('删除成功')
            loadData()
        }
    } catch (e) { message.error('删除失败') }
}

onMounted(() => loadData())
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
