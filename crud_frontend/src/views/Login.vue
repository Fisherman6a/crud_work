<template>
  <div class="login-box">
    <n-card title="学生管理系统登录" style="width: 400px; box-shadow: 0 4px 12px rgba(0,0,0,0.1)">
      <n-form>
        <n-form-item label="用户名">
          <n-input v-model:value="form.username" placeholder="admin / user" />
        </n-form-item>
        <n-form-item label="密码">
          <n-input type="password" v-model:value="form.password" placeholder="请输入密码" />
        </n-form-item>

        <n-form-item label="验证码">
          <div style="display: flex; width: 100%; align-items: center; gap: 10px;">
            <n-input v-model:value="form.captchaCode" placeholder="请输入右侧字符" style="flex: 1;" />
            <img :src="captchaUrl" @click="loadCaptcha"
              style="cursor: pointer; height: 34px; border: 1px solid #ddd; border-radius: 4px;" />
          </div>
        </n-form-item>

        <n-button type="primary" block @click="handleLogin" :loading="loading">
          登录
        </n-button>
      </n-form>
    </n-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, NCard, NForm, NFormItem, NInput, NButton } from 'naive-ui'
import axios from 'axios'

const router = useRouter()
const message = useMessage()
const loading = ref(false)
// 修改 form 定义，增加验证码字段
const form = ref({
  username: '',
  password: '',
  captchaCode: '',
  captchaKey: ''
})

// 加载验证码函数
const loadCaptcha = async () => {
  try {
    const res = await axios.get('http://localhost:8080/captcha')
    if (res.data) {
      form.value.captchaKey = res.data.key
      captchaUrl.value = res.data.image
    }
  } catch (e) {
    message.error('验证码加载失败')
  }
}

// 存储图片 Base64
const captchaUrl = ref('')

// 页面加载时获取一次验证码
onMounted(() => {
  loadCaptcha()
})

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    message.warning('请输入用户名和密码')
    return
  }

  loading.value = true
  try {
    // 对应后端的 UserController
    const res = await axios.post('http://localhost:8080/login', form.value)

    if (res.data.code === 200) {
      message.success('登录成功')
      // 存储关键信息到浏览器
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('role', res.data.role)
      localStorage.setItem('username', res.data.username)
      // 注意：后端返回的角色通常是大写 (ADMIN/USER)，前端判断需注意大小写

      const role = res.data.role;
      console.log('准备进行路由跳转，角色:', role); // 添加调试日志 1
      if (role === 'ADMIN' || role === 'admin') {
        console.log('正在跳转到 管理员端...');
        router.push('/app/student') // 管理员默认跳学生管理
      } else {
        console.log('正在跳转到 学生端...');
        router.push('/app/student-course') // 学生默认跳选课页面
      }
    } else {
      message.error(res.data.msg || '登录失败')
      loadCaptcha() // 登录失败刷新验证码，防止暴力破解
    }
  } catch (err) {
    // message.error('连接服务器失败，请检查后端 Nacos/Redis 是否启动')
    console.error("【登录异常捕获】:", err);

    // 只有当它是真正的网络错误（axios错误）时，才提示连接失败
    if (err.response || err.request) {
      message.error('连接服务器失败，请检查后端/网络')
    } else {
      // 如果是代码逻辑错误（比如路由找不到），提示具体的 JS 错误
      message.error('前端逻辑错误: ' + err.message)
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-box {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #5ecde0 0%, #8648c4 100%);
  /* 渐变 */
  position: relative;
  overflow: hidden;
}

/* 让登录卡片有玻璃效果*/
.login-box :deep(.n-card) {
  position: relative;
  z-index: 10;
  /* 层级 */
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  background-color: rgba(255, 255, 255, 0.863);
  border: 2px solid rgba(255, 255, 255, 0.822);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
}
</style>
