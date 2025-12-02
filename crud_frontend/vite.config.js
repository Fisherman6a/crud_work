import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path' // 引入 path

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 关键配置：让 "@" 指向 "src" 目录
      '@': path.resolve(__dirname, 'src')
    }
  }
})
