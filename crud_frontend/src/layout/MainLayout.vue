<template>
    <n-layout has-sider position="absolute">

        <n-layout-sider bordered collapse-mode="width" :collapsed-width="64" :width="220" show-trigger
            :native-scrollbar="false" class="app-sider">
            <div class="logo">
                <n-gradient-text type="primary" size="20">
                    教务系统
                </n-gradient-text>
            </div>

            <n-menu :options="menuOptions" :value="activeKey" @update:value="handleMenuUpdate" />
        </n-layout-sider>

        <n-layout>
            <n-layout-header>
                <TopBar />
            </n-layout-header>

            <n-layout-content class="app-content">
                <router-view />
            </n-layout-content>
        </n-layout>

    </n-layout>
</template>

<script setup>
import { ref, h, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
    NIcon,
    NLayout,
    NLayoutSider,
    NLayoutHeader,
    NLayoutContent,
    NMenu,
    NGradientText
} from 'naive-ui'
import TopBar from '../components/TopBar.vue' // 引入刚才写的 TopBar
import {
    BookOutline,
    PeopleOutline,
    CalendarOutline,
    GridOutline,
    IdCardOutline,
    TimeOutline, // 添加一个新图标用于学生排课
    FolderOpenOutline // 课程资料图标
} from '@vicons/ionicons5'

const router = useRouter()
const route = useRoute()
const activeKey = ref(route.name) // 保持菜单高亮与当前路由一致

// 根据角色动态生成菜单
const role = localStorage.getItem('role')

function renderIcon(icon) {
    return () => h(NIcon, null, { default: () => h(icon) })
}

// 管理员菜单
const adminMenu = [
    { label: '学生管理', key: 'student', icon: renderIcon(PeopleOutline) },
    { label: '教师管理', key: 'teacher-manager', icon: renderIcon(IdCardOutline) },

    // 原 selection-manage -> 现 "课程管理"
    { label: '课程管理', key: 'selection-manage', icon: renderIcon(GridOutline) },

    // 原 course-manager -> 现 "课程资料"
    { label: '课程资料', key: 'course-manager', icon: renderIcon(BookOutline) },

    // 新增：排课管理（课程排课系统）
    { label: '排课管理', key: 'schedule-manager', icon: renderIcon(CalendarOutline) },

    // 原 timetable -> 现 "学生排课"（为学生安排课程）
    { label: '学生排课', key: 'timetable', icon: renderIcon(TimeOutline) }
]

// 学生菜单
const studentMenu = [
    { label: '我的选课', key: 'student-course', icon: renderIcon(BookOutline) },   // 对应 StudentCourse.vue
    { label: '课程表', key: 'my-timetable', icon: renderIcon(CalendarOutline) },
    { label: '课程资料', key: 'student-materials', icon: renderIcon(FolderOpenOutline) }
]

const menuOptions = computed(() => (role === 'admin' || role === 'ADMIN') ? adminMenu : studentMenu)

const handleMenuUpdate = (key) => {
    activeKey.value = key
    router.push({ name: key })
}
</script>

<style scoped>
.logo {
    height: 64px;
    display: flex;
    justify-content: center;
    align-items: center;
    border-bottom: 1px solid #eee;
}

.app-content {
    background-color: #f5f7f9;
    /* 浅灰背景，衬托 PageContainer 的白卡片 */
    height: calc(100vh - 64px);
    /* 减去 TopBar 高度 */
}
</style>
