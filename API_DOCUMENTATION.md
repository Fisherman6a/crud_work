# 📡 API接口文档

## 基础信息

- **Base URL**: `http://localhost:8080`
- **Content-Type**: `application/json`
- **跨域支持**: 所有接口支持CORS

---

## 通知系统 API

### 1. 获取用户通知列表

**接口地址**: `GET /notification/list/{userId}`

**请求参数**:
- Path Variable:
  - `userId` (String): 用户ID（用户名或学号）
- Query Parameter:
  - `limit` (Integer, 可选): 返回数量限制，默认50

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "userId": "admin",
      "type": "COURSE_REMIND",
      "title": "课程提醒",
      "content": "您的《高等数学》课程将在30分钟后开始",
      "courseId": 1001,
      "scheduleId": 2001,
      "isRead": false,
      "createTime": "2025-12-16T10:30:00"
    }
  ]
}
```

**通知类型**:
- `SYSTEM_NOTICE`: 系统通知
- `COURSE_REMIND`: 课程提醒
- `SELECTION_SUCCESS`: 选课成功
- `COURSE_CHANGE`: 课程变更

---

### 2. 获取未读通知数量

**接口地址**: `GET /notification/unread-count/{userId}`

**请求参数**:
- Path Variable:
  - `userId` (String): 用户ID

**响应示例**:
```json
{
  "code": 200,
  "data": 5
}
```

---

### 3. 标记通知为已读

**接口地址**: `PUT /notification/read/{id}`

**请求参数**:
- Path Variable:
  - `id` (Long): 通知ID

**响应示例**:
```json
{
  "code": 200,
  "message": "标记成功"
}
```

---

### 4. 删除通知

**接口地址**: `DELETE /notification/delete/{id}`

**请求参数**:
- Path Variable:
  - `id` (Long): 通知ID

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

### 5. 发送通知（并推送WebSocket）

**接口地址**: `POST /notification/send`

**请求体**:
```json
{
  "userId": "admin",
  "type": "SYSTEM_NOTICE",
  "title": "系统通知",
  "content": "这是一条测试通知",
  "courseId": null,
  "scheduleId": null
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "发送成功"
}
```

**功能说明**:
- 保存通知到数据库
- 自动通过WebSocket推送给在线用户
- 可选关联课程和排课

---

## 课程资源 API

### 1. 上传课程资源

**接口地址**: `POST /resource/upload`

**请求参数**:
- Query Parameter:
  - `courseId` (Long): 课程ID
- Form Data:
  - `file` (MultipartFile): 上传的文件

**支持格式**: PDF, DOC, DOCX, PPT, PPTX, MP4, MP3, AVI, MOV

**响应示例**:
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "id": 1,
    "courseId": 1001,
    "resourceName": "第一章课件.pdf",
    "resourceType": "pdf",
    "resourceUrl": "http://localhost:9000/course-files/xxx.pdf",
    "createTime": "2025-12-16T10:30:00"
  }
}
```

**功能说明**:
- 文件上传到MinIO
- 使用Tika提取文档内容
- 内容索引到Elasticsearch
- 记录到t_course_resource表

---

### 2. 获取课程资源列表

**接口地址**: `GET /resource/list`

**请求参数**:
- Query Parameter:
  - `courseId` (Long): 课程ID

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "courseId": 1001,
      "resourceName": "第一章课件.pdf",
      "resourceType": "pdf",
      "resourceUrl": "http://localhost:9000/course-files/xxx.pdf",
      "createTime": "2025-12-16T10:30:00"
    }
  ]
}
```

---

### 3. 获取文件预览URL

**接口地址**: `GET /resource/preview/{id}`

**请求参数**:
- Path Variable:
  - `id` (Long): 资源ID

**响应示例**:
```json
{
  "code": 200,
  "data": "http://localhost:9000/course-files/xxx.pdf?X-Amz-Expires=3600"
}
```

**说明**: 返回MinIO预签名URL，有效期1小时

---

### 4. 下载文件

**接口地址**: `GET /resource/download/{id}`

**请求参数**:
- Path Variable:
  - `id` (Long): 资源ID

**响应**: 文件流（触发浏览器下载）

---

### 5. 删除资源

**接口地址**: `DELETE /resource/delete/{id}`

**请求参数**:
- Path Variable:
  - `id` (Long): 资源ID

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

**功能说明**:
- 删除MinIO中的文件
- 删除Elasticsearch索引
- 删除数据库记录

---

## 课程搜索 API

### 全文搜索课程资源

**接口地址**: `GET /course/search`

**请求参数**:
- Query Parameter:
  - `keyword` (String): 搜索关键词

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "courseId": 1001,
      "courseName": "高等数学",
      "resourceName": "第一章：极限与连续.pdf",
      "content": "...高亮的搜索内容...",
      "score": 8.5
    }
  ]
}
```

