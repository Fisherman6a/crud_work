<template>
    <n-card>
        <n-space vertical>
            <n-input-group>
                <n-input v-model:value="searchStudentId" placeholder="输入学号查询" />
                <n-button type="primary" @click="fetchTimetable">查询</n-button>
            </n-input-group>

            <n-divider />

            <div v-if="hasData">
                <n-space justify="end">
                    <n-switch v-model:value="isEditMode">
                        <template #checked>编辑模式</template>
                        <template #unchecked>查看模式</template>
                    </n-switch>
                    <n-button v-if="isEditMode" type="success" @click="saveChanges">保存更改</n-button>
                </n-space>

                <time-table :courses="currentCourses" :editable="isEditMode" @cell-click="handleEdit" />
            </div>
        </n-space>
    </n-card>

    <n-modal v-model:show="showAddModal">
        <n-card title="添加课程" style="width: 500px">
            <n-select :options="availableCoursesOptions" />
            <n-button @click="confirmAdd">确认添加</n-button>
        </n-card>
    </n-modal>
</template>

<script setup>
import { ref } from 'vue'
import TimeTable from '../components/TimeTable.vue'
import axios from 'axios' // 假设已配置

const searchStudentId = ref('')
const currentCourses = ref([])
const hasData = ref(false)
const isEditMode = ref(false)
const showAddModal = ref(false)

const fetchTimetable = async () => {
    // 调用后端 GET /timetable/student/{id}
    // const res = await axios.get(...)
    // currentCourses.value = res.data
    // 模拟数据：
    currentCourses.value = [
        { week_day: 1, section_start: 1, section_end: 2, name: '高等数学', location: 'A101' }
    ]
    hasData.value = true
}

const handleEdit = ({ day, section, course }) => {
    if (course) {
        if (confirm(`确定要移除 ${course.name} 吗？`)) {
            // 前端先移除，保存时再提交，或者直接调用API移除
        }
    } else {
        // 点击了空白处，弹出选课框
        showAddModal.value = true
    }
}
</script>
