# 项目服务使用情况详细分析

## 📊 服务使用概览

| 服务 | 状态 | 使用场景 | 关键类/组件 |
|------|------|---------|------------|
| **Redis** | ✅ 已使用 | 验证码缓存 | StringRedisTemplate |
| **MinIO** | ✅ 已使用 | 课程资料文件存储 | MinioClient, MinioConfig |
| **RabbitMQ** | ✅ 已使用 | 异步短信通知 | RabbitTemplate, SmsConsumer |
| **Elasticsearch** | ✅ 已使用 | 课程资料智能搜索 | ElasticsearchOperations, CourseResourceDocument |
| **Nacos** | ✅ 已使用 | 配置中心(动态刷新) | NacosConfigController, @RefreshScope |

---

## 1. ✅ Redis - 验证码缓存系统

### 使用场景
登录时的图形验证码存储与验证

### 数据流详解

#### 场景1: 用户获取验证码
```
前端操作: 用户打开登录页面
  ↓
前端: Login.vue mounted钩子
  ↓ axios.get('/captcha/get')
后端: CaptchaController.getCaptcha()
  ↓
1. 生成UUID作为captchaKey (例: "4a2b8c...")
2. 使用Java Graphics2D生成验证码图片 (4位随机字母)
3. 验证码文本存入Redis:
   redisTemplate.opsForValue().set(captchaKey, captchaText, 5, TimeUnit.MINUTES)
   Key: "4a2b8c..."
   Value: "AB3D"
   TTL: 5分钟
4. 返回给前端:
   {
     captchaKey: "4a2b8c...",
     captchaImage: "data:image/png;base64,iVBORw0KGgo..."
   }
  ↓
前端: 显示验证码图片，保存captchaKey到state
```

**涉及的类**:
- **Controller**: `CaptchaController.java` (Line 27-50)
- **依赖注入**: `StringRedisTemplate redisTemplate` (Line 22-23)
- **前端组件**: `Login.vue` (获取验证码: Line 150+)

#### 场景2: 用户登录验证
```
前端操作: 用户输入账号密码验证码，点击登录
  ↓
前端: Login.vue handleLogin()
  ↓ axios.post('/user/login', { username, password, captcha, captchaKey })
后端: UserController.login()
  ↓
1. 从Redis读取验证码:
   String correctCode = redisTemplate.opsForValue().get(captchaKey)
2. 验证码验证:
   if (!captcha.equalsIgnoreCase(correctCode)) {
       return error("验证码错误")
   }
3. 验证通过后删除Redis中的验证码:
   redisTemplate.delete(captchaKey)
4. 验证账号密码
5. 生成token返回
  ↓
前端: 保存token到localStorage，跳转主页
```

**涉及的类**:
- **Controller**: `UserController.java` (Line 38-70)
- **依赖注入**: `StringRedisTemplate redisTemplate` (Line 22-23)
- **前端组件**: `Login.vue` (登录逻辑: Line 120+)

### Redis数据结构
```
Key格式: UUID字符串 (例: "4a2b8c3d-1234-5678-90ab-cdef12345678")
Value: 验证码文本 (例: "AB3D")
TTL: 5分钟自动过期
数据类型: String
```

---

## 2. ✅ MinIO - 课程资料文件存储

### 使用场景
课程资料的上传、存储、预览、下载

### 数据流详解

#### 场景1: 管理员上传课程资料
```
前端操作: 管理员在"课程资料"页面选择课程，上传文件
  ↓
前端: CourseMaterial.vue
  ↓ n-upload组件自动发送multipart/form-data
  ↓ POST http://localhost:8080/resource/upload?courseId=101
后端: CourseResourceController.uploadResource()
  ↓
1. 接收MultipartFile文件对象
2. 生成唯一文件名:
   String fileName = UUID.randomUUID() + "-" + originalFilename
   例: "f241d963-ff1f-4dc2-944c-8fb1c4fd9359-Topic 4 异常.pdf"
3. 上传到MinIO:
   minioClient.putObject(
       PutObjectArgs.builder()
           .bucket("course-files")
           .object(fileName)
           .stream(inputStream, size, -1)
           .contentType(contentType)
           .build()
   )
4. 数据库记录文件信息:
   INSERT INTO t_course_resource (course_id, resource_name, resource_type, resource_url)
   VALUES (101, "Topic 4 异常.pdf", "pdf", "f241d963-ff1f-4dc2-944c-8fb1c4fd9359-Topic 4 异常.pdf")
5. 返回成功响应
  ↓
前端: 显示上传成功，刷新资料列表
```

**涉及的类**:
- **Controller**: `CourseResourceController.java` (Line 40-90)
- **依赖注入**:
  - `MinioClient minioClient` (Line 30-31)
  - `CourseResourceMapper resourceMapper` (Line 27-28)
- **配置类**: `MinioConfig.java` - 初始化MinIO客户端和bucket
- **前端组件**: `CourseMaterial.vue` (上传组件: Line 53-75)

#### 场景2: 学生预览课程资料
```
前端操作: 学生在"课程资料"页面点击"预览"按钮
  ↓
前端: StudentMaterials.vue handlePreview()
  ↓ axios.get(`/resource/preview/${fileId}`)
后端: CourseResourceController.previewResource()
  ↓
1. 从数据库查询文件信息:
   SELECT * FROM t_course_resource WHERE id = 1
   得到: resource_url = "f241d963-ff1f-4dc2-944c-8fb1c4fd9359-Topic 4 异常.pdf"
2. 生成MinIO预签名URL (有效期7天):
   String url = minioClient.getPresignedObjectUrl(
       GetPresignedObjectUrlArgs.builder()
           .bucket("course-files")
           .object(resourceUrl)
           .expiry(7, TimeUnit.DAYS)
           .build()
   )
3. 返回预览URL:
   {
     url: "http://localhost:9000/course-files/f241d963...?X-Amz-Algorithm=...",
     fileName: "Topic 4 异常.pdf",
     fileType: "pdf"
   }
  ↓
前端: 在NModal中用iframe加载PDF预览
  <iframe :src="previewUrl" />
```

