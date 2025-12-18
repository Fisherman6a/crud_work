# 完整功能实施计划

## 项目概述

基于现有的课程管理系统，实现以下功能：

### 第一部分：课程资源管理
1. 课程附件上传（MinIO）
2. 课程附件全文搜索（Elasticsearch）
3. 课程附件预览（文档展示、音视频播放）

### 第二部分：学生选课系统
1. 学生选课功能
2. 课程日历表展示
3. 课程通知推送（WebSocket站内信 + RabbitMQ + 短信接口）

---

## 技术栈

**后端**:
- Spring Boot
- MinIO (文件存储)
- Elasticsearch (全文搜索)
- RabbitMQ (消息队列)
- WebSocket (实时通知)
- MyBatis-Plus

**前端**:
- Vue 3
- Naive UI
- Axios

---

## 第一阶段：课程资源管理

### 1.1 后端实现

#### CourseResourceController

**文件**: `crud_backend/src/main/java/com/example/crud_backend/controller/CourseResourceController.java`

**接口列表**:

```java
// 1. 上传课程资源
POST /resource/upload
Parameters: file (MultipartFile), courseId (Long), resourceName (String)
Response: {resourceId, resourceUrl, success}

// 2. 获取课程资源列表
GET /resource/list?courseId={courseId}
Response: List<CourseResource>

// 3. 全文搜索资源
GET /resource/search?keyword={keyword}
Response: List<CourseDoc>

// 4. 获取资源预览URL
GET /resource/preview/{resourceId}
Response: {url, resourceType, resourceName}

// 5. 删除资源
DELETE /resource/{resourceId}
Response: {success}
```

**实现要点**:
- 上传到MinIO时使用UUID防止文件名冲突
- 使用Apache Tika提取文档内容并索引到ES
- 生成临时访问URL（24小时有效期）
- 支持文件类型：PDF, DOC, PPT, MP4, MP3

#### Mapper接口

**文件**: `crud_backend/src/main/java/com/example/crud_backend/mapper/CourseResourceMapper.java`

```java
@Mapper
public interface CourseResourceMapper extends BaseMapper<CourseResource> {
}
```

### 1.2 前端实现

#### 课程资源管理页面

**文件**: `crud_frontend/src/views/CourseMaterial.vue`

**功能模块**:

1. **课程列表** (已有)
   - 展示所有课程卡片
   - 点击进入课程资源页面

2. **资源列表**
   - 表格展示资源：文件名、类型、上传时间、操作
   - 支持上传、预览、删除

3. **资源搜索**
   - 顶部搜索框
   - 全文搜索（调用ES接口）
   - 高亮显示搜索结果

4. **资源预览**
   - PDF: iframe预览
   - 视频: video标签播放
   - 音频: audio标签播放
   - 文档: 提供下载链接

**UI布局**:
```
┌─────────────────────────────────────────┐
│ 📚 课程资料管理                          │
├─────────────────────────────────────────┤
│ [搜索框] [上传按钮]                      │
├─────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐ │
│ │ 文件名 │ 类型 │ 上传时间 │ 操作    │ │
│ ├─────────────────────────────────────┤ │
│ │ xxx.pdf │ PDF │ 2025-12-16 │ 预览 删 │ │
│ │ xxx.mp4 │ 视频 │ 2025-12-16 │ 播放 删 │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

---

## 第二阶段：学生选课系统

### 2.1 后端实现

#### TimetableController (选课控制器)

**文件**: `crud_backend/src/main/java/com/example/crud_backend/controller/TimetableController.java`

**接口列表**:

```java
// 1. 获取可选课程列表
GET /timetable/available?studentId={studentId}
Response: List<CourseScheduleDTO>

// 2. 学生选课
POST /timetable/select
Body: {studentId, scheduleId}
Response: {success, message}

// 3. 退课
POST /timetable/drop
Body: {studentId, scheduleId}
Response: {success, message}

// 4. 获取学生课表
GET /timetable/student/{studentId}
Response: List<StudentCourseDTO>

