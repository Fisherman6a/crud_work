<template>
    <div class="my-timetable">
        <n-card title="我的课程表">
            <template #header-extra>
                <n-button @click="fetchMyCourses" :loading="loading">
                    <template #icon>
                        <n-icon :component="RefreshOutline" />
                    </template>
                    刷新
                </n-button>
            </template>
            <n-spin :show="loading">
                <time-table :courses="myCourses" :editable="false" />
            </n-spin>
        </n-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TimeTable from './TimeTable.vue'
import { NCard, NButton, NIcon, NSpin, useMessage } from 'naive-ui'
import { RefreshOutline } from '@vicons/ionicons5'
import axios from 'axios'

const myCourses = ref([])
const loading = ref(false)
const message = useMessage()
const studentId = localStorage.getItem('username') // 假设存的是学号，或者专门存一个 studentId

const fetchMyCourses = async () => {
    loading.value = true
    try {
        console.log('🔄 加载学生课表, 学号:', studentId)

        // 使用与StudentCourse.vue相同的API端点
        const res = await axios.get(`http://localhost:8080/api/timetable/student/${studentId}`)

        console.log('✅ 课表API返回:', res.data)

        // 数据映射: 后端返回 snake_case, TimeTable组件需要 snake_case
        myCourses.value = (res.data.data || []).map(item => ({
            week_day: item.week_day,
            section_start: item.section_start,
            section_end: item.section_end,
            name: item.course_name,
            location: item.location,
            teacher: item.teacher_name,
            credit: item.credit
        }))

        console.log('✅ 课表数据映射完成:', myCourses.value.length, '门课程')
        message.success('课表刷新成功')
    } catch (e) {
        console.error('❌ 课表加载失败:', e)
        message.error('课表加载失败: ' + (e.message || ''))
    } finally {
        loading.value = false
    }
}

onMounted(() => {
    fetchMyCourses()
})
</script>

<style scoped>
.my-timetable {
    padding: 12px;
}
</style>