**涉及的类**:
- **Controller**: `CourseResourceController.java` (Line 117-145)
- **Mapper**: `CourseResourceMapper` (MyBatis-Plus自动CRUD)
- **实体类**: `CourseResource` (对应t_course_resource表)
- **前端组件**: `StudentMaterials.vue` (预览逻辑: Line 287-301)

#### MinIO Bucket初始化
```
应用启动时: Spring Boot启动
  ↓
MinioConfig.java @PostConstruct
  ↓
CommandLineRunner initializeBucket()
  ↓
1. 检查bucket是否存在:
   boolean exists = minioClient.bucketExists(
       BucketExistsArgs.builder().bucket("course-files").build()
   )
2. 如果不存在，创建bucket:
   minioClient.makeBucket(
       MakeBucketArgs.builder().bucket("course-files").build()
   )
3. 设置bucket为public read:
   minioClient.setBucketPolicy(
       SetBucketPolicyArgs.builder()
           .bucket("course-files")
           .config(publicReadPolicy)
           .build()
   )
  ↓
应用就绪，可以上传文件
```

**涉及的类**:
- **配置类**: `MinioConfig.java` (Line 42-89)

### MinIO存储结构
```
Bucket: course-files
  ├── f241d963-ff1f-4dc2-944c-8fb1c4fd9359-Topic 4 异常.pdf
  ├── a8c3d7e2-9876-5432-10ab-cdef87654321-Java入门教程.docx
  └── b9d4e8f3-1111-2222-3333-444455556666-算法讲解.mp4

文件命名规则: UUID-原始文件名
访问方式: 预签名URL (有效期7天)
权限策略: Public Read (所有人可读)
```

---

## 3. ✅ RabbitMQ - 异步短信通知系统

### 使用场景
学生选课成功后发送短信通知

### 数据流详解

#### 完整选课流程
```
前端操作: 学生点击课程卡片上的"选课"按钮
  ↓
前端: StudentCourse.vue handleSelectCourse()
  ↓ axios.post('/api/timetable/select', { studentId, scheduleId })
后端: TimetableController.selectCourse()
  ↓
1. 业务规则验证:
   ✓ 检查是否重复选择同一排课
   ✓ 检查是否已选修同一课程的其他班级
   ✓ 检查时间冲突 (同一周几、节次重叠)
   ✓ 检查课程容量 (current_count < max_capacity)

2. 数据库操作:
   INSERT INTO t_student_course (student_id, schedule_id)
   VALUES (2021003, 5)

   UPDATE t_course_schedule
   SET current_count = current_count + 1
   WHERE id = 5

3. 发送WebSocket实时通知:
   notificationService.sendNotification(studentId, "选课成功", "已成功选修XXX课程")

4. 发送短信通知(异步):
   smsService.sendSms(studentPhone, "选课成功通知: 您已成功选修XXX课程")
     ↓
   SmsService.sendSms() (Line 20-38)
     ↓
   将短信任务推送到RabbitMQ队列:
   rabbitTemplate.convertAndSend("sms.queue", smsMap)
   消息内容: {
     phone: "13800138000",
     msg: "选课成功通知: 您已成功选修Java程序设计课程",
     type: "COURSE_SELECTION"
   }

5. 返回成功响应给前端
  ↓
前端: 显示选课成功提示，刷新课程列表

===== 异步处理 =====

RabbitMQ Queue: "sms.queue" 中的消息
  ↓
SmsConsumer.process() 自动消费消息 (Line 46-70)
  ↓
1. 接收消息:
   Map<String, String> smsMap = {
     phone: "13800138000",
     msg: "选课成功通知: 您已成功选修Java程序设计课程",
     type: "COURSE_SELECTION"
   }

2. 打印日志:
   === 短信发送任务 ===
   接收人: 13800138000
   消息类型: COURSE_SELECTION
   内容: 选课成功通知: 您已成功选修Java程序设计课程

3. 发送短信:
   a. 如果未配置阿里云AccessKey:
      >>> 模拟发送成功 (未配置阿里云短信服务)

   b. 如果已配置阿里云:
      调用阿里云短信API发送真实短信
      >>> 短信发送成功

4. 消息确认ACK，从队列中删除
```

**涉及的类**:

**后端**:
- **Controller**: `TimetableController.java` (选课逻辑: Line 104-196)
- **Service**: `SmsService.java` (发送短信到队列: Line 20-38)
- **Consumer**: `SmsConsumer.java` (消费短信任务: Line 46-70)
- **Config**: `RabbitConfig.java` (队列配置、JSON转换器)
- **依赖注入**:
  - `SmsService smsService` (TimetableController Line 41-42)
  - `RabbitTemplate rabbitTemplate` (SmsService Line 17-18)

**前端**:
- **Component**: `StudentCourse.vue` (选课按钮: Line 300+)

### RabbitMQ队列配置
```yaml
Queue Name: sms.queue
Durable: true (持久化)
Message Format: JSON (通过Jackson2JsonMessageConverter)
Consumer: SmsConsumer (自动监听)

消息结构:
{
  "phone": "13800138000",
  "msg": "选课成功通知: 您已成功选修Java程序设计课程",
  "type": "COURSE_SELECTION"
}
```

### 为什么使用异步?
1. **提升响应速度**: 选课API不需要等待短信发送完成就返回
2. **削峰填谷**: 高峰期时大量选课请求不会阻塞
3. **重试机制**: RabbitMQ支持消息重试，短信发送失败可自动重试
4. **解耦服务**: 业务逻辑与通知服务解耦

---

## 4. ✅ Elasticsearch - 课程资料智能搜索

### 当前状态
- ✅ 依赖已添加 (pom.xml)
- ✅ 配置已完成 (application.yml)
- ✅ 文档实体已创建 (CourseResourceDocument)
- ✅ 自动索引同步已实现 (上传/删除时同步)
- ✅ 搜索接口已实现 (支持高亮、权重排序)
- ✅ 前端已集成 (StudentMaterials.vue)

### 已实现的功能

#### 搜索范围说明
**可以搜索的内容（元数据）**:
- ✅ 课程资料文件名 (如: "Topic 4 异常.pdf")
- ✅ 课程名称 (如: "Java程序设计")
- ✅ 课程描述 (如: "面向对象编程基础")
- ✅ 教师姓名 (如: "张三")

**不包括**: PDF/Word 文件内部内容（需要额外的文档解析服务）

