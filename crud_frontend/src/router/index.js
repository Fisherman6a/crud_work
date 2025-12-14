import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import MainLayout from '../layout/MainLayout.vue' // 引入布局组件

// 引入你的业务页面
import Manager from '../views/Manager.vue'
import CourseManager from '../views/CourseManager.vue'
import SelectionManage from '../views/SelectionManage.vue'
import StudentCourse from '../views/StudentCourse.vue'

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
      // 核心修改：定义一个父路由使用 MainLayout
      path: '/app',
      component: MainLayout,
      redirect: '/app/student', // 默认跳到学生管理
      children: [
        {
          path: 'student', // 访问地址: /app/student
          name: 'student',
          component: Manager
        },
        {
          path: 'course-manager', // 访问地址: /app/course-manager
          name: 'course-manager',
          component: CourseManager
        },
        // 这里继续添加你的其他页面...
        // { path: 'selection', name: 'selection-manage', component: SelectionManage },
        // { path: 'my-course', name: 'student-course', component: StudentCourse },
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
