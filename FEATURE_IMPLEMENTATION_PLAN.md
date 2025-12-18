# 课程管理与学生选课系统 - 新功能实现方案

## 一、功能需求总览

### 1. 课程管理模块（管理员端）
- ✅ 课程附件上传（MinIO存储）
- ✅ 课程附件全文搜索（Elasticsearch + IK分词）
- 📋 课程附件预览（文档展示、音视频播放）
- 📋 美化课程资料管理界面

### 2. 学生选课模块（学生端）
- ✅ 学生选课基础功能
- 📋 优化选课界面（卡片式布局）
- 📋 课程日历表视图
- 📋 课程通知系统（站内信 + 短信）

---

## 二、技术架构

### 后端技术栈
- **文件存储**: MinIO (已配置 localhost:9000)
- **全文搜索**: Elasticsearch 7.x + IK分词器
- **实时通知**: WebSocket (已实现 /ws/{userId})
- **异步消息**: RabbitMQ (队列: sms.queue)
- **文档处理**: Apache Tika 2.9.1

### 前端技术栈
- **UI框架**: Naive UI (n-card, n-upload, n-calendar, n-notification)
- **状态管理**: Pinia (stores/system.js)
- **视频播放**: HTML5 Video Player
- **文档预览**: PDF.js / iframe 预览

---

## 三、详细实现方案

### 模块1：课程附件管理（CourseMaterial.vue 优化）

#### 1.1 后端API（已实现 ✅）
```java
// CourseResourceController.java
POST   /resource/upload          // 上传资源到MinIO
GET    /resource/download/{id}   // 下载资源
DELETE /resource/delete/{id}     // 删除资源
GET    /resource/preview/{id}    // 获取预览URL
GET    /resource/course/{id}     // 获取课程的所有资源

// CourseController.java
GET    /course/search            // ES全文搜索课程资源
```

#### 1.2 前端界面设计
```vue
布局：左右分栏
- 左侧：课程列表（n-list + 搜索框）
- 右侧：选中课程的资源管理
  - 上传区域（n-upload 拖拽上传）
  - 资源列表（n-data-table）
    - 列：文件名、类型、大小、上传时间、操作
    - 操作：预览、下载、删除
  - 全文搜索（n-input + 高亮显示）
```

#### 1.3 文件预览组件（新建）
```vue
// components/FilePreview.vue
支持格式：
- PDF: 使用 <iframe> 或 PDF.js
- Word/PPT: 转换为PDF或使用Office Online Viewer
- 音频: <audio> 标签
- 视频: <video> 标签 + 播放控制
- 图片: <img> 标签
```

---

### 模块2：学生选课界面优化（StudentCourse.vue）

#### 2.1 新界面布局
```vue
顶部：搜索栏 + 筛选器
- 搜索框：课程名称/教师名称
- 筛选：上课时间、学分、课程类型
- 我的选课按钮：跳转到已选课程列表

主体：卡片式课程列表（n-grid + n-card）
每张卡片显示：
- 课程封面图（默认图）
- 课程名称 + 课程编号
- 教师名称（支持多教师）
- 上课时间、教室、学分
- 已选/总容量 进度条
- 课程简介（折叠显示）
- 操作按钮：选课/退课/查看详情
```

#### 2.2 课程详情抽屉（n-drawer）
```vue
- 完整课程信息
- 课程资料预览列表
- 选课记录统计
- 选课确认按钮
```

---

### 模块3：课程日历表（新页面）

#### 3.1 后端API（需新增）
```java
// StudentCourseController.java
GET /student-course/calendar/{studentId}
返回：
[
  {
    date: "2025-09-01",
    courses: [
      { courseId, courseName, time, room, teacher }
    ]
  }
]
```

#### 3.2 前端组件（基于 MyTimetable.vue 扩展）
```vue
// views/MyCourseCalendar.vue
使用 n-calendar 组件
- 月视图：显示每天的课程数量（圆点标记）
- 点击日期：显示当天课程详情（n-popover）
- 支持切换月份
- 今日高亮
```