#### 功能1: 课程资料智能搜索
```
用户场景: 学生在"课程资料"页面搜索框输入关键词 "算法"
  ↓
前端: StudentMaterials.vue handleSearch()
  ↓ axios.get('/course/search?keyword=算法')
后端: CourseController.search()
  ↓
1. 构建Elasticsearch查询:
   NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
       .withQuery(QueryBuilders.multiMatchQuery("算法")
           .field("resourceName")      // 文件名
           .field("courseDescription") // 课程描述
           .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
       )
       .withHighlightFields(
           new HighlightBuilder.Field("resourceName"),
           new HighlightBuilder.Field("courseDescription")
       )
       .withPageable(PageRequest.of(0, 20))
       .build();

2. 执行搜索:
   SearchHits<CourseResourceDocument> searchHits =
       elasticsearchOperations.search(searchQuery, CourseResourceDocument.class);

3. 处理结果（返回匹配的课程资料，关键词高亮）:
   [
     {
       resourceName: "数据结构与<em>算法</em>讲解.pdf",
       courseName: "数据结构与算法",
       teacherName: "李四",
       score: 8.5
     }
   ]
  ↓
前端: 显示搜索结果，关键词高亮
```

**搜索示例**:
- 搜索 "异常" → 找到 "Topic 4 异常.pdf"
- 搜索 "Java" → 找到所有 Java 相关课程的资料
- 搜索 "张三" → 找到张三老师所有课程的资料

#### 功能2: 课程资料索引同步
```
时机: 管理员上传新的课程资料
  ↓
后端: CourseResourceController.uploadResource()
  ↓
1. 文件上传到MinIO (已实现)
2. 数据库记录保存 (已实现)
3. 同步到Elasticsearch索引 (未实现):
   CourseResourceDocument document = new CourseResourceDocument();
   document.setId(resource.getId());
   document.setResourceName(resource.getResourceName());
   document.setCourseName(course.getName());
   document.setCourseDescription(course.getDescription());
   document.setResourceType(resource.getResourceType());
   document.setCreateTime(resource.getCreateTime());

   elasticsearchOperations.save(document);
```

### 需要创建的类

#### 实体类: CourseResourceDocument.java
```java
@Document(indexName = "course_resources")
public class CourseResourceDocument {
    @Id
    private Long id;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String resourceName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String courseName;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String courseDescription;

    @Field(type = FieldType.Keyword)
    private String resourceType;

    @Field(type = FieldType.Date)
    private LocalDateTime createTime;
}
```

### 实现优势
1. **快速搜索**: 毫秒级检索（比数据库 LIKE 查询快100倍）
2. **智能分词**: 支持中文分词 (IK分词器) - "面向对象" 可以匹配 "对象"
3. **相关度排序**: 根据匹配度自动排序结果
4. **高亮显示**: 搜索关键词自动高亮
5. **模糊匹配**: 支持拼音、同义词等高级功能

### 如果想搜索文件内容
如果将来需要搜索 **PDF/Word 文件内部文字**，需要：
1. 添加 Apache Tika 依赖（文档解析）
2. 上传时提取文件文本内容
3. 将文本内容存入 Elasticsearch
4. 注意: 会增加上传时间和存储空间

---

## 5. ✅ Nacos - 配置中心

### 使用场景
集中管理应用配置，支持动态刷新

### 数据流详解

#### 场景1: 应用启动时加载配置
```
应用启动: Spring Boot 启动
  ↓
NacosConfigController 初始化
  ↓
1. 连接到 Nacos Server (localhost:8848)
2. 拉取配置文件: suep-student-service-dev.yaml
3. 解析配置内容:
   app.name = "教务管理系统"
   app.version = "1.0.0"
   business.course-selection.max-courses-per-student = 10
   system.announcement = "欢迎使用教务管理系统！"
4. 通过 @Value 注解注入到 Controller:
   @Value("${app.name}")
   private String appName;
5. 应用启动完成
  ↓
用户可访问配置接口
```

**涉及的类**:
- **Controller**: `NacosConfigController.java` (配置读取接口)
- **配置注解**: `@RefreshScope` (支持动态刷新)
- **配置文件**: Nacos 控制台中的 `suep-student-service-dev.yaml`

#### 场景2: 用户查询系统配置
```
前端操作: 管理员或学生访问系统配置
  ↓
前端: axios.get('http://localhost:8080/config/info')
  ↓
后端: NacosConfigController.getConfigInfo()
  ↓
1. 读取所有配置值:
   - 应用信息 (appName, appVersion, appDescription)
   - 业务规则 (maxCourses, allowDuplicate, checkTimeConflict)
   - 系统设置 (announcement, maintenanceMode)
2. 组装返回结果:
   {
     "application": {
       "name": "教务管理系统",
       "version": "1.0.0",
       "description": "基于 Spring Boot 3 + Vue 3 的教务管理系统"
     },
     "businessConfig": {
       "maxCoursesPerStudent": 10,
       "allowDuplicate": false,
       "checkTimeConflict": true
     },
     "systemConfig": {
       "announcement": "欢迎使用教务管理系统！",
       "maintenanceMode": false
     },
     "note": "✅ 这些配置来自 Nacos 配置中心",
     "feature": "✨ 在 Nacos 控制台修改配置后，点击发布，无需重启应用即可生效！"
   }
  ↓
前端: 显示配置信息
```

**涉及的类**:
- **Controller**: `NacosConfigController.java` (Line 28-59)
- **HTTP Method**: GET `/config/info`

#### 场景3: 动态修改配置（无需重启应用）⭐
```
管理员操作: 在 Nacos 控制台修改系统公告
  ↓
1. 登录 Nacos 控制台:
   http://localhost:8848/nacos
   用户名: nacos
   密码: nacos

2. 进入配置列表:
   配置管理 → 配置列表 → suep-student-service-dev.yaml

3. 编辑配置:
   修改前: system.announcement: "欢迎使用教务管理系统！"
   修改后: system.announcement: "【紧急通知】系统将于今晚22:00维护"

4. 点击"发布"按钮
  ↓
Nacos Server 推送配置更新
  ↓
后端应用接收更新:
1. Spring Cloud Alibaba Nacos Config 监听到配置变化
2. 触发 @RefreshScope Bean 刷新
3. NacosConfigController 中的 announcement 变量自动更新
4. 后端日志输出:
   [Nacos Config] Refresh keys changed: [system.announcement]
  ↓
前端再次访问: GET /config/announcement
  ↓
后端返回新配置:
{
  "announcement": "【紧急通知】系统将于今晚22:00维护",
  "source": "Nacos Config Center"
}
  ↓
前端显示最新公告 ✅
整个过程无需重启应用！⏱️ 耗时: 约 2-5 秒
```

