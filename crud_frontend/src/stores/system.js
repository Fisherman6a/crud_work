import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import axios from 'axios'

export const useSystemStore = defineStore('system', () => {
  // 状态
  const currentTime = ref(new Date().getTime())
  const openStart = ref(new Date('2025-09-01 00:00:00').getTime())
  const openEnd = ref(new Date('2025-09-10 00:00:00').getTime())

  // 计算属性：是否在选课时间内
  const isInSelectionPeriod = computed(() => {
    return currentTime.value >= openStart.value && currentTime.value <= openEnd.value
  })

  // 格式化时间显示
  const formattedTime = computed(() => {
    const date = new Date(currentTime.value)
    const year = date.getFullYear()
    const month = String(date.getMonth() + 1).padStart(2, '0')
    const day = String(date.getDate()).padStart(2, '0')
    const hours = String(date.getHours()).padStart(2, '0')
    const minutes = String(date.getMinutes()).padStart(2, '0')
    const seconds = String(date.getSeconds()).padStart(2, '0')
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
  })

  // 方法：从后端加载配置
  const loadConfig = async () => {
    try {
      const res = await axios.get('http://localhost:8080/system/config')
      currentTime.value = res.data.currentTime
      openStart.value = res.data.openStart
      openEnd.value = res.data.openEnd
    } catch (error) {
      console.error('加载系统配置失败:', error)
    }
  }

  // 方法：更新时间（测试用）
  const updateTime = (newTime) => {
    const timestamp = new Date(newTime).getTime()
    currentTime.value = timestamp

    // 同步到后端
    axios.post('http://localhost:8080/system/time', {
      currentTime: timestamp
    }).catch(err => {
      console.error('更新时间失败:', err)
    })
  }

  return {
    currentTime,
    openStart,
    openEnd,
    isInSelectionPeriod,
    formattedTime,
    loadConfig,
    updateTime
  }
})