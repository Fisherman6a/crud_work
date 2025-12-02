import { createRouter, createWebHistory } from 'vue-router'
import Login from '../views/Login.vue'
import Manager from '../views/Manager.vue'

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
      path: '/student',
      name: 'student',
      component: Manager
    }
  ]
})

// 路由守卫：检查是否有 token
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
