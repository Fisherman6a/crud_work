<template>
    <div class="my-timetable">
        <n-card title="我的课程表">
            <time-table :courses="myCourses" :editable="false" />
        </n-card>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import TimeTable from './TimeTable.vue'
import { useMessage } from 'naive-ui'
import axios from 'axios'

const myCourses = ref([])
const message = useMessage()
const studentId = localStorage.getItem('username') // 假设存的是学号，或者专门存一个 studentId

const fetchMyCourses = async () => {
    try {
        // 对应后端 StudentCourseController.java 中的接口
        // 目前后端好像只有 /student-course/calendar，建议新增一个 /student-course/schedule 返回标准课表结构
        // const res = await axios.get(`http://localhost:8080/student-course/schedule?studentId=${studentId}`)
        // myCourses.value = res.data

        // 模拟数据
        myCourses.value = [
            { week_day: 2, section_start: 1, section_end: 2, name: '高等数学', location: 'B302' },
            { week_day: 4, section_start: 5, section_end: 6, name: '大学英语', location: 'D101' }
        ]
    } catch (e) {
        message.error('课表加载失败')
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