---

### 模块4：课程通知系统

#### 4.1 后端实现

##### 4.1.1 消息实体（新增）
```java
// entity/Notification.java
- id (Long)
- userId (String)        // 接收用户
- type (String)          // 消息类型：COURSE_REMIND, SELECTION_SUCCESS
- title (String)
- content (String)
- courseId (String)      // 关联课程
- isRead (Boolean)
- createTime (LocalDateTime)
```

##### 4.1.2 通知API（新增）
```java
// controller/NotificationController.java
GET    /notification/list/{userId}       // 获取用户通知列表
PUT    /notification/read/{id}           // 标记已读
DELETE /notification/delete/{id}         // 删除通知
GET    /notification/unread-count/{userId} // 未读数量
```

##### 4.1.3 WebSocket推送（已实现，增强）
```java
// WebSocketServer.java
增加方法：
- sendNotification(userId, notification)  // 发送通知
- broadcastCourseRemind(courseId)         // 课程提醒广播
```

##### 4.1.4 RabbitMQ短信队列（已实现，完善）
```java
// mq/SmsConsumer.java
监听 sms.queue
处理场景：
1. 选课成功通知
2. 课程时间变更通知
3. 课程取消通知
4. 选课开始/结束提醒
```

##### 4.1.5 定时任务（新增）
```java
// scheduled/CourseReminderTask.java
使用 @Scheduled
- 每天早上8点：推送当天课程提醒
- 课前30分钟：推送上课提醒
```

#### 4.2 前端实现

##### 4.2.1 消息中心组件（新增）
```vue
// components/NotificationCenter.vue
位于 TopBar 右侧
- 小铃铛图标 + 未读数量徽章
- 点击展开下拉列表（n-dropdown）
- 显示最近10条通知
- 操作：标记已读、删除、查看全部
```

##### 4.2.2 WebSocket客户端（新增）
```javascript
// utils/websocket.js
- 连接 ws://localhost:8081/ws/{userId}
- 监听消息推送
- 触发 n-notification 桌面通知
- 更新 Pinia store 中的消息列表
```

##### 4.2.3 通知Store（新增）
```javascript
// stores/notification.js
state:
- notifications: []        // 通知列表
- unreadCount: 0          // 未读数量
- isConnected: false      // WebSocket连接状态

actions:
- connectWebSocket()
- fetchNotifications()
- markAsRead(id)
- deleteNotification(id)
```

---

## 四、数据库表设计

### 新增表：t_notification
```sql
CREATE TABLE t_notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(50) NOT NULL COMMENT '用户ID',
    type VARCHAR(50) NOT NULL COMMENT '通知类型',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    course_id VARCHAR(50) COMMENT '关联课程ID',
    is_read BOOLEAN DEFAULT FALSE COMMENT '是否已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
```

---

## 五、实施步骤

### 阶段1：完善课程资料管理（1-2天）
1. ✅ 验证MinIO、ES配置
2. 📋 优化 CourseMaterial.vue UI
3. 📋 实现 FilePreview.vue 组件
4. 📋 测试文件上传、搜索、预览

### 阶段2：优化选课界面（1天）
1. 📋 重构 StudentCourse.vue 为卡片布局
2. 📋 添加搜索和筛选功能
3. 📋 实现课程详情抽屉

### 阶段3：实现课程日历（0.5天）
1. 📋 创建后端日历API
2. 📋 开发 MyCourseCalendar.vue
3. 📋 添加路由配置

### 阶段4：构建通知系统（2天）
1. 📋 创建数据库表和实体类
2. 📋 实现通知后端API
3. 📋 开发WebSocket客户端
4. 📋 实现NotificationCenter组件
5. 📋 集成RabbitMQ短信通知
6. 📋 添加定时任务