// 5. 获取周课表数据（用于日历展示）
GET /timetable/weekly/{studentId}
Response: Map<Integer, List<CourseSchedule>>  // key: weekDay (1-7)
```

**业务逻辑**:
1. 选课时检查：
   - 课程容量是否已满
   - 时间冲突检测
   - 学分限制检查
   - 选课时间窗口验证

2. 选课成功后：
   - 更新 `t_student_course` 表
   - 增加 `t_course_schedule.current_count`
   - 发送通知到RabbitMQ队列

#### NotificationService (通知服务)

**文件**: `crud_backend/src/main/java/com/example/crud_backend/service/NotificationService.java`

**功能**:
- 发送选课成功通知
- 发送退课通知
- 发送课程变更通知

### 2.2 前端实现

#### 学生选课页面

**文件**: `crud_frontend/src/views/StudentCourse.vue`

**功能模块**:

1. **可选课程列表**
   - 展示所有可选课程
   - 显示课程信息、教师、时间、容量
   - 支持筛选和搜索

2. **我的课表**
   - Tab切换：列表视图 / 日历视图
   - 列表视图：表格展示已选课程
   - 日历视图：周课表（7天 x 节次）

3. **选课操作**
   - 点击"选课"按钮
   - 实时容量显示
   - 冲突提示

**UI布局**:
```
┌───────────────────────────────────────────┐
│ 📖 学生选课                                │
├───────────────────────────────────────────┤
│ [可选课程] [我的课表]                      │
├───────────────────────────────────────────┤
│ ┌─────────────────────────────────────┐   │
│ │ 课程名 │教师│时间│容量│操作         │   │
│ ├─────────────────────────────────────┤   │
│ │ 高数  │张三│周一1-2│30/50│[选课]  │   │
│ │ 英语  │李四│周二3-4│40/50│[选课]  │   │
│ └─────────────────────────────────────┘   │
└───────────────────────────────────────────┘
```

#### 课程日历表

使用 Naive UI 的 Calendar 组件，展示每周课程安排。

**示例代码**:
```vue
<n-calendar
  :value="selectedDate"
  @update:value="onDateChange"
>
  <template #cell="{ date }">
    <div v-for="course in getCoursesForDate(date)" :key="course.id">
      {{ course.name }} - {{ course.location }}
    </div>
  </template>
</n-calendar>
```

---

## 第三阶段：通知推送系统

### 3.1 WebSocket站内信推送

#### WebSocketConfig

**文件**: `crud_backend/src/main/java/com/example/crud_backend/config/WebSocketConfig.java`

```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new NotificationWebSocketHandler(), "/ws/notifications")
                .setAllowedOrigins("*");
    }
}
```

#### 前端WebSocket连接

**文件**: `crud_frontend/src/utils/websocket.js`

```javascript
export class WebSocketClient {
  constructor(url) {
    this.ws = new WebSocket(url)
    this.ws.onmessage = (event) => {
      const notification = JSON.parse(event.data)
      // 显示通知
      window.$message.success(notification.message)
    }
  }
}
```

### 3.2 RabbitMQ消息队列

#### RabbitMQConfig

**文件**: `crud_backend/src/main/java/com/example/crud_backend/config/RabbitMQConfig.java`

```java
@Configuration
public class RabbitMQConfig {
    public static final String NOTIFICATION_QUEUE = "course.notification.queue";
    public static final String NOTIFICATION_EXCHANGE = "course.notification.exchange";

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(notificationQueue())
            .to(notificationExchange())
            .with("notification");
    }
}
```

#### NotificationConsumer

**文件**: `crud_backend/src/main/java/com/example/crud_backend/consumer/NotificationConsumer.java`

```java
@Component
public class NotificationConsumer {

    @Autowired
    private SmsService smsService;

    @Autowired
    private WebSocketService webSocketService;

    @RabbitListener(queues = RabbitMQConfig.NOTIFICATION_QUEUE)
    public void handleNotification(NotificationMessage message) {
        // 1. WebSocket推送站内信
        webSocketService.sendToUser(message.getUserId(), message);

        // 2. 发送短信
        if (message.isSendSms()) {
            smsService.send(message.getPhone(), message.getContent());
        }
    }
}
```

### 3.3 短信接口集成

#### SmsService

**文件**: `crud_backend/src/main/java/com/example/crud_backend/service/SmsService.java`

```java
@Service
public class SmsService {

