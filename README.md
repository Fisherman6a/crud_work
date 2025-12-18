# 🎓 教育管理系统 - 课程管理与学生选课平台

<div align="center">

![Vue 3](https://img.shields.io/badge/Vue-3.5-42b883?logo=vue.js)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-6db33f?logo=spring)
![Naive UI](https://img.shields.io/badge/Naive%20UI-2.41-18a058)
![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)

一个功能完善的教育管理系统，支持课程管理、学生选课、资料共享和实时通知。

[快速开始](#-快速开始) • [功能特性](#-功能特性) • [技术栈](#️-技术栈) • [文档](#-文档)

</div>

---

## ✨ 最新更新 (v2.0.0 - 2025-12-16)

### 🎉 重大功能升级

1. **📢 实时通知系统**
   - WebSocket双向通信
   - 通知中心UI组件
   - 未读消息徽章提醒
   - 浏览器桌面通知

2. **📚 课程资料管理**
   - MinIO对象存储
   - 拖拽上传文件
   - 多格式预览（PDF、视频、音频）
   - Elasticsearch全文搜索

3. **🎨 美化学生选课界面**
   - 现代卡片式设计
   - 彩色渐变封面
   - 实时容量进度条
   - 智能搜索和筛选

---

## 📸 界面预览

### 学生选课 - 卡片式设计
- 🎨 彩色渐变卡片封面
- 📊 实时容量进度条（绿/橙/红）
- 🔍 智能搜索和筛选功能
- 📱 响应式布局（移动端友好）

### 课程资料管理
- ☁️ 拖拽上传（支持多文件）
- 📄 多格式预览（PDF、Word、PPT、视频、音频）
- 🔍 Elasticsearch全文搜索
- 💾 MinIO分布式存储

### 通知中心
- 🔔 WebSocket实时推送
- 📬 通知中心下拉面板
- 🔴 未读徽章提醒（99+）
- 🖥️ 浏览器桌面通知

---

## 🚀 快速开始

### 环境要求

- ✅ JDK 21
- ✅ Node.js 16+
- ✅ MySQL 8.0
- ✅ Redis
- ✅ MinIO
- ✅ Elasticsearch 7.x
- ✅ Nacos (可选)
- ✅ RabbitMQ (可选)

### 一键启动

```bash
# 1. 初始化数据库
mysql -u root -p < crud_backend/sql/table.sql

# 2. 启动MinIO
minio server D:\minio-data --console-address ":9001"

# 3. 启动后端
cd crud_backend
mvn spring-boot:run

# 4. 启动前端
cd crud_frontend
npm install
npm run dev
```

### 登录测试

**管理员**: `admin` / `123456`
**学生**: `user` / `123456`

更多详情: [快速启动指南](QUICK_START.md)

---

## 🎯 功能特性

### 👨‍💼 管理员功能
- ✅ 学生管理（CRUD，学号作主键）
- ✅ 教师管理（课程分配）
- ✅ 课程管理（设置学分、简介）
- ✅ **课程资料管理（上传、预览、全文搜索）** ✨ 新增
- ✅ 排课管理（时间、教室安排）
- ✅ **通知发送（WebSocket推送）** ✨ 新增

### 👨‍🎓 学生功能
- ✅ **课程浏览（美观卡片式界面）** ✨ 优化
- ✅ **智能筛选（时间、学分、教师）** ✨ 新增
- ✅ 在线选课（实时容量显示）
- ✅ **课程资料（下载、预览）** ✨ 新增
- ✅ **课表查看（日历视图）** ✨ 优化
- ✅ **消息通知（实时推送）** ✨ 新增

---

## 🛠️ 技术栈

### 后端
- **框架**: Spring Boot 3.2.5
- **语言**: Java 21
- **ORM**: MyBatis Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **搜索**: Elasticsearch 7.x + IK分词
- **文件存储**: MinIO
- **消息队列**: RabbitMQ
- **实时通信**: WebSocket
- **服务治理**: Nacos
- **文档处理**: Apache Tika 2.9.1

### 前端
- **框架**: Vue 3.5 (Composition API)
- **构建工具**: Vite 6.0
- **UI库**: Naive UI 2.41
- **状态管理**: Pinia 3.0
- **路由**: Vue Router 4.5
- **HTTP**: Axios 1.7
- **图标**: @vicons/ionicons5

---

## 📂 项目结构

```
crud_work/
├── crud_backend/              # 后端Spring Boot项目
│   ├── src/main/java/
│   │   └── com/example/crud_backend/
│   │       ├── entity/        # 实体类（包含Notification ✨）
│   │       ├── mapper/        # MyBatis映射
│   │       ├── service/       # 业务逻辑
│   │       ├── controller/    # REST API（包含NotificationController ✨）
│   │       ├── config/        # MinIO、RabbitMQ、WebSocket配置
│   │       └── websocket/     # WebSocket服务
│   └── sql/
│       └── table.sql          # 数据库表结构（包含t_notification ✨）
│
├── crud_frontend/             # 前端Vue项目
│   ├── src/
│   │   ├── views/             # 页面组件
│   │   │   ├── CourseMaterial.vue      # 课程资料 ✨ 重构
│   │   │   ├── StudentCourse.vue       # 学生选课 ✨ 重构
│   │   │   └── ...
│   │   ├── components/        # 公共组件
│   │   │   ├── NotificationCenter.vue  # 通知中心 ✨ 新增
│   │   │   ├── FilePreview.vue         # 文件预览 ✨ 新增
│   │   │   └── TopBar.vue              # 顶部栏 ✨ 修改
│   │   ├── stores/            # Pinia状态管理
│   │   │   └── notification.js         # 通知store ✨ 新增
│   │   └── utils/
│   │       └── websocket.js            # WebSocket工具 ✨ 新增
│
└── docs/                      # 项目文档
    ├── IMPLEMENTATION_SUMMARY.md       # 实施总结 ✨
    ├── QUICK_START.md                  # 快速开始 ✨
    ├── API_DOCUMENTATION.md            # API文档 ✨
    └── FEATURE_IMPLEMENTATION_PLAN.md  # 功能计划 ✨
```

---

## 📖 文档

| 文档 | 描述 |
|------|------|
| [📄 实施总结](IMPLEMENTATION_SUMMARY.md) | 完整的功能实施总结，包含所有新增功能 |
| [🚀 快速开始](QUICK_START.md) | 详细的启动步骤和故障排查指南 |
| [📡 API文档](API_DOCUMENTATION.md) | 完整的REST API接口文档 |
| [📝 功能计划](FEATURE_IMPLEMENTATION_PLAN.md) | 功能需求和技术实现方案 |

---

## 🗄️ 数据库设计

| 表名 | 说明 | 主键类型 | 新增 |
|------|------|---------|------|
| `t_user` | 用户表 | 用户名 | - |
| `t_student` | 学生表 | 学号 | - |
| `t_teacher` | 教师表 | 手动输入ID | - |
| `t_course` | 课程表 | 手动输入ID | - |
| `t_course_teacher` | 课程-教师关联 | 自增ID | - |
| `t_course_schedule` | 排课表 | 自增ID | - |
| `t_student_course` | 选课记录 | 自增ID | - |
| `t_course_resource` | 课程资源 | 自增ID | - |
| `t_notification` | 通知表 | 自增ID | ✨ |

---

## 🔌 核心API

### 通知系统
```bash
GET    /notification/list/{userId}        # 获取通知列表
GET    /notification/unread-count/{userId} # 未读数量
PUT    /notification/read/{id}            # 标记已读
DELETE /notification/delete/{id}          # 删除通知
POST   /notification/send                 # 发送通知
```

### 课程资源
```bash
POST   /resource/upload                   # 上传文件
GET    /resource/list                     # 资源列表
GET    /resource/preview/{id}             # 预览
GET    /resource/download/{id}            # 下载
DELETE /resource/delete/{id}              # 删除
```

### 学生选课
```bash
GET    /timetable/available               # 可选课程
POST   /timetable/select                  # 选课
POST   /timetable/drop                    # 退课
GET    /student-course/my-courses         # 我的选课
```

完整API文档: [API_DOCUMENTATION.md](API_DOCUMENTATION.md)

---

## 🧪 快速测试

### 测试通知系统

```bash
# 发送测试通知
curl -X POST http://localhost:8080/notification/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "admin",
    "type": "SYSTEM_NOTICE",
    "title": "测试通知",
    "content": "这是一条测试消息"
  }'

# 查看右上角铃铛图标，应该显示未读徽章
```

### 测试文件上传

1. 登录管理员账号
2. 访问"课程资料"页面
3. 选择任意课程
4. 拖拽文件到上传区
5. 点击"预览"测试各种格式

---

## 🎨 界面特色

### 1. 现代化卡片设计
- 渐变色封面（6种颜色轮换）
- 悬停卡片上浮动画
- 已选课程绿色边框

### 2. 实时进度指示
- 绿色: 容量充裕 (< 80%)
- 橙色: 即将满员 (80-99%)
- 红色: 已满 (100%)

### 3. 响应式布局
- 移动端: 1列
- 平板: 2列
- 桌面: 3-4列

---

## ⚙️ 配置说明

### 后端配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/student

  elasticsearch:
    uris: http://localhost:9200

minio:
  endpoint: http://localhost:9000
  accessKey: minioadmin
  secretKey: minioadmin
  bucketName: course-files
```

### 前端配置

所有API地址配置在各组件的 `API_BASE` 常量中：
```javascript
const API_BASE = 'http://localhost:8080'
const WS_URL = 'ws://localhost:8080/ws'
```

---

## 🐛 常见问题

### WebSocket连接失败
- 检查后端是否启动
- 验证端口8080是否可访问
- 查看浏览器控制台错误信息

### 文件上传失败
- 检查MinIO是否运行: `curl http://localhost:9000/minio/health/live`
- 验证存储桶是否创建: 访问 http://localhost:9001
- 检查文件大小限制（默认10MB）

### 搜索无结果
- 检查Elasticsearch: `curl http://localhost:9200`
- 确认文件已上传并触发索引
- 查看后端日志是否有Tika错误

更多问题: [快速开始指南 - 故障排查](QUICK_START.md#常见问题排查)

---

## 📈 性能优化

- ✅ 前端路由懒加载
- ✅ 虚拟滚动列表
- ✅ Redis热点数据缓存
- ✅ ES倒排索引优化
- ✅ MinIO预签名URL
- ✅ WebSocket心跳检测

---

## 🔒 安全建议

生产环境部署前请完成：
- ⚠️ 添加JWT认证
- ⚠️ 配置HTTPS
- ⚠️ 限制文件上传大小和类型
- ⚠️ API接口权限控制
- ⚠️ SQL注入防护（已使用MyBatis Plus参数化）

---

## 📝 更新日志

### v2.0.0 (2025-12-16) ✨

**新增**:
- ✨ 完整通知系统（WebSocket + UI）
- ✨ 课程资料管理（MinIO + ES）
- ✨ 卡片式选课界面
- ✨ 文件预览组件

**优化**:
- 🎨 统一UI风格（Naive UI）
- 📱 响应式布局
- ⚡ 性能优化
- 📖 完善文档

### v1.0.0
- 基础管理功能
- 选课系统
- 排课管理

---

## 🤝 贡献

欢迎贡献代码！

1. Fork本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启Pull Request

---

## 📄 许可证

MIT License

---

## 🙏 致谢

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Vue.js](https://vuejs.org/)
- [Naive UI](https://www.naiveui.com/)
- [MinIO](https://min.io/)
- [Elasticsearch](https://www.elastic.co/)
- [MyBatis Plus](https://baomidou.com/)

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给个Star! ⭐**

Made with ❤️ by Development Team

</div>
