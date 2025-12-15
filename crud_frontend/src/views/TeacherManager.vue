<template>
    <n-card title="教师信息管理">
        <n-space vertical>
            <n-space justify="space-between">
                <n-input-group>
                    <n-input v-model:value="searchText" placeholder="搜索教师姓名" />
                    <n-button type="primary" @click="fetchTeachers">搜索</n-button>
                </n-input-group>
                <n-button type="primary" @click="openAddModal">
                    <template #icon><span style="font-size:16px">+</span></template>
                    添加教师
                </n-button>
            </n-space>

            <n-data-table :columns="columns" :data="teacherList" :pagination="pagination" :loading="loading" />
        </n-space>
    </n-card>

    <n-modal v-model:show="showModal">
        <n-card style="width: 500px" :title="isEdit ? '编辑教师' : '添加教师'" :bordered="false" size="huge">
            <n-form ref="formRef" :model="form" label-placement="left" label-width="80"
                require-mark-placement="right-hanging">
                <n-form-item label="姓名" path="name">
                    <n-input v-model:value="form.name" placeholder="请输入姓名" />
                </n-form-item>
                <n-form-item label="职称" path="title">
                    <n-select v-model:value="form.title" :options="titleOptions" placeholder="请选择职称" />
                </n-form-item>
                <n-form-item label="电话" path="phone">
                    <n-input v-model:value="form.phone" placeholder="请输入联系电话" />
                </n-form-item>
            </n-form>
            <template #footer>
                <n-space justify="end">
                    <n-button @click="showModal = false">取消</n-button>
                    <n-button type="primary" @click="handleSubmit">确认提交</n-button>
                </n-space>
            </template>
        </n-card>
    </n-modal>
</template>

<script setup>
import { ref, h, onMounted, reactive } from 'vue'
import { NButton, NSpace, useMessage } from 'naive-ui'
import axios from 'axios'

const message = useMessage()
const loading = ref(false)
const showModal = ref(false)
const isEdit = ref(false)
const searchText = ref('')
const teacherList = ref([])

// 表单数据
const form = reactive({
    id: null,
    name: '',
    title: '讲师',
    phone: ''
})

// 职称选项（可根据需求修改）
const titleOptions = [
    { label: '助教', value: '助教' },
    { label: '讲师', value: '讲师' },
    { label: '副教授', value: '副教授' },
    { label: '教授', value: '教授' }
]

// 表格列定义
const columns = [
    { title: 'ID', key: 'id', width: 80 },
    { title: '姓名', key: 'name' },
    { title: '职称', key: 'title' },
    { title: '联系电话', key: 'phone' },
    {
        title: '操作',
        key: 'actions',
        render(row) {
            return h(NSpace, null, {
                default: () => [
                    h(NButton, {
                        size: 'small',
                        type: 'info',
                        onClick: () => handleEdit(row)
                    }, { default: () => '编辑' }),
                    h(NButton, {
                        size: 'small',
                        type: 'error',
                        onClick: () => handleDelete(row)
                    }, { default: () => '删除' })
                ]
            })
        }
    }
]

// 分页配置
const pagination = reactive({
    pageSize: 10,
    itemCount: 0,
    onChange: (page) => {
        pagination.page = page
        fetchTeachers() // 换页重新请求
    }
})

// --- API 方法 ---

// 1. 获取列表
const fetchTeachers = async () => {
    loading.value = true
    try {
        // 对应 AdminBasicController.java 的 listTeachers
        const res = await axios.get('http://localhost:8080/api/admin/teacher/list', {
            params: {
                page: pagination.page || 1,
                size: pagination.pageSize,
                name: searchText.value // 如果后端支持搜索
            }
        })
        if (res.data.code === 200) {
            teacherList.value = res.data.data
            pagination.itemCount = res.data.total
        }
    } catch (e) {
        message.error('获取教师列表失败')
    } finally {
        loading.value = false
    }
}

// 2. 提交添加/修改
const handleSubmit = async () => {
    if (!form.name) return message.warning('请填写姓名')

    try {
        if (isEdit.value) {
            // TODO: 后端 AdminBasicController 暂时没提供 update 接口，需要你在后端补充
            message.warning('暂未实现编辑接口')
        } else {
            // 对应 AdminBasicController.java 的 addTeacher
            await axios.post('http://localhost:8080/api/admin/teacher', {
                name: form.name,
                title: form.title,
                phone: form.phone
            })
            message.success('添加成功')
        }
        showModal.value = false
        fetchTeachers()
    } catch (e) {
        message.error('操作失败')
    }
}

// 3. 删除
const handleDelete = async (row) => {
    if (confirm(`确定要删除教师 ${row.name} 吗？`)) {
        // TODO: 后端 AdminBasicController 暂时没提供 delete 接口，需要你在后端补充
        message.success('模拟删除成功 (需完善后端)')
        // await axios.delete(...)
        // fetchTeachers()
    }
}

// 交互方法
const openAddModal = () => {
    isEdit.value = false
    form.id = null
    form.name = ''
    form.phone = ''
    form.title = '讲师'
    showModal.value = true
}

const handleEdit = (row) => {
    isEdit.value = true
    Object.assign(form, row)
    showModal.value = true
}

onMounted(() => {
    fetchTeachers()
})
</script>