**功能说明**:
- 基于Elasticsearch全文搜索
- 支持IK中文分词
- 结果按相关度排序
- 高亮显示匹配内容

---

## 学生选课 API

### 1. 获取可选课程列表

**接口地址**: `GET /timetable/available`

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 2001,
      "courseId": 1001,
      "name": "高等数学",
      "teacherId": 3001,
      "teacherName": "张教授",
      "weekDay": 1,
      "sectionStart": 1,
      "sectionEnd": 2,
      "location": "A101",
      "maxCapacity": 50,
      "currentCount": 35,
      "credit": 4,
      "description": "高等数学是理工科学生的基础课程..."
    }
  ]
}
```

**字段说明**:
- `id`: 排课ID (schedule_id)
- `weekDay`: 星期几 (1-7)
- `sectionStart/End`: 第几节课
- `currentCount/maxCapacity`: 已选/总容量

---

### 2. 选课

**接口地址**: `POST /timetable/select`

**请求体**:
```json
{
  "studentId": "2021001",
  "scheduleId": 2001
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "选课成功"
}
```

**错误响应**:
```json
{
  "code": 400,
  "message": "课程容量已满"
}
```

**功能说明**:
- 检查容量限制
- 检查时间冲突
- 更新选课记录
- 发送通知
- 触发RabbitMQ短信任务

---

### 3. 退课

**接口地址**: `POST /timetable/drop`

**请求体**:
```json
{
  "studentId": "2021001",
  "scheduleId": 2001
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "退课成功"
}
```

---

### 4. 获取我的选课列表

**接口地址**: `GET /student-course/my-courses`

**请求参数**:
- Query Parameter:
  - `studentId` (String): 学号

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 5001,
      "studentId": "2021001",
      "scheduleId": 2001,
      "courseName": "高等数学",
      "teacherName": "张教授",
      "weekDay": 1,
      "sectionStart": 1,
      "sectionEnd": 2,
      "location": "A101",
      "createTime": "2025-09-01T10:00:00"
    }
  ]
}
```

---

## 课程管理 API（管理员）

### 1. 获取所有课程

**接口地址**: `GET /course/all`

**响应示例**:
```json
{
  "code": 200,
  "data": [
    {
      "id": 1001,
      "name": "高等数学",
      "description": "理工科基础课程",
      "credit": 4,
      "teacherName": "张教授, 李老师"
    }
  ]
}
```

---

### 2. 创建课程

**接口地址**: `POST /course/add`

**请求体**:
```json
{
  "id": 1002,
  "name": "大学物理",
  "description": "物理学基础",
  "credit": 3
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "添加成功"
}
```

---

### 3. 更新课程

**接口地址**: `PUT /course/update`

**请求体**:
```json
{
  "id": 1002,
  "name": "大学物理(修订版)",
  "description": "物理学基础课程",
  "credit": 4
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "更新成功"
}
```

---

### 4. 删除课程

**接口地址**: `DELETE /course/delete/{id}`

**请求参数**:
- Path Variable:
  - `id` (Long): 课程ID

**响应示例**:
```json
{
  "code": 200,
  "message": "删除成功"
}
```

---

## WebSocket接口

### 连接地址

**WebSocket URL**: `ws://localhost:8080/ws/{userId}`