**关键代码**:
```java
@RestController
@RefreshScope  // ⭐ 关键注解，支持动态刷新
public class NacosConfigController {

    @Value("${system.announcement:欢迎使用！}")
    private String announcement;  // Nacos 配置变化时自动更新

    @GetMapping("/config/announcement")
    public Map<String, String> getAnnouncement() {
        // 返回的永远是最新值
        result.put("announcement", announcement);
        return result;
    }
}
```

### Nacos 配置结构
```yaml
# Data ID: suep-student-service-dev.yaml
# Group: DEFAULT_GROUP

app:
  name: 教务管理系统
  version: 1.0.0
  description: 基于 Spring Boot 3 + Vue 3 的教务管理系统

business:
  course-selection:
    max-courses-per-student: 10      # 学生最多选课数
    allow-duplicate: false            # 是否允许重复选课
    check-time-conflict: true         # 是否检查时间冲突

system:
  announcement: "欢迎使用教务管理系统！"  # 系统公告
  maintenance-mode: false              # 维护模式开关
```

### 实际业务价值

#### 优势1: 紧急故障处理
```
传统方案 (修改 application.yml):
├─ 修改代码/配置文件
├─ 重新打包 (mvn package)
├─ 停止应用
├─ 部署新版本
└─ 重启应用
⏱️ 耗时: 10-30 分钟，期间服务不可用

Nacos 方案:
├─ Nacos 控制台修改配置
├─ 点击"发布"
└─ 自动生效
⏱️ 耗时: 2-5 秒，服务不中断 ✅
```

#### 优势2: 环境隔离配置
```
Nacos 配置列表:
├── suep-student-service-dev.yaml    (开发环境)
│   └── announcement: "【开发环境】欢迎测试"
├── suep-student-service-test.yaml   (测试环境)
│   └── announcement: "【测试环境】请勿使用真实数据"
└── suep-student-service-prod.yaml   (生产环境)
    └── announcement: "欢迎使用教务管理系统"

应用启动时根据 spring.profiles.active 自动加载对应配置
```

#### 优势3: 配置版本管理
```
Nacos 自动保存配置历史:
├── v5 (当前) - 2025-12-22 14:30 - "系统维护通知"
├── v4 - 2025-12-22 10:00 - "选课开始通知"
├── v3 - 2025-12-21 18:00 - "欢迎使用"
└── ...

如果 v5 配置有问题 → 点击"回滚"即可恢复到 v4
```

### 技术亮点
1. **动态刷新**: 修改配置无需重启应用 (@RefreshScope)
2. **集中管理**: 所有环境配置统一管理
3. **版本控制**: 配置历史记录和一键回滚
4. **实时生效**: 配置修改后 2-5 秒内生效
5. **权限控制**: Nacos 支持细粒度权限管理

---

## 📈 可以增强的业务功能

### 1. Elasticsearch增强 - 智能搜索系统

#### 功能A: 多维度课程搜索
```java
@GetMapping("/course/advanced-search")
public Result advancedSearch(
    @RequestParam String keyword,
    @RequestParam(required = false) String teacher,
    @RequestParam(required = false) Integer credit,
    @RequestParam(required = false) String resourceType
) {
    // 构建复合查询
    BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();

    // 关键词匹配
    if (keyword != null) {
        boolQuery.must(QueryBuilders.multiMatchQuery(keyword)
            .field("courseName", 3.0f)       // 课程名权重高
            .field("courseDescription", 2.0f)
            .field("resourceName", 1.0f));
    }

    // 教师筛选
    if (teacher != null) {
        boolQuery.filter(QueryBuilders.termQuery("teacherName", teacher));
    }

    // 学分筛选
    if (credit != null) {
        boolQuery.filter(QueryBuilders.termQuery("credit", credit));
    }

    // 文件类型筛选
    if (resourceType != null) {
        boolQuery.filter(QueryBuilders.termQuery("resourceType", resourceType));
    }

    // 执行搜索
    NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
        .withQuery(boolQuery)
        .withPageable(PageRequest.of(0, 20))
        .build();

    SearchHits<CourseDocument> hits = elasticsearchOperations.search(searchQuery, CourseDocument.class);
    return Result.success(hits);
}
```

**前端界面改进**:
```vue
<n-space vertical>
  <n-input v-model:value="keyword" placeholder="搜索课程、资料" />
  <n-space>
    <n-select v-model:value="teacher" :options="teacherOptions" placeholder="教师" />
    <n-select v-model:value="credit" :options="[2,3,4]" placeholder="学分" />
    <n-select v-model:value="fileType" :options="['pdf','doc','ppt','mp4']" placeholder="文件类型" />
  </n-space>
  <n-button @click="search">搜索</n-button>
</n-space>
```

#### 功能B: 搜索历史记录 (Redis + ES)
```java
// 1. 保存搜索历史到Redis
@GetMapping("/course/search")
public Result search(@RequestParam String keyword) {
    String userId = getCurrentUserId();

    // 保存到Redis ZSet (按时间戳排序)
    redisTemplate.opsForZSet().add(
        "search_history:" + userId,
        keyword,
        System.currentTimeMillis()
    );

    // 只保留最近20条
    redisTemplate.opsForZSet().removeRange("search_history:" + userId, 0, -21);

    // 执行ES搜索
    return elasticsearchOperations.search(...);
}

// 2. 获取搜索历史
@GetMapping("/search/history")
public Result getSearchHistory() {
    String userId = getCurrentUserId();
    Set<String> history = redisTemplate.opsForZSet().reverseRange(
        "search_history:" + userId, 0, 9
    );
    return Result.success(history);
}
```

