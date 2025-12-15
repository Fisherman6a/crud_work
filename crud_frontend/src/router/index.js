import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import MainLayout from '../layout/MainLayout.vue'

// 引入业务页面
// 注意：确保这些文件实际存在于 views 目录下
import Manager from '../views/Manager.vue'
import CourseManager from '../views/CourseManager.vue'
import TeacherManager from '../views/TeacherManager.vue'
import SelectionManage from '../views/SelectionManage.vue'
import StudentCourse from '../views/StudentCourse.vue'
// 如果 TimeTable.vue 在 views 下，请从 views 引入
import TimeTable from '../components/TimeTable.vue'
import AdminTimetable from '../components/AdminTimetable.vue'
import MyTimetable from '../components/MyTimetable.vue'
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'login',
      component: Login
    },
    {
      // 主应用布局
      path: '/app',
      component: MainLayout,
      // 这里的 redirect 需要根据登录角色动态判断，或者默认跳到一个通用页
      redirect: '/app/student',
      children: [
        // --- 管理员路由 ---
        {
          path: 'student', // /app/student
          name: 'student',
          component: Manager,
          meta: { title: '学生管理', roles: ['admin'] }
        },
        {
          path: 'course-manager', // /app/course-manager
          name: 'course-manager',
          component: CourseManager,
          meta: { title: '课程资料', roles: ['admin'] }
        },
        {
          path: 'selection-manage', // /app/selection-manage
          name: 'selection-manage',
          component: SelectionManage,
          meta: { title: '选课管理', roles: ['admin'] }
        },
        {
          path: 'timetable',
          name: 'timetable',
          component: AdminTimetable, // ✅ 替换 TimeTable 为 AdminTimetable
          meta: { title: '排课总表', roles: ['admin'] }
        },
        {
          path: 'teacher-manager', // 访问路径 /app/teacher-manager
          name: 'teacher-manager',
          component: TeacherManager,
          meta: { title: '教师管理', roles: ['admin'] } // 仅管理员可见
        },
        // --- 学生路由 ---
        {
          path: 'student-course', // /app/student-course
          name: 'student-course',
          component: StudentCourse,
          meta: { title: '我的选课', roles: ['user'] }
        },
        {
          path: 'my-timetable', // /app/my-timetable
          name: 'my-timetable',
          // 复用 TimeTable 组件，但最好封装一个 MyTimetable.vue 来传数据
          component: TimeTable,
          meta: { title: '课程表', roles: ['user'] }
        }
      ]
    }
  ]
})

// 简单的路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