    @Value("${sms.api-key}")
    private String apiKey;

    @Value("${sms.api-url}")
    private String apiUrl;

    public void send(String phone, String content) {
        // 调用第三方短信API
        // 例如：阿里云短信服务、腾讯云短信服务
        RestTemplate restTemplate = new RestTemplate();
        Map<String, Object> params = new HashMap<>();
        params.put("phone", phone);
        params.put("content", content);
        params.put("apiKey", apiKey);

        restTemplate.postForObject(apiUrl, params, String.class);
    }
}
```

---

## 实施顺序

### 阶段一：基础功能（1-2天）
1. ✅ 完善CourseResourceController接口
2. ✅ 实现资源上传和MinIO集成
3. ✅ 实现ES全文搜索功能
4. ✅ 前端资源管理页面

### 阶段二：选课系统（2-3天）
5. ✅ 实现TimetableController接口
6. ✅ 前端学生选课页面
7. ✅ 实现课程日历表

### 阶段三：通知系统（2-3天）
8. ✅ 集成WebSocket站内信
9. ✅ 集成RabbitMQ消息队列
10. ✅ 集成短信接口

### 阶段四：测试和优化（1-2天）
11. ✅ 端到端测试
12. ✅ 性能优化
13. ✅ UI/UX调整

---

## 数据库表设计（已有）

### t_course_resource (课程资源表)
```sql
CREATE TABLE t_course_resource (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    resource_name VARCHAR(255),
    resource_type VARCHAR(50),
    resource_url VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (course_id) REFERENCES t_course(id)
);
```

### t_student_course (选课记录表)
```sql
CREATE TABLE t_student_course (
    id BIGINT PRIMARY KEY,
    student_id VARCHAR(50) NOT NULL,
    schedule_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    score DECIMAL(5, 2),
    UNIQUE KEY uk_stu_schedule (student_id, schedule_id)
);
```

### t_course_schedule (排课表)
```sql
CREATE TABLE t_course_schedule (
    id BIGINT PRIMARY KEY,
    course_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    semester VARCHAR(20) DEFAULT '2025-1',
    week_day INT NOT NULL,
    section_start INT NOT NULL,
    section_end INT NOT NULL,
    location VARCHAR(50),
    max_capacity INT DEFAULT 50,
    current_count INT DEFAULT 0,
    FOREIGN KEY (course_id) REFERENCES t_course(id),
    FOREIGN KEY (teacher_id) REFERENCES t_teacher(id)
);
```

---

## 前端页面结构

```
crud_frontend/src/views/
├── Manager.vue (学生管理 - 已有)
├── TeacherManager.vue (教师管理 - 已有)
├── SelectionManage.vue (课程管理 - 已有)
├── CourseMaterial.vue (课程资料 - 需完善)
├── StudentCourse.vue (学生选课 - 需完善)
└── ScheduleManager.vue (排课管理 - 已有)
```

---

## 依赖配置

### pom.xml 需添加的依赖

```xml
<!-- WebSocket -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>

<!-- RabbitMQ -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>

<!-- Apache Tika (文档内容提取) -->
<dependency>
    <groupId>org.apache.tika</groupId>
    <artifactId>tika-core</artifactId>
    <version>2.9.1</version>
</dependency>
```

---

## 注意事项

1. **MinIO Bucket创建**: 确保在MinIO中创建了 `course-files` bucket
2. **Elasticsearch索引**: 确保CourseDoc的索引已创建
3. **RabbitMQ队列**: 确保RabbitMQ服务已启动
4. **短信API**: 需要申请第三方短信服务API Key
5. **跨域配置**: 所有Controller已添加 `@CrossOrigin(origins = "*")`

---

## 下一步行动

我将开始实施这个计划，从第一阶段的CourseResourceController开始。您希望我：

1. **逐步实施** - 一次完成一个功能模块，每完成一个给您review
2. **快速实施** - 一次性实现所有关键代码
3. **选择性实施** - 只实施您最关心的部分

请告诉我您的偏好！