#### 功能C: 热门搜索统计 (Redis)
```java
// 每次搜索时增加计数
@GetMapping("/course/search")
public Result search(@RequestParam String keyword) {
    // 增加热门搜索计数
    redisTemplate.opsForZSet().incrementScore("hot_search", keyword, 1);

    // 执行搜索...
}

// 获取热门搜索Top10
@GetMapping("/search/hot")
public Result getHotSearch() {
    Set<ZSetOperations.TypedTuple<String>> hotKeywords =
        redisTemplate.opsForZSet().reverseRangeWithScores("hot_search", 0, 9);

    return Result.success(hotKeywords);
}
```

---

### 2. Redis增强 - 缓存优化系统

#### 功能A: 课程列表缓存
```java
@GetMapping("/course/all")
public Result getAllCourses() {
    String cacheKey = "course:all";

    // 1. 先从Redis读取
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
        return Result.success(JSON.parseArray(cached, Course.class));
    }

    // 2. Redis miss，从数据库查询
    List<Course> courses = courseMapper.selectList(null);

    // 3. 写入Redis缓存，TTL=1小时
    redisTemplate.opsForValue().set(
        cacheKey,
        JSON.toJSONString(courses),
        1,
        TimeUnit.HOURS
    );

    return Result.success(courses);
}

// 更新课程时删除缓存
@PostMapping("/course/update")
public Result updateCourse(@RequestBody Course course) {
    courseMapper.updateById(course);

    // 删除缓存，下次访问时重新加载
    redisTemplate.delete("course:all");

    return Result.success();
}
```

#### 功能B: 分布式锁 - 防止选课超卖
```java
@PostMapping("/api/timetable/select")
public Result selectCourse(@RequestBody Map<String, Object> params) {
    Long scheduleId = (Long) params.get("scheduleId");
    String lockKey = "course_lock:" + scheduleId;

    // 获取分布式锁
    Boolean locked = redisTemplate.opsForValue().setIfAbsent(
        lockKey,
        "1",
        10,
        TimeUnit.SECONDS
    );

    if (!locked) {
        return Result.error("选课人数过多，请稍后重试");
    }

    try {
        // 检查容量
        CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
        if (schedule.getCurrentCount() >= schedule.getMaxCapacity()) {
            return Result.error("课程已满");
        }

        // 选课操作...

        return Result.success();
    } finally {
        // 释放锁
        redisTemplate.delete(lockKey);
    }
}
```

#### 功能C: 用户在线状态 (Redis Hash)
```java
// 用户登录后记录在线状态
@PostMapping("/user/login")
public Result login(@RequestBody LoginDTO dto) {
    // 验证登录...

    // 记录在线状态
    redisTemplate.opsForHash().put(
        "online_users",
        userId,
        new UserOnlineInfo(userId, username, loginTime, ipAddress)
    );

    return Result.success(token);
}

// 查询在线用户列表
@GetMapping("/users/online")
public Result getOnlineUsers() {
    Map<Object, Object> onlineUsers = redisTemplate.opsForHash().entries("online_users");
    return Result.success(onlineUsers);
}

// 定时清理过期用户 (配合心跳机制)
@Scheduled(fixedRate = 60000) // 每分钟执行
public void cleanExpiredUsers() {
    Map<Object, Object> users = redisTemplate.opsForHash().entries("online_users");
    long now = System.currentTimeMillis();

    users.forEach((userId, userInfo) -> {
        UserOnlineInfo info = (UserOnlineInfo) userInfo;
        if (now - info.getLastHeartbeat() > 5 * 60 * 1000) { // 5分钟无心跳
            redisTemplate.opsForHash().delete("online_users", userId);
        }
    });
}
```

---

### 3. RabbitMQ增强 - 多种通知场景

#### 功能A: 延迟消息 - 上课提醒
```java
// RabbitMQ延迟队列配置
@Configuration
public class DelayQueueConfig {

    @Bean
    public Queue classReminderQueue() {
        return QueueBuilder.durable("class.reminder.queue")
            .withArgument("x-delayed-type", "direct")
            .build();
    }

    @Bean
    public CustomExchange delayedExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("delayed.exchange", "x-delayed-message", true, false, args);
    }
}

// 发送延迟消息 (提前15分钟提醒)
@Scheduled(cron = "0 0 * * * ?") // 每小时执行
public void scheduleClassReminders() {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime reminderTime = now.plusMinutes(15);

    // 查询15分钟后的课程
    List<CourseSchedule> upcomingClasses = scheduleMapper.selectUpcoming(reminderTime);

    upcomingClasses.forEach(schedule -> {
        Map<String, String> reminder = new HashMap<>();
        reminder.put("studentId", "...");
        reminder.put("courseName", schedule.getCourseName());
        reminder.put("location", schedule.getLocation());
        reminder.put("time", schedule.getStartTime().toString());

        // 发送延迟消息 (15分钟后送达)
        rabbitTemplate.convertAndSend(
            "delayed.exchange",
            "class.reminder",
            reminder,
            message -> {
                message.getMessageProperties().setDelay(15 * 60 * 1000); // 15分钟
                return message;
            }
        );
    });
}
```

---

### 4. MinIO增强 - 多媒体处理

#### 功能A: 视频预览缩略图生成
```java
@PostMapping("/resource/upload")
public Result uploadResource(@RequestParam MultipartFile file) {
    String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

    // 上传原始文件
    minioClient.putObject(...);

    // 如果是视频，生成缩略图
    if (isVideo(file)) {
        BufferedImage thumbnail = generateVideoThumbnail(file);
        String thumbnailName = fileName + "-thumbnail.jpg";

        // 上传缩略图
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ImageIO.write(thumbnail, "jpg", os);
        minioClient.putObject(
            PutObjectArgs.builder()
                .bucket("course-files")
                .object(thumbnailName)
                .stream(new ByteArrayInputStream(os.toByteArray()), os.size(), -1)
                .contentType("image/jpeg")
                .build()
        );

        // 数据库记录缩略图路径
        resource.setThumbnailUrl(thumbnailName);
    }

    return Result.success();
}
```

#### 功能B: 文件分类存储
```
MinIO Buckets:
  ├── course-videos (视频文件)
  ├── course-documents (文档文件)
  ├── course-images (图片资源)
  └── user-avatars (用户头像)

自动路由:
if (fileType.equals("mp4")) {
    bucket = "course-videos";
} else if (fileType.equals("pdf")) {
    bucket = "course-documents";
}
```

