<template>
    <PageContainer>
        <div class="header">
            <n-h2 style="margin: 0;">🎓 学生信息管理</n-h2>
        </div>

        <n-space justify="space-between" style="margin-bottom: 20px;">
            <n-input-group>
                <n-input v-model:value="searchName" placeholder="输入姓名搜索" clearable @keydown.enter="loadData" />
                <n-button type="primary" @click="loadData">
                    查询
                </n-button>
            </n-input-group>

            <n-button type="primary" @click="openAddModal">
                <template #icon>➕</template>
                新增学生
            </n-button>
        </n-space>

        <n-data-table :columns="columns" :data="tableData" :loading="loading" :bordered="true" :single-line="false"
            min-height="300" />

        <n-space justify="end" style="margin-top: 20px;">
            <n-pagination v-model:page="pagination.page" v-model:page-size="pagination.pageSize"
                :item-count="pagination.itemCount" show-size-picker :page-sizes="[10, 20, 50]" @update:page="loadData"
                @update:page-size="loadData" />
        </n-space>

        <n-modal v-model:show="showModal" preset="card" :title="isEdit ? '编辑学生' : '新增学生'" style="width: 500px;">
            <n-form ref="formRef" :model="formModel" label-placement="left" label-width="80"
                require-mark-placement="right-hanging">
                <n-form-item label="学号" path="studentNumber">
                    <n-input v-model:value="formModel.studentNumber" :disabled="isEdit" placeholder="请输入学号" />
                </n-form-item>
                <n-form-item label="姓名" path="name">
                    <n-input v-model:value="formModel.name" placeholder="请输入姓名" />
                </n-form-item>
                <n-form-item label="性别" path="gender">
                    <n-radio-group v-model:value="formModel.gender">
                        <n-radio value="男">男</n-radio>
                        <n-radio value="女">女</n-radio>
                    </n-radio-group>
                </n-form-item>
                <n-form-item label="年龄" path="age">
                    <n-input-number v-model:value="formModel.age" :min="1" :max="100" />
                </n-form-item>
                <n-form-item label="班级" path="className">
                    <n-input v-model:value="formModel.className" placeholder="例如: 计科2101" />
                </n-form-item>
                <n-form-item label="入学时间" path="createTime">
                    <n-date-picker v-model:value="formModel.createTime" type="date" placeholder="选择入学日期"
                        style="width: 100%;" clearable />
                </n-form-item>
            </n-form>

            <template #footer>
                <n-space justify="end">
                    <n-button @click="showModal = false">取消</n-button>
                    <n-button type="primary" @click="handleSave">确定</n-button>
                </n-space>
            </template>
        </n-modal>
    </PageContainer>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import {
    NButton, NSpace, NTag, NPopconfirm, useMessage,
    NModal, NForm, NFormItem, NInput, NInputNumber,
    NRadioGroup, NRadio, NPagination, NDataTable,
    NH2, NInputGroup, NDatePicker
} from 'naive-ui'
import PageContainer from '../components/PageContainer.vue' // 注意这里用相对路径，避免 @ 报错
import axios from 'axios'

const message = useMessage()

// --- 状态定义 ---
const loading = ref(false)
const searchName = ref('')
const showModal = ref(false) // 控制弹窗显示
const isEdit = ref(false)
const tableData = ref([])

const pagination = reactive({
    page: 1,
    pageSize: 10,
    itemCount: 0
})

const formModel = reactive({
    studentNumber: '',
    name: '',
    gender: '男',
    age: 18,
    className: '',
    createTime: null
})

const API_BASE = 'http://localhost:8080/student'