**连接示例**:
```javascript
const socket = new WebSocket('ws://localhost:8080/ws/admin')

socket.onopen = () => {
  console.log('WebSocket connected')
}

socket.onmessage = (event) => {
  console.log('Received:', event.data)
  const message = JSON.parse(event.data)
  // 处理消息
}

socket.onerror = (error) => {
  console.error('WebSocket error:', error)
}

socket.onclose = () => {
  console.log('WebSocket closed')
}
```

---

### 消息格式

**服务器推送的消息格式**:
```json
{
  "type": "COURSE_REMIND",
  "title": "课程提醒",
  "content": "您的《高等数学》课程将在30分钟后开始",
  "courseId": 1001,
  "scheduleId": 2001,
  "timestamp": "2025-12-16T10:30:00"
}
```

**心跳检测**:
```json
{
  "type": "ping"
}
```

**服务器响应**:
```json
{
  "type": "pong"
}
```

---

## 系统配置 API

### 获取系统配置

**接口地址**: `GET /system/config`

**响应示例**:
```json
{
  "code": 200,
  "data": {
    "selectionStartDate": "2025-09-01",
    "selectionEndDate": "2025-09-10",
    "currentSemester": "2025-1",
    "maxCoursesPerStudent": 10
  }
}
```

---

## 错误码说明

| 错误码 | 说明 | 处理建议 |
|--------|------|----------|
| 200 | 成功 | - |
| 400 | 请求参数错误 | 检查请求参数格式 |
| 401 | 未授权 | 重新登录 |
| 403 | 无权限 | 联系管理员 |
| 404 | 资源不存在 | 检查资源ID |
| 500 | 服务器错误 | 查看后端日志 |

---

## 请求示例

### 使用Postman

1. 导入以下集合（Collection）
2. 设置环境变量：
   - `baseUrl`: `http://localhost:8080`
   - `userId`: `admin` 或 `user`

### 使用curl

```bash
# 发送通知
curl -X POST http://localhost:8080/notification/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "admin",
    "type": "SYSTEM_NOTICE",
    "title": "测试通知",
    "content": "这是测试内容"
  }'

# 获取通知列表
curl http://localhost:8080/notification/list/admin?limit=10

# 获取未读数量
curl http://localhost:8080/notification/unread-count/admin

# 标记已读
curl -X PUT http://localhost:8080/notification/read/1

# 上传文件
curl -X POST "http://localhost:8080/resource/upload?courseId=1001" \
  -F "file=@/path/to/file.pdf"

# 获取课程资源
curl http://localhost:8080/resource/list?courseId=1001

# 选课
curl -X POST http://localhost:8080/timetable/select \
  -H "Content-Type: application/json" \
  -d '{
    "studentId": "2021001",
    "scheduleId": 2001
  }'
```

---

## 前端调用示例

### Axios封装

```javascript
import axios from 'axios'

const API_BASE = 'http://localhost:8080'

// 获取通知列表
export const getNotifications = (userId, limit = 50) => {
  return axios.get(`${API_BASE}/notification/list/${userId}`, {
    params: { limit }
  })
}

// 发送通知
export const sendNotification = (notification) => {
  return axios.post(`${API_BASE}/notification/send`, notification)
}

// 上传文件
export const uploadResource = (courseId, file) => {
  const formData = new FormData()
  formData.append('file', file)

  return axios.post(`${API_BASE}/resource/upload`, formData, {
    params: { courseId },
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 选课
export const selectCourse = (studentId, scheduleId) => {
  return axios.post(`${API_BASE}/timetable/select`, {
    studentId,
    scheduleId
  })
}
```

---

## 性能建议

1. **分页查询**: 对于列表接口，建议使用分页参数
2. **缓存策略**: 课程列表等静态数据可以前端缓存
3. **文件上传**: 大文件建议使用分片上传
4. **WebSocket**: 实现心跳检测和断线重连
5. **搜索优化**: ES搜索结果可以前端缓存一段时间

---

## 安全建议

1. **认证**: 生产环境应添加JWT token验证
2. **权限**: 接口应验证用户角色权限
3. **文件上传**: 限制文件类型和大小
4. **SQL注入**: 使用MyBatis Plus防止SQL注入
5. **XSS**: 前端渲染用户输入时进行转义

---

**文档版本**: 1.0
**最后更新**: 2025-12-16