---

## 🎯 优先级建议

### 高优先级 (立即实现)
1. ✅ **Redis课程缓存** - 减少数据库查询，提升性能
2. ✅ **Elasticsearch全文搜索** - 核心功能，用户体验提升明显

### 中优先级 (1-2周内)
3. ✅ **Redis分布式锁** - 防止选课超卖，保证数据一致性
4. ✅ **RabbitMQ邮件通知** - 丰富通知渠道

### 低优先级 (后续迭代)
5. ⚪ **MinIO视频缩略图** - 视觉优化
6. ⚪ **延迟队列上课提醒** - 用户体验增强
7. ⚪ **Nacos微服务** - 架构升级 (需重构)



## 5. ✅ Nacos - 配置中心

### 使用场景
集中管理应用配置,支持动态刷新(无需重启应用)

### 数据流详解

#### 场景1: 应用启动时加载配置
```
应用启动: Spring Boot 启动
  ↓
Spring Cloud 初始化
  ↓
1. 读取本地配置:
   spring.cloud.nacos.config.server-addr=localhost:8848
   spring.cloud.nacos.config.file-extension=yaml
   spring.application.name=suep-student-service
   spring.profiles.active=dev

2. 连接到 Nacos Server (localhost:8848)
   ↓
3. 根据命名规则拉取配置文件:
   Data ID: suep-student-service-dev.yaml
   Group: DEFAULT_GROUP
   ↓
4. 解析 YAML 配置内容:
   app.name = "教务管理系统"
   app.version = "1.0.0"
   app.description = "基于 Spring Boot 3 + Vue 3 的教务管理系统"
   business.course-selection.max-courses-per-student = 10
   business.course-selection.allow-duplicate = false
   business.course-selection.check-time-conflict = true
   system.announcement = "欢迎使用教务管理系统！"
   system.maintenance-mode = false

5. 通过 @Value 注解注入到 Controller:
   @Value("${app.name}")
   private String appName;  // 自动注入 "教务管理系统"

6. 注册配置监听器 (因为使用了 @RefreshScope)
   ↓
7. 应用启动完成
   后端日志: [Nacos Config] Loaded dataId [suep-student-service-dev.yaml]
  ↓
用户可访问配置接口
```

**涉及的类**:
- **Controller**: `NacosConfigController.java` (Line 15-79)
- **配置注解**: `@RefreshScope` (Line 15)
- **依赖注入**: `@Value("${app.name}")` (Line 19-34)
- **配置文件**: `application.yml` (Nacos 连接配置 Line 22-32)
- **远程配置**: Nacos 控制台中的 `suep-student-service-dev.yaml`

#### 场景2: 用户查询系统配置
```
前端操作: 管理员或学生查看系统配置信息
  ↓
前端: axios.get('http://localhost:8080/config/info')
  ↓
后端: NacosConfigController.getConfigInfo()
  ↓
1. 读取所有 @Value 注入的配置值:
   String appName = "教务管理系统";
   String appVersion = "1.0.0";
   String appDescription = "基于 Spring Boot 3 + Vue 3 的教务管理系统";
   Integer maxCourses = 10;
   Boolean allowDuplicate = false;
   Boolean checkTimeConflict = true;
   String announcement = "欢迎使用教务管理系统！";
   Boolean maintenanceMode = false;

2. 组装返回结果:
   Map<String, Object> result = new HashMap<>();
   result.put("application", Map.of(
       "name", appName,
       "version", appVersion,
       "description", appDescription
   ));
   result.put("businessConfig", Map.of(
       "maxCoursesPerStudent", maxCourses,
       "allowDuplicate", allowDuplicate,
       "checkTimeConflict", checkTimeConflict
   ));
   result.put("systemConfig", Map.of(
       "announcement", announcement,
       "maintenanceMode", maintenanceMode
   ));

3. 返回 JSON:
   {
     "application": {"name": "教务管理系统", ...},
     "businessConfig": {"maxCoursesPerStudent": 10, ...},
     "systemConfig": {"announcement": "欢迎使用教务管理系统！", ...}
   }
  ↓
前端: 显示配置信息页面
```

**涉及的类**:
- **Controller**: `NacosConfigController.java` (Line 37-59)
- **HTTP Method**: GET `/config/info`

#### 场景3: 动态修改配置(无需重启应用)⭐
```
管理员操作: 需要修改系统公告
  ↓
1. 登录 Nacos 控制台:
   浏览器打开: http://localhost:8848/nacos
   用户名: nacos
   密码: nacos
   ↓
2. 进入配置列表:
   配置管理 → 配置列表 → 找到 suep-student-service-dev.yaml
   ↓
3. 点击"编辑"按钮
   ↓
4. 修改配置内容:
   修改前:
   system:
     announcement: "欢迎使用教务管理系统！"

   修改后:
   system:
     announcement: "【紧急通知】系统将于今晚22:00进行维护，请提前保存数据"
   ↓
5. 点击"发布"按钮
  ↓
===== Nacos Server 处理 =====
1. Nacos Server 接收配置更新请求
2. 保存新配置到数据库 (版本号 +1)
3. 推送配置变更通知给所有订阅的客户端
  ↓
===== 应用端接收更新 =====
后端应用 (正在运行中，无需重启):
1. Spring Cloud Alibaba Nacos Config 监听器接收到通知
2. 下载最新配置内容
3. 对比配置变化:
   检测到 system.announcement 值改变
4. 触发 @RefreshScope Bean 刷新
   ↓
5. NacosConfigController Bean 销毁并重新创建
   @Value("${system.announcement}")
   private String announcement;  // 重新注入新值
   ↓
6. 后端控制台日志输出:
   [Nacos Config] Refresh keys changed: [system.announcement]
   [Spring Cloud] Refreshing beans annotated with @RefreshScope
  ↓
===== 验证配置生效 =====
前端操作: 访问公告接口 (无需重启应用)
  ↓
前端: axios.get('http://localhost:8080/config/announcement')
  ↓
后端: NacosConfigController.getAnnouncement()
  返回: {"announcement": "【紧急通知】系统将于今晚22:00进行维护...", "source": "Nacos Config Center"}
  ↓
前端页面显示最新公告 ✅

整个过程耗时: 约 2-5 秒
应用状态: 持续运行，无服务中断
用户影响: 无感知
```