// --- 表格列定义 ---
const columns = [
    { title: '学号', key: 'studentNumber', width: 150 },
    { title: '姓名', key: 'name', width: 120 },
    {
        title: '性别',
        key: 'gender',
        width: 80,
        render(row) {
            return h(NTag, { type: row.gender === '男' ? 'info' : 'error', size: 'small' }, { default: () => row.gender })
        }
    },
    { title: '年龄', key: 'age', width: 80 },
    { title: '班级', key: 'className' },
    // 显示时，把后端传来的 ISO 时间字符串美化一下
    {
        title: '入学时间',
        key: 'createTime',
        width: 180,
        render(row) {
            // 简单处理显示：取 "2023-11-28T10:00:00" 的前10位 "2023-11-28"
            return row.createTime ? row.createTime.substring(0, 10) : ''
        }
    },
    {
        title: '操作',
        key: 'actions',
        width: 180,
        render(row) {
            return h(NSpace, {}, {
                default: () => [
                    h(NButton, { size: 'small', onClick: () => openEditModal(row) }, { default: () => '编辑' }),
                    h(NPopconfirm, { onPositiveClick: () => handleDelete(row.studentNumber) }, {
                        trigger: () => h(NButton, { size: 'small', type: 'error' }, { default: () => '删除' }),
                        default: () => '确定要删除这位学生吗？'
                    })
                ]
            })
        }
    }
]

// --- 核心逻辑 ---
// 加载数据
const loadData = async () => {
    loading.value = true
    try {
        const res = await axios.get(`${API_BASE}/page`, {
            params: {
                pageNum: pagination.page,
                pageSize: pagination.pageSize,
                name: searchName.value
            }
        })
        tableData.value = res.data.records
        pagination.itemCount = res.data.total
    } catch (error) {
        message.error('数据加载失败：可能是后端没启动')
        console.error(error)
    } finally {
        loading.value = false
    }
}

// 打开新增
const openAddModal = () => {
    isEdit.value = false
    // 重置表单
    Object.assign(formModel, {
        studentNumber: '', name: '', gender: '男', age: 18, className: '', createTime: Date.now() // 默认给当前时间戳
    })
    showModal.value = true // 只要 NModal 引入了，这里变 true 弹窗就会出来
}

// 打开编辑
const openEditModal = (row) => {
    isEdit.value = true
    Object.assign(formModel, row)
    if (row.createTime) {
        formModel.createTime = new Date(row.createTime).getTime()
    } else {
        formModel.createTime = null
    }

    showModal.value = true
}

// 保存
const handleSave = async () => {
    try {
        // 拷贝一份数据用于提交，避免污染视图绑定的 formModel
        const submitData = { ...formModel }

        // ⚠️ 关键：将时间戳转回后端认识的 ISO 字符串 (yyyy-MM-ddTHH:mm:ss)
        if (submitData.createTime) {
            const date = new Date(submitData.createTime)
            // 处理时区偏移，保证传给后端的是本地时间
            // 简单的办法：拼接字符串
            const year = date.getFullYear()
            const month = String(date.getMonth() + 1).padStart(2, '0')
            const day = String(date.getDate()).padStart(2, '0')
            const hours = String(date.getHours()).padStart(2, '0')
            const minutes = String(date.getMinutes()).padStart(2, '0')
            const seconds = String(date.getSeconds()).padStart(2, '0')

            submitData.createTime = `${year}-${month}-${day}T${hours}:${minutes}:${seconds}`
        }

        let res
        if (isEdit.value) {
            res = await axios.put(API_BASE, submitData)
        } else {
            res = await axios.post(API_BASE, submitData)
        }

        if (res.data.code === 200) {
            message.success(res.data.msg)
            showModal.value = false
            loadData()
        } else {
            message.error(res.data.msg || '操作失败')
        }
    } catch (error) {
        console.error(error)
        message.error('请求异常')
    }
}

// 删除
const handleDelete = async (id) => {
    try {
        const res = await axios.delete(`${API_BASE}/${id}`)
        if (res.data && res.data.includes('成功')) {
            message.success('删除成功')
            loadData()
        } else {
            message.error('删除失败')
        }
    } catch (error) {
        message.error('删除异常')
    }
}

onMounted(() => {
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
