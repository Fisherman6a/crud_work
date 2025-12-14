<template>
    <div class="timetable">
        <div v-if="editable" class="toolbar">
            <n-alert type="info">编辑模式：点击空白格子添加课程，点击已有课程进行删除。</n-alert>
        </div>

        <table class="course-table">
            <thead>
                <tr>
                    <th>节次/星期</th>
                    <th v-for="i in 7" :key="i">周{{ i }}</th>
                </tr>
            </thead>
            <tbody>
                <tr v-for="section in 8" :key="section">
                    <td class="section-idx">第 {{ section }} 节</td>
                    <td v-for="day in 7" :key="day" class="course-cell" @click="handleCellClick(day, section)">
                        <div v-if="getCourse(day, section)" class="course-content"
                            :style="{ backgroundColor: getColor(getCourse(day, section).name) }">
                            <strong>{{ getCourse(day, section).name }}</strong>
                            <br />
                            <span style="font-size:12px">{{ getCourse(day, section).location }}</span>
                        </div>
                    </td>
                </tr>
            </tbody>
        </table>
    </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
    courses: Array, // [{week_day: 1, section_start: 1, name: 'Java', ...}]
    editable: Boolean
})

const emit = defineEmits(['cell-click'])

// 辅助函数：根据坐标获取课程
const getCourse = (day, section) => {
    return props.courses.find(c => c.week_day === day && section >= c.section_start && section <= c.section_end)
}

// 简单的颜色生成
const getColor = (str) => {
    // 省略：根据课程名生成固定颜色的逻辑
    return '#e6f7ff'
}

const handleCellClick = (day, section) => {
    if (!props.editable) return;
    const existing = getCourse(day, section);
    emit('cell-click', { day, section, course: existing });
}
</script>

<style scoped>
.course-table {
    width: 100%;
    border-collapse: collapse;
}

.course-table th,
.course-table td {
    border: 1px solid #eee;
    height: 80px;
    text-align: center;
    vertical-align: middle;
    width: 12.5%;
}

.course-content {
    background-color: #e6f7ff;
    padding: 4px;
    border-radius: 4px;
    height: 100%;
    cursor: pointer;
}

.course-cell:hover {
    background-color: #fafafa;
}
</style>