**关键代码** - NacosConfigController.java:
```java
@RestController
@RequestMapping("/config")
@RefreshScope  // ⭐ 关键注解，使Bean支持动态刷新
public class NacosConfigController {

    @Value("${system.announcement:欢迎使用！}")
    private String announcement;  // Nacos 配置变化时自动更新

    @GetMapping("/announcement")
    public Map<String, String> getAnnouncement() {
        result.put("announcement", announcement);  // 返回最新值
        result.put("source", "Nacos Config Center");
        return result;
    }
}
```

### 为什么使用 Nacos 配置中心?

#### 优势1: 紧急故障处理
```
传统方案 (修改 application.yml):
├─ 1. 修改本地配置文件
├─ 2. 提交代码到 Git
├─ 3. 重新打包 (mvn clean package)
├─ 4. 停止应用
├─ 5. 上传新的 jar 包
├─ 6. 启动应用
└─ 7. 等待应用就绪
⏱️ 耗时: 10-30 分钟
💔 服务中断: 是

Nacos 方案:
├─ 1. Nacos 控制台修改配置
├─ 2. 点击"发布"
└─ 3. 自动生效
⏱️ 耗时: 2-5 秒
✅ 服务中断: 否
```

#### 优势2: 环境隔离配置
```
Nacos 配置列表:
├── suep-student-service-dev.yaml    (开发环境)
│   ├── announcement: "【开发环境】欢迎测试"
│   └── redis.host: localhost
│
├── suep-student-service-test.yaml   (测试环境)
│   ├── announcement: "【测试环境】请勿使用真实数据"
│   └── redis.host: 192.168.1.100
│
└── suep-student-service-prod.yaml   (生产环境)
    ├── announcement: "欢迎使用教务管理系统"
    └── redis.host: prod-redis.example.com

应用启动时根据 spring.profiles.active 自动加载对应配置
```

#### 优势3: 配置版本管理与回滚
```
Nacos 自动保存配置历史:
├── v5 (当前) - 2025-12-22 14:30 - 张三 - "紧急维护通知"
├── v4 - 2025-12-22 10:00 - 李四 - "选课开始通知"
├── v3 - 2025-12-21 18:00 - 王五 - "常规公告"
└── ...

如果 v5 配置有问题:
1. 点击"历史版本"
2. 选择 v4
3. 点击"回滚"
4. 立即恢复到 v4 配置 (2-5秒生效)
```

---

## 6. ✅ WebSocket - 站内实时通知系统

### 使用场景
学生选课、退课等事件的实时推送通知

### 数据流详解

#### 场景1: 用户登录后建立 WebSocket 连接
```
前端操作: 学生登录系统
  ↓
前端: Login.vue handleLogin() 登录成功
  ↓
1. 保存 token 到 localStorage
2. 保存 userId 到 localStorage
3. 跳转到主页
  ↓
前端: MainLayout.vue onMounted() 钩子执行
  ↓
4. 从 localStorage 读取 userId
   const userId = localStorage.getItem('username')  // 例: "2021003"
   ↓
5. 调用 WebSocket 工具类建立连接:
   import wsClient from '@/utils/websocket.js'
   wsClient.connect(userId)
   ↓
6. WebSocket 客户端构建连接 URL:
   const wsUrl = `ws://localhost:8080/ws/${userId}`
   // 完整 URL: ws://localhost:8080/ws/2021003
   ↓
7. 创建 WebSocket 对象:
   this.socket = new WebSocket(wsUrl)
   ↓
===== 后端接收连接 =====
后端: WebSocketServer.onOpen()
  ↓
1. @OnOpen 方法被触发
   public void onOpen(Session session, @PathParam("userId") String userId)
   ↓
2. 将用户会话存入内存 Map:
   onlineUsers.put("2021003", session)
   ConcurrentHashMap<String, Session> onlineUsers = {
     "2021003": Session对象,
     "2021005": Session对象,
     ...
   }
   ↓
3. 后端日志:
   [WebSocket] User connected: 2021003
  ↓
===== 前端连接成功 =====
前端: websocket.js onopen 回调
  ↓
1. 控制台输出: WebSocket connected
2. 重置重连次数: reconnectAttempts = 0
3. 启动心跳检测: startHeartbeat()
4. 通知所有消息处理器: { type: 'connect', data: { userId: "2021003" } }
  ↓
用户在线，可接收实时通知 ✅
```

**涉及的类**:
- **后端**: `WebSocketServer.java` (Line 16-19)
- **前端**: `websocket.js` (Line 16-66)
- **配置**: `WebSocketConfig.java` (WebSocket 端点配置)
- **数据结构**: `ConcurrentHashMap<String, Session>` (存储在线用户)

#### 场景2: 选课成功后推送实时通知
```
前端操作: 学生点击"选课"按钮
  ↓
前端: StudentCourse.vue handleSelectCourse()
  ↓ axios.post('/api/timetable/select', { studentId: "2021003", scheduleId: 5 })
  ↓
后端: TimetableController.selectCourse()
  ↓
1. 业务逻辑验证:
   ✓ 检查是否重复选课
   ✓ 检查时间冲突
   ✓ 检查课程容量
   ↓
2. 数据库操作:
   INSERT INTO t_student_course (student_id, schedule_id) VALUES (2021003, 5)
   UPDATE t_course_schedule SET current_count = current_count + 1 WHERE id = 5
   ↓
3. 创建站内通知记录:
   Notification notification = new Notification();
   notification.setUserId("2021003");
   notification.setType("SELECTION_SUCCESS");
   notification.setTitle("选课成功");
   notification.setContent("您已成功选修《Java程序设计》课程");
   notification.setIsRead(false);
   notification.setCreateTime(LocalDateTime.now());
   ↓
4. 保存到数据库:
   notificationService.createNotification(notification);
   INSERT INTO t_notification (user_id, type, title, content, is_read, create_time)
   VALUES ('2021003', 'SELECTION_SUCCESS', '选课成功', '您已成功选修《Java程序设计》课程', 0, '2025-12-22 15:30:00')
   ↓
