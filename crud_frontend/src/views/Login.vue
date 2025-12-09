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
              style="cursor: pointer; height: 34px; border: 1px solid #ddd; border-radius: 4px;" title="看不清？点击刷新" />
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
      router.push('/student')
    } else {
      message.error(res.data.msg || '登录失败')
      loadCaptcha() // 登录失败刷新验证码，防止暴力破解
    }
  } catch (err) {
    message.error('连接服务器失败，请检查后端 Nacos/Redis 是否启动')
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
  background-color: #f0f2f5;
}
</style>