### 阶段5：测试与优化（1天）
1. 📋 端到端测试所有功能
2. 📋 性能优化
3. 📋 UI/UX调整

---

## 六、技术难点与解决方案

### 难点1：大文件上传性能
**解决方案**：
- 前端：使用分片上传（n-upload customRequest）
- 后端：MinIO支持分段上传

### 难点2：文档全文搜索准确性
**解决方案**：
- 使用IK分词器中文分词
- Tika提取文档纯文本内容
- ES高亮显示搜索结果

### 难点3：音视频播放兼容性
**解决方案**：
- 视频格式转码（FFmpeg）
- 支持HLS流式播放
- 移动端适配

### 难点4：WebSocket连接稳定性
**解决方案**：
- 心跳检测机制
- 断线自动重连
- 降级为轮询

### 难点5：短信接口对接
**解决方案**：
- 使用阿里云短信服务
- Mock模式方便测试
- 限流防止滥用

---

## 七、UI设计规范

### 色彩方案
- 主色调：#18a058（Naive UI默认绿色）
- 成功：#18a058
- 警告：#f0a020
- 错误：#d03050
- 信息：#2080f0

### 组件风格
- 统一使用Naive UI组件库
- 卡片圆角：8px
- 阴影：0 2px 8px rgba(0,0,0,0.1)
- 间距：8px的倍数

### 响应式设计
- 桌面端：1200px+ 三列布局
- 平板端：768-1199px 两列布局
- 移动端：<768px 单列布局

---

## 八、测试计划

### 功能测试
- [ ] 文件上传（Word/PDF/PPT/视频/音频）
- [ ] 文件搜索（中文分词、模糊匹配）
- [ ] 文件预览（各种格式）
- [ ] 选课流程（选课、退课、容量限制）
- [ ] 日历显示（周视图、月视图）
- [ ] WebSocket通知（实时推送）
- [ ] 短信通知（模拟发送）

### 性能测试
- [ ] 100MB大文件上传
- [ ] 1000条数据搜索响应时间
- [ ] 100并发选课压力测试
- [ ] WebSocket 1000连接负载

### 兼容性测试
- [ ] Chrome/Edge/Firefox浏览器
- [ ] Windows/Mac/Linux系统
- [ ] 移动端H5页面

---

## 九、部署检查清单

### 中间件启动
- [x] MySQL 3306
- [ ] Redis 6379
- [ ] Elasticsearch 9200
- [ ] RabbitMQ 5672/15672
- [ ] MinIO 9000/9001
- [ ] Nacos 8848

### 配置验证
- [ ] application.yml 所有连接地址
- [ ] MinIO存储桶已创建
- [ ] ES索引已创建
- [ ] RabbitMQ队列已声明

---

## 十、后续优化方向

1. **智能推荐**：基于选课历史推荐课程
2. **课程评价**：学生对课程的评分和评论
3. **数据分析**：选课热度、时间段分布
4. **移动端App**：原生iOS/Android应用
5. **AI助手**：智能选课建议

---

## 附录：主要文件清单

### 后端文件
```
entity/Notification.java              [新增]
mapper/NotificationMapper.java        [新增]
service/INotificationService.java     [新增]
service/impl/NotificationServiceImpl.java [新增]
controller/NotificationController.java [新增]
scheduled/CourseReminderTask.java     [新增]
websocket/WebSocketServer.java        [修改]
mq/SmsConsumer.java                   [完善]
```

### 前端文件
```
views/CourseMaterial.vue              [重构]
views/StudentCourse.vue               [重构]
views/MyCourseCalendar.vue            [新增]
components/FilePreview.vue            [新增]
components/NotificationCenter.vue     [新增]
components/TopBar.vue                 [修改]
stores/notification.js                [新增]
utils/websocket.js                    [新增]
router/index.js                       [修改]
```

---

**预计总工时**: 6-7天
**优先级**: P0（核心功能）
**负责人**: 开发团队
**截止日期**: 根据实际进度调整