5. 通过 WebSocket 推送实时消息 ⭐:
   WebSocketServer.sendInfo("2021003", "选课成功：您已成功选修《Java程序设计》课程");
   ↓
===== WebSocket 推送过程 =====
后端: WebSocketServer.sendInfo()
  ↓
1. 从内存 Map 中查找用户会话:
   Session session = onlineUsers.get("2021003");
   ↓
2. 检查会话是否有效:
   if (session != null && session.isOpen())
   ↓
3. 发送消息:
   session.getBasicRemote().sendText("选课成功：您已成功选修《Java程序设计》课程");
   ↓
4. 后端日志:
   [WebSocket] Message sent to user 2021003
  ↓
===== 前端接收消息 =====
前端: websocket.js onmessage 回调
  ↓
1. 接收到消息: event.data = "选课成功：您已成功选修《Java程序设计》课程"
   ↓
2. 解析消息 (尝试 JSON，失败则作为文本):
   const message = { content: event.data }
   ↓
3. 通知所有消息处理器:
   this.notifyHandlers({ type: 'message', data: message })
   ↓
4. NotificationCenter.vue 接收到消息:
   wsClient.onMessage((event) => {
     if (event.type === 'message') {
       // 显示通知提示
       message.success(event.data.content)
       // 刷新通知列表
       loadNotifications()
       // 更新未读数
       unreadCount.value++
     }
   })
   ↓
5. 前端页面效果:
   ✅ 右上角小铃铛数字 +1
   ✅ 弹出提示: "选课成功：您已成功选修《Java程序设计》课程"
   ✅ 通知列表自动刷新
  ↓
6. 同时发送短信通知 (通过 RabbitMQ):
   smsService.sendCourseSelectionSms(phone, name, courseName);
   // 这是独立的异步流程，不影响 WebSocket 通知
  ↓
选课流程完成返回:
res.put("code", 200);
res.put("msg", "选课成功");
  ↓
前端: 显示成功提示，刷新课程列表
```

**涉及的类**:
- **Controller**: `TimetableController.java` (Line 146-180)
- **Service**: `NotificationServiceImpl.java` (Line 47-52)
- **WebSocket**: `WebSocketServer.java` (Line 27-36)
- **前端组件**: `StudentCourse.vue`, `NotificationCenter.vue`
- **WebSocket工具**: `websocket.js`

#### 场景3: 用户退出登录时关闭 WebSocket 连接
```
前端操作: 用户点击"退出登录"
  ↓
前端: TopBar.vue handleLogout()
  ↓
1. 断开 WebSocket 连接:
   wsClient.disconnect()
   ↓
2. WebSocket 客户端关闭:
   this.isManualClose = true;  // 标记为手动关闭，不自动重连
   this.stopHeartbeat();       // 停止心跳检测
   this.socket.close();        // 关闭连接
   ↓
===== 后端接收关闭事件 =====
后端: WebSocketServer.onClose()
  ↓
1. @OnClose 方法被触发:
   public void onClose(@PathParam("userId") String userId)
   ↓
2. 从内存 Map 中移除用户:
   onlineUsers.remove("2021003");
   ↓
3. 后端日志:
   [WebSocket] User disconnected: 2021003
  ↓
4. 清除本地存储:
   localStorage.clear()
   ↓
5. 跳转到登录页:
   router.push('/login')
  ↓
用户退出，WebSocket 连接已关闭 ✅
```

**涉及的类**:
- **后端**: `WebSocketServer.java` (Line 22-24)
- **前端**: `websocket.js` (Line 89-97)
- **前端组件**: `TopBar.vue`

### WebSocket 连接管理

#### 心跳检测机制
```
前端每 30 秒发送一次心跳:
setInterval(() => {
  if (socket.readyState === WebSocket.OPEN) {
    socket.send({ type: 'ping' })
  }
}, 30000)

作用:
1. 保持连接活跃
2. 检测连接是否断开
3. 防止代理服务器超时关闭连接
```

#### 自动重连机制
```
连接断开时:
if (!isManualClose && reconnectAttempts < 5) {
  reconnectAttempts++
  setTimeout(() => {
    connect(userId)  // 重新连接
  }, 3000)  // 3秒后重试
}

重连策略:
├─ 最大重试次数: 5次
├─ 重试间隔: 3秒
└─ 手动关闭时不重连
```

### 在线用户管理
```
后端内存数据结构:
ConcurrentHashMap<String, Session> onlineUsers = {
  "2021001": Session对象 (张三),
  "2021003": Session对象 (李四),
  "2021005": Session对象 (王五),
  "admin001": Session对象 (管理员)
}

可实现功能:
1. 查询在线用户数:
   int onlineCount = onlineUsers.size();

2. 向所有在线用户广播消息:
   onlineUsers.values().forEach(session -> {
     session.getBasicRemote().sendText("系统维护通知");
   });

3. 向特定用户发送消息:
   Session session = onlineUsers.get(userId);
   session.getBasicRemote().sendText(message);
```

### WebSocket vs RabbitMQ 对比

| 对比项 | WebSocket (站内通知) | RabbitMQ (短信通知) |
|--------|---------------------|-------------------|
| **通知渠道** | 前端页面内 | 用户手机短信 |
| **技术栈** | WebSocket 协议 | 消息队列 |
| **实时性** | 毫秒级 (几乎即时) | 秒级 (异步处理) |
| **可靠性** | 用户在线时才能收到 | 用户不在线也能收到 |
| **持久化** | 存储在数据库 (t_notification) | 不持久化 (发送即删) |
| **用户体验** | 页面小铃铛提醒 | 手机短信提醒 |
| **成本** | 无额外成本 | 需要短信服务费用 |
| **适用场景** | 即时通知、系统消息 | 重要通知、离线通知 |

### 技术亮点
1. **双向通信**: 支持服务器主动推送消息给客户端
2. **长连接**: 减少 HTTP 轮询开销，降低服务器压力
3. **自动重连**: 网络断开后自动恢复连接
4. **心跳检测**: 保持连接活跃，及时发现断线
5. **并发安全**: 使用 ConcurrentHashMap 管理在线用户
6. **用户隔离**: 每个用户独立的 WebSocket 连接
