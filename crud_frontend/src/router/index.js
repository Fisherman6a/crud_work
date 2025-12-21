import { createRouter, createWebHistory } from 'vue-router'
import { useMessage } from 'naive-ui'
import Login from '../views/Login.vue'
import MainLayout from '../layout/MainLayout.vue'

// 引入业务页面
import Manager from '../views/Manager.vue'        // 学生管理
import CourseMaterial from '../views/CourseMaterial.vue' // 课程资料
import TeacherManager from '../views/TeacherManager.vue' // 教师管理
import SelectionManage from '../views/SelectionManage.vue' // 课程管理
import StudentCourse from '../views/StudentCourse.vue' // 学生端-我的选课
import StudentMaterials from '../views/StudentMaterials.vue' // 学生端-课程资料
import ScheduleManager from '../views/ScheduleManager.vue' // 排课管理

// 引入组件
import TimeTable from '../components/TimeTable.vue'      // 课表组件
import MyTimetable from '../components/MyTimetable.vue'  // 学生课表页面
import AdminTimetable from '../components/AdminTimetable.vue' // 管理员排课

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
      redirect: '/app/student', // 默认跳这里，具体根据 Login.vue 跳转逻辑决定
      children: [
        // --- 管理员路由 ---
        {
          path: 'student',
          name: 'student',
          component: Manager,
          meta: { title: '学生管理', roles: ['admin'] }
        },
        {
          // 对应侧边栏的 "教师管理"
          path: 'teacher-manager',
          name: 'teacher-manager',
          component: TeacherManager,
          meta: { title: '教师管理', roles: ['admin'] }
        },
        {
          // 对应侧边栏的 "课程管理" (原选课管理改名)
          // 这里复用 SelectionManage.vue，请确保你已经把我上一条回答里的代码贴进去了
          path: 'selection-manage',
          name: 'selection-manage',
          component: SelectionManage,
          meta: { title: '课程管理', roles: ['admin'] }
        },
        {
          // 对应侧边栏的 "课程资料"
          path: 'course-manager',
          name: 'course-manager',
          component: CourseMaterial,
          meta: { title: '课程资料', roles: ['admin'] }
        },
        {
          // 对应侧边栏的 "排课管理"（新的排课系统）
          path: 'schedule-manager',
          name: 'schedule-manager',
          component: ScheduleManager,
          meta: { title: '排课管理', roles: ['admin'] }
        },
        {
          // 对应侧边栏的 "学生课表"（为学生安排课程）
          path: 'timetable',
          name: 'timetable',
          component: AdminTimetable,
          meta: { title: '学生排课', roles: ['admin'] }
        },

        // --- 学生路由 ---
        {
          path: 'student-course',
          name: 'student-course',
          component: StudentCourse,
          meta: { title: '我的选课', roles: ['user'] }
        },
        {
          path: 'my-timetable',
          name: 'my-timetable',
          component: MyTimetable,
          meta: { title: '课程表', roles: ['user'] }
        },
        {
          path: 'student-materials',
          name: 'student-materials',
          component: StudentMaterials,
          meta: { title: '课程资料', roles: ['user'] }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const role = localStorage.getItem('role')

  // 未登录跳转登录页
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
    return
  }

  // 角色权限验证
  if (to.meta.roles && to.meta.roles.length > 0) {
    const hasPermission = to.meta.roles.includes(role) ||
                          to.meta.roles.includes(role?.toUpperCase()) ||
                          to.meta.roles.includes(role?.toLowerCase())
    if (!hasPermission) {
      // 使用 Naive UI 的 message 需要在组件外部初始化
      console.warn('无权限访问该页面')
      next(false)
      return
    }
  }

  next()
})

export default router
