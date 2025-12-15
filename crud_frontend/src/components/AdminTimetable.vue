<template>
    <div class="admin-timetable">
        <n-card title="全校排课总表" style="margin-bottom: 16px">
            <template #header-extra>
                <n-button type="primary" size="small" @click="fetchData">刷新数据</n-button>
            </template>
            
            <n-alert type="info" style="margin-bottom: 12px">
                管理员模式：点击空白处可排课，点击已有课程可删除/修改。
            </n-alert>

            <time-table 
                :courses="allCourses" 
                :editable="true" 
                @cell-click="handleCellClick" 
            />
        </n-card>

        <n-modal v-model:show="showModal">
            <n-card style="width: 400px" title="排课设置">
                <n-form>
                    <n-form-item label="课程名称">
                        <n-input v-model:value="form.courseName" />
                    </n-form-item>
                    <n-form-item label="上课地点">
                        <n-input v-model:value="form.location" />
                    </n-form-item>
                    </n-form>
                <template #action>
                    <n-button type="primary" @click="submitSchedule">确认排课</n-button>
                </template>
            </n-card>
        </n-modal>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TimeTable from './TimeTable.vue' // 假设 TimeTable.vue 在 views 目录下
import { useMessage } from 'naive-ui'
import axios from 'axios'

const message = useMessage()
const allCourses = ref([])
const showModal = ref(false)
const form = ref({ courseName: '', location: '', weekDay: 1, sectionStart: 1 })

// 1. 获取所有排课数据
const fetchData = async () => {
    try {
        // 后端需要提供一个接口 GET /api/admin/schedule/list
        // 这里先用模拟数据，防止报错
        // const res = await axios.get('http://localhost:8080/api/admin/schedule/list')
        // allCourses.value = res.data.data
        
        allCourses.value = [
            { id: 1, week_day: 1, section_start: 1, section_end: 2, name: 'Java程序设计', location: 'C201' },
            { id: 2, week_day: 3, section_start: 3, section_end: 4, name: '数据库原理', location: 'A105' }
        ]
        message.success('排课数据已更新')
    } catch (e) {
        message.error('数据加载失败')
    }
}

// 2. 处理单元格点击
const handleCellClick = ({ day, section, course }) => {
    if (course) {
        if(confirm(`确认要删除 ${course.name} 的排课吗？`)) {
            // 调用删除接口
            message.success('模拟删除成功')
            // fetchData()
        }
    } else {
        // 空白处：新增排课
        form.value.weekDay = day
        form.value.sectionStart = section
        showModal.value = true
    }
}

const submitSchedule = () => {
    // 调用新增接口 POST /api/admin/schedule
    message.success(`已在周${form.value.weekDay}第${form.value.sectionStart}节添加 ${form.value.courseName}`)
    showModal.value = false
    // fetchData()
}

onMounted(() => {
    fetchData()
})
</script>

<style scoped>
.admin-timetable {
    padding: 12px;
}
</style>
