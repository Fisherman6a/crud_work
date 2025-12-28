# 学生选课管理系统 - 服务信息流传递完整文档

> 📅 最后更新：2025-12-23
> 📝 作者：bao
> 🏗️ 架构：Spring Boot 3.2.5 + Vue 3 + Nacos + Elasticsearch + RabbitMQ + MinIO + WebSocket

---

## 目录

1. [MySQL - 核心数据库](#1-mysql---核心数据库)
2. [Redis - 缓存与验证码](#2-redis---缓存与验证码)
3. [Elasticsearch - 智能搜索引擎](#3-elasticsearch---智能搜索引擎)
4. [MinIO - 对象存储服务](#4-minio---对象存储服务)
5. [RabbitMQ - 消息队列](#5-rabbitmq---消息队列)
6. [WebSocket - 实时通知](#6-websocket---实时通知)
7. [Nacos - 服务注册与监控](#7-nacos---服务注册与监控)
8. [架构层次信息流](#8-架构层次信息流)

---

## 1. ✅ MySQL - 核心数据库

### 使用场景
存储所有业务数据：学生、教师、课程、选课记录、通知等

### 数据流详解

#### 场景1: 学生选课
```
前端操作: 学生点击"选课"按钮
  ↓
前端: StudentCourse.vue handleSelectCourse()
  ↓ axios.post('/api/timetable/select', { studentId: "2021003", scheduleId: 5 })
  ↓
===== 后端 Controller 层 =====
后端: TimetableController.selectCourse()
  ↓
1. 接收请求参数:
   @PostMapping("/select")
   public Map<String, Object> selectCourse(@RequestBody Map<String, Object> params)
   String studentId = "2021003"
   Long scheduleId = 5
   ↓
2. 业务验证逻辑:
   // 检查是否重复选课
   QueryWrapper<StudentCourse> wrapper = new QueryWrapper<>();
   wrapper.eq("student_id", studentId).eq("schedule_id", scheduleId);
   List<StudentCourse> existing = studentCourseMapper.selectList(wrapper);
   if (!existing.isEmpty()) {
     return error("您已选过该课程");
   }
   ↓
   // 检查课程容量
   CourseSchedule schedule = courseScheduleMapper.selectById(scheduleId);
   if (schedule.getCurrentCount() >= schedule.getMaxCount()) {
     return error("课程已满");
   }
   ↓
   // 检查时间冲突
   List<StudentCourse> myCourses = getMyCourses(studentId);
   for (StudentCourse course : myCourses) {
     if (hasTimeConflict(course, schedule)) {
       return error("时间冲突");
     }
   }
   ↓
===== 数据库操作 =====
3. 开始事务 (@Transactional):
   ↓
4. 插入选课记录:
   StudentCourse record = new StudentCourse();
   record.setStudentId("2021003");
   record.setScheduleId(5L);
   record.setSelectTime(LocalDateTime.now());
   record.setScore(null);  // 成绩初始为空

   studentCourseMapper.insert(record);

   SQL执行:
   INSERT INTO t_student_course (student_id, schedule_id, select_time)
   VALUES ('2021003', 5, '2025-12-23 16:30:00')
   ↓
5. 更新课程人数:
   schedule.setCurrentCount(schedule.getCurrentCount() + 1);
   courseScheduleMapper.updateById(schedule);

   SQL执行:
   UPDATE t_course_schedule
   SET current_count = current_count + 1
   WHERE id = 5
   ↓
6. 创建站内通知:
   Notification notification = new Notification();
   notification.setUserId("2021003");
   notification.setType("COURSE_SELECTION");
   notification.setTitle("选课成功");
   notification.setContent("您已成功选修《Java程序设计》课程");
   notification.setCourseId(course.getId());
   notification.setScheduleId(scheduleId);
   notification.setIsRead(false);
   notification.setCreateTime(LocalDateTime.now());

   notificationMapper.insert(notification);

   SQL执行:
   INSERT INTO t_notification
   (user_id, type, title, content, course_id, schedule_id, is_read, create_time)
   VALUES ('2021003', 'COURSE_SELECTION', '选课成功',
           '您已成功选修《Java程序设计》课程', 1, 5, 0, '2025-12-23 16:30:00')
   ↓
7. 提交事务:
   Transaction commit ✅
   ↓
===== 异步通知 =====
8. WebSocket 实时推送:
   WebSocketServer.sendInfo("2021003", "选课成功：您已成功选修《Java程序设计》课程");
   (详见 WebSocket 章节)
   ↓
9. RabbitMQ 短信通知:
   if (student.getPhone() != null) {
     smsService.sendCourseSelectionSms(
       student.getPhone(),
       student.getName(),
       course.getName()
     );
   }
   (详见 RabbitMQ 章节)
   ↓
10. 返回响应:
    res.put("code", 200);
    res.put("msg", "选课成功");
    return res;
  ↓
前端: 显示成功提示，刷新课程列表
```

**涉及的类**:
- **Controller**: `TimetableController.java` (Line 123-184)
- **Mapper**: `StudentCourseMapper.java`, `CourseScheduleMapper.java`, `NotificationMapper.java`
- **Entity**: `StudentCourse.java`, `CourseSchedule.java`, `Notification.java`
- **数据库表**: `t_student_course`, `t_course_schedule`, `t_notification`

**SQL 语句汇总**:
```sql
-- 1. 检查重复选课
SELECT * FROM t_student_course
WHERE student_id = '2021003' AND schedule_id = 5

-- 2. 查询课程信息
SELECT * FROM t_course_schedule WHERE id = 5

-- 3. 插入选课记录
INSERT INTO t_student_course (student_id, schedule_id, select_time)
VALUES ('2021003', 5, NOW())

-- 4. 更新课程人数
UPDATE t_course_schedule SET current_count = current_count + 1 WHERE id = 5

-- 5. 插入通知记录
INSERT INTO t_notification (...) VALUES (...)
```

#### 场景2: 查询学生已选课程
```
前端操作: 进入"我的课程"页面
  ↓
前端: MyCourses.vue onMounted()
  ↓ axios.get('/api/timetable/my-courses?studentId=2021003')
  ↓
===== 后端处理 =====
后端: TimetableController.getMyCourses()
  ↓
1. 接收参数:
   @GetMapping("/my-courses")
   public List<Map<String, Object>> getMyCourses(@RequestParam String studentId)
   ↓
2. 多表联查 SQL (MyBatis-Plus):
   QueryWrapper<StudentCourse> wrapper = new QueryWrapper<>();
   wrapper.eq("student_id", studentId);
   List<StudentCourse> records = studentCourseMapper.selectList(wrapper);

   SQL执行:
   SELECT sc.*, cs.*, c.*, t.*
   FROM t_student_course sc
   LEFT JOIN t_course_schedule cs ON sc.schedule_id = cs.id
   LEFT JOIN t_course c ON cs.course_id = c.id
   LEFT JOIN t_teacher t ON cs.teacher_id = t.id
   WHERE sc.student_id = '2021003'
   ORDER BY sc.select_time DESC
   ↓
3. 数据组装:
   for (StudentCourse record : records) {
     CourseSchedule schedule = courseScheduleMapper.selectById(record.getScheduleId());
     Course course = courseMapper.selectById(schedule.getCourseId());
     Teacher teacher = teacherMapper.selectById(schedule.getTeacherId());

     Map<String, Object> item = new HashMap<>();
     item.put("courseName", course.getName());
     item.put("teacherName", teacher.getName());
     item.put("classTime", schedule.getClassTime());
     item.put("classroom", schedule.getClassroom());
     item.put("score", record.getScore());
     result.add(item);
   }
   ↓
4. 返回 JSON:
   [
     {
       "courseName": "Java程序设计",
       "teacherName": "张三",
       "classTime": "周一 1-2节",
       "classroom": "A101",
       "score": null
     },
     ...
   ]
  ↓
前端: 渲染课程列表
```

**涉及的类**:
- **Controller**: `TimetableController.java`
- **Mapper**: `StudentCourseMapper.java`, `CourseScheduleMapper.java`, `CourseMapper.java`, `TeacherMapper.java`

---

## 2. ✅ Redis - 缓存与验证码

### 使用场景
1. 图形验证码存储（防止暴力破解）
2. 用户会话缓存（可选）
3. 热点数据缓存（可选）

### 数据流详解

#### 场景1: 生成登录验证码
```
前端操作: 打开登录页面
  ↓
前端: Login.vue onMounted()
  ↓
1. 生成唯一 UUID:
   const captchaKey = `captcha_${Date.now()}_${Math.random()}`
   // 例: "captcha_1703345678123_0.456789"
   ↓
2. 请求验证码图片:
   axios.get(`/api/captcha/image?key=${captchaKey}`)
  ↓
===== 后端生成验证码 =====
后端: CaptchaController.generateCaptcha()
  ↓
1. 接收参数:
   @GetMapping("/image")
   public void generateCaptcha(@RequestParam String key, HttpServletResponse response)
   String captchaKey = "captcha_1703345678123_0.456789"
   ↓
2. 生成随机验证码文本:
   String code = RandomUtil.randomNumbers(4);  // 例: "5739"
   ↓
3. 存储到 Redis (5分钟过期):
   redisTemplate.opsForValue().set(
     captchaKey,          // key: "captcha_1703345678123_0.456789"
     code,                // value: "5739"
     5,                   // 过期时间: 5
     TimeUnit.MINUTES     // 单位: 分钟
   );

   Redis 数据:
   {
     "captcha_1703345678123_0.456789": "5739",
     TTL: 300秒
   }
   ↓
4. 生成验证码图片:
   BufferedImage image = createCaptchaImage(code);
   // 生成 100x40 的图片，包含文字 "5739" + 噪点 + 干扰线
   ↓
5. 输出图片到响应流:
   response.setContentType("image/png");
   ImageIO.write(image, "png", response.getOutputStream());
  ↓
前端: 显示验证码图片
```

**涉及的类**:
- **Controller**: `CaptchaController.java`
- **Redis**: `RedisTemplate<String, Object>`
- **工具**: HuTool `RandomUtil`, Java `BufferedImage`

#### 场景2: 登录验证
```
前端操作: 输入账号密码验证码，点击"登录"
  ↓
前端: Login.vue handleLogin()
  ↓
前端数据:
{
  "username": "2021003",
  "password": "123456",
  "captchaKey": "captcha_1703345678123_0.456789",
  "captchaCode": "5739"  // 用户输入的验证码
}
  ↓ axios.post('/api/user/login', ...)
  ↓
===== 后端验证 =====
后端: UserController.login()
  ↓
1. 从 Redis 读取正确验证码:
   String correctCode = (String) redisTemplate.opsForValue().get(captchaKey);
   // correctCode = "5739" (从 Redis 获取)
   ↓
2. 验证码校验:
   if (correctCode == null) {
     return error("验证码已过期");
   }
   if (!correctCode.equalsIgnoreCase(captchaCode)) {
     return error("验证码错误");
   }
   ↓
3. 验证通过后立即删除 (防止重复使用):
   redisTemplate.delete(captchaKey);

   Redis 操作:
   DEL "captcha_1703345678123_0.456789"
   ↓
4. 验证账号密码:
   QueryWrapper<User> wrapper = new QueryWrapper<>();
   wrapper.eq("username", username);
   User user = userMapper.selectOne(wrapper);

   if (user == null || !user.getPassword().equals(MD5(password))) {
     return error("用户名或密码错误");
   }
   ↓
5. 生成 Token:
   String token = JWTUtil.createToken(user.getId());
   ↓
6. 返回登录信息:
   {
     "code": 200,
     "msg": "登录成功",
     "data": {
       "token": "eyJhbGciOiJIUzI1NiIs...",
       "userId": "2021003",
       "username": "张三",
       "role": "student"
     }
   }
  ↓
前端: 保存 token 到 localStorage，跳转到主页
```

**涉及的类**:
- **Controller**: `UserController.java`
- **Redis**: `RedisTemplate<String, Object>`
- **Mapper**: `UserMapper.java`

**Redis 命令汇总**:
```bash
# 1. 存储验证码（5分钟过期）
SET "captcha_1703345678123_0.456789" "5739" EX 300

# 2. 读取验证码
GET "captcha_1703345678123_0.456789"

# 3. 删除验证码（验证后立即删除）
DEL "captcha_1703345678123_0.456789"

# 4. 查看 TTL
TTL "captcha_1703345678123_0.456789"  # 返回剩余秒数
```

---

## 3. ✅ Elasticsearch - 智能搜索引擎

### 使用场景
课程资料的全文搜索、高亮显示、智能匹配

### 数据流详解

#### 场景1: 上传课程资料并同步到 Elasticsearch
```
前端操作: 教师上传课程资料
  ↓
前端: CourseMaterial.vue handleUpload()
  ↓
1. 准备文件:
   const formData = new FormData();
   formData.append('file', file.raw);  // PDF文件
   formData.append('courseId', courseId);  // 课程ID: 1
   ↓
2. 发送请求:
   axios.post('/api/resource/upload', formData, {
     headers: { 'Content-Type': 'multipart/form-data' }
   })
  ↓
===== 后端 Service 层处理 =====
后端: CourseResourceServiceImpl.uploadResource()
  ↓
1. 接收文件:
   @PostMapping("/upload")
   public Map<String, Object> uploadResource(
     @RequestParam("file") MultipartFile file,
     @RequestParam("courseId") Long courseId
   )

   文件信息:
   - 原始文件名: "Java面向对象编程.pdf"
   - 文件大小: 2.5 MB
   - 内容类型: application/pdf
   ↓
2. 生成唯一文件名:
   String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();
   // 例: "a1b2c3d4-5e6f-7g8h-9i0j-k1l2m3n4o5p6-Java面向对象编程.pdf"
   ↓
3. 上传到 MinIO (对象存储):
   minioClient.putObject(
     PutObjectArgs.builder()
       .bucket("course-files")
       .object(fileName)
       .stream(file.getInputStream(), file.getSize(), -1)
       .contentType("application/pdf")
       .build()
   );

   MinIO 存储路径:
   minio://course-files/a1b2c3d4-5e6f-7g8h-9i0j-k1l2m3n4o5p6-Java面向对象编程.pdf
   ↓
4. 保存记录到 MySQL:
   CourseResource resource = new CourseResource();
   resource.setCourseId(1L);
   resource.setResourceName("Java面向对象编程.pdf");
   resource.setResourceType("pdf");
   resource.setResourceUrl(fileName);
   resource.setCreateTime(LocalDateTime.now());

   resourceMapper.insert(resource);

   MySQL 数据:
   INSERT INTO t_course_resource
   (course_id, resource_name, resource_type, resource_url, create_time)
   VALUES (1, 'Java面向对象编程.pdf', 'pdf', 'a1b2c3d4-...pdf', NOW())

   返回自增ID: resource.getId() = 15
   ↓
===== 同步到 Elasticsearch =====
5. 查询课程和教师信息:
   Course course = courseMapper.selectById(1L);
   // course.getName() = "Java程序设计"
   // course.getDescription() = "面向对象编程基础课程"

   Teacher teacher = teacherMapper.selectById(course.getTeacherId());
   // teacher.getName() = "张三"
   ↓
6. 构建 Elasticsearch 文档:
   CourseResourceDocument document = new CourseResourceDocument();
   document.setId(15L);
   document.setResourceName("Java面向对象编程.pdf");
   document.setResourceType("pdf");
   document.setCourseId(1L);
   document.setCourseName("Java程序设计");
   document.setCourseDescription("面向对象编程基础课程");
   document.setTeacherName("张三");
   document.setCredit(4);
   document.setCreateTime("2025-12-23 16:30:00");
   ↓
7. 保存到 Elasticsearch:
   elasticsearchOperations.save(document);

   Elasticsearch 索引数据:
   PUT /course_resources/_doc/15
   {
     "id": 15,
     "resourceName": "Java面向对象编程.pdf",  // 使用 ik_max_word 分词
     "resourceType": "pdf",
     "courseId": 1,
     "courseName": "Java程序设计",  // 使用 ik_max_word 分词
     "courseDescription": "面向对象编程基础课程",
     "teacherName": "张三",
     "credit": 4,
     "createTime": "2025-12-23 16:30:00"
   }

   分词结果 (ik_max_word):
   "Java面向对象编程.pdf" → ["Java", "面向", "对象", "编程", "pdf"]
   "Java程序设计" → ["Java", "程序", "设计", "程序设计"]
   ↓
8. 返回成功响应:
   {
     "success": true,
     "resourceId": 15,
     "fileName": "a1b2c3d4-...pdf",
     "message": "上传成功"
   }
  ↓
前端: 显示成功提示，刷新资料列表
```

**涉及的类**:
- **Service**: `CourseResourceServiceImpl.java` (Line 82-127)
- **Document**: `CourseResourceDocument.java`
- **Elasticsearch**: `ElasticsearchOperations`
- **MinIO**: `MinioClient`
- **Mapper**: `CourseResourceMapper.java`, `CourseMapper.java`, `TeacherMapper.java`

#### 场景2: 搜索课程资料
```
前端操作: 学生在搜索框输入"Java 面向对象"
  ↓
前端: StudentMaterials.vue handleSearch()
  ↓
前端请求:
axios.get('/api/course/search-resources', {
  params: {
    keyword: "Java 面向对象",
    resourceType: "",  // 空表示不过滤类型
    courseId: null,    // null表示搜索全部课程
    pageNum: 1,
    pageSize: 10
  }
})
  ↓
===== 后端 Service 层处理 =====
后端: CourseServiceImpl.searchCourseResources()
  ↓
1. 构建 Elasticsearch 查询:
   BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
   ↓
2. 添加多字段匹配 (Multi-Match Query):
   MultiMatchQuery multiMatchQuery = MultiMatchQuery.of(m -> m
     .query("Java 面向对象")
     .fields(
       "resourceName^3.0",       // 资源名称权重 3.0 (最重要)
       "courseName^2.0",         // 课程名称权重 2.0
       "teacherName^1.5",        // 教师名称权重 1.5
       "courseDescription^1.0"   // 课程描述权重 1.0
     )
     .type(TextQueryType.BestFields)  // 最佳字段匹配
     .fuzziness("AUTO")               // 模糊匹配 (容错)
   );

   boolQueryBuilder.must(Query.of(q -> q.multiMatch(multiMatchQuery)));
   ↓
3. 配置高亮:
   List<HighlightField> highlightFields = Arrays.asList(
     new HighlightField("resourceName"),
     new HighlightField("courseName"),
     new HighlightField("teacherName"),
     new HighlightField("courseDescription")
   );
   Highlight highlight = new Highlight(highlightFields);
   ↓
4. 执行搜索:
   NativeQuery searchQuery = NativeQuery.builder()
     .withQuery(Query.of(q -> q.bool(boolQueryBuilder.build())))
     .withHighlightQuery(new HighlightQuery(highlight, CourseResourceDocument.class))
     .withPageable(PageRequest.of(0, 10))
     .build();

   SearchHits<CourseResourceDocument> searchHits =
     elasticsearchOperations.search(searchQuery, CourseResourceDocument.class);

   Elasticsearch DSL (等价于):
   GET /course_resources/_search
   {
     "query": {
       "bool": {
         "must": [
           {
             "multi_match": {
               "query": "Java 面向对象",
               "fields": [
                 "resourceName^3.0",
                 "courseName^2.0",
                 "teacherName^1.5",
                 "courseDescription^1.0"
               ],
               "type": "best_fields",
               "fuzziness": "AUTO"
             }
           }
         ]
       }
     },
     "highlight": {
       "fields": {
         "resourceName": {},
         "courseName": {},
         "teacherName": {},
         "courseDescription": {}
       }
     },
     "from": 0,
     "size": 10
   }
   ↓
5. Elasticsearch 分词匹配过程:
   查询词分词:
   "Java 面向对象" → ["Java", "面向", "对象"]

   索引中的文档:
   文档1: "Java面向对象编程.pdf"
   - 分词: ["Java", "面向", "对象", "编程", "pdf"]
   - 匹配字段: resourceName (权重 3.0)
   - 匹配词: Java ✅, 面向 ✅, 对象 ✅
   - 评分: 8.5 (高分)

   文档2: "数据结构与算法.pdf"
   - 分词: ["数据", "结构", "算法", "pdf"]
   - 匹配词: 无
   - 评分: 0 (不返回)

   文档3: "Java程序设计"
   - 分词: ["Java", "程序", "设计", "程序设计"]
   - 匹配词: Java ✅
   - 匹配字段: courseName (权重 2.0)
   - 评分: 3.2 (较低分)
   ↓
6. 处理搜索结果:
   List<ResourceResponse> responses = searchHits.getSearchHits().stream()
     .map(hit -> {
       CourseResourceDocument doc = hit.getContent();
       ResourceResponse response = new ResourceResponse();

       response.setId(doc.getId());
       response.setResourceName(doc.getResourceName());
       response.setCourseName(doc.getCourseName());
       response.setTeacherName(doc.getTeacherName());
       response.setResourceType(doc.getResourceType());
       response.setScore(Double.valueOf(hit.getScore()));  // 相关性评分

       // 处理高亮
       Map<String, String> highlights = new HashMap<>();
       if (!hit.getHighlightFields().isEmpty()) {
         hit.getHighlightFields().forEach((field, values) -> {
           if (!values.isEmpty()) {
             highlights.put(field, values.get(0));
             // 例: "resourceName" → "<em>Java</em><em>面向</em><em>对象</em>编程.pdf"
           }
         });
       }
       response.setHighlights(highlights);

       return response;
     })
     .collect(Collectors.toList());
   ↓
7. 返回搜索结果:
   {
     "code": 200,
     "msg": "搜索成功",
     "total": 3,
     "data": [
       {
         "id": 15,
         "resourceName": "Java面向对象编程.pdf",
         "courseName": "Java程序设计",
         "teacherName": "张三",
         "resourceType": "pdf",
         "score": 8.5,
         "highlights": {
           "resourceName": "<em>Java</em><em>面向</em><em>对象</em>编程.pdf"
         }
       },
       {
         "id": 23,
         "resourceName": "OOP基础讲义.pptx",
         "courseName": "Java程序设计",
         "teacherName": "张三",
         "resourceType": "pptx",
         "score": 5.2,
         "highlights": {
           "courseName": "<em>Java</em>程序设计"
         }
       }
     ]
   }
  ↓
前端: 渲染搜索结果，高亮显示匹配词
```

**涉及的类**:
- **Service**: `CourseServiceImpl.java` (Line 189-260)
- **Controller**: `CourseController.java` (搜索接口)
- **Document**: `CourseResourceDocument.java`
- **Response**: `ResourceResponse.java`, `SearchResponse.java`

**Elasticsearch 关键配置**:
```java
@Document(indexName = "course_resources")
@Setting(shards = 1, replicas = 0)
public class CourseResourceDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Text,
           analyzer = "ik_max_word",      // 索引时细粒度分词
           searchAnalyzer = "ik_smart")   // 搜索时粗粒度分词
    private String resourceName;

    @Field(type = FieldType.Text,
           analyzer = "ik_max_word",
           searchAnalyzer = "ik_smart")
    private String courseName;

    @Field(type = FieldType.Keyword)  // 不分词，精确匹配
    private String resourceType;
}
```

---

## 4. ✅ MinIO - 对象存储服务

### 使用场景
存储课程资料文件（PDF、PPT、视频等）

### 数据流详解

#### 场景1: 上传文件到 MinIO
```
(接 Elasticsearch 章节场景1 - 步骤3)
  ↓
后端: CourseResourceServiceImpl.uploadResource()
  ↓
1. 获取文件流:
   InputStream inputStream = file.getInputStream();
   long fileSize = file.getSize();  // 2.5 MB = 2621440 字节
   ↓
2. 构建上传参数:
   PutObjectArgs args = PutObjectArgs.builder()
     .bucket("course-files")  // 桶名称
     .object("a1b2c3d4-...pdf")  // 对象名称（文件路径）
     .stream(inputStream, fileSize, -1)  // 文件流
     .contentType("application/pdf")  // MIME 类型
     .build();
   ↓
3. 执行上传:
   minioClient.putObject(args);

   MinIO 内部操作:
   ├─ 1. 检查 bucket "course-files" 是否存在
   ├─ 2. 将文件分块上传（默认 5MB/块）
   ├─ 3. 计算文件 MD5 校验和
   ├─ 4. 存储到磁盘: /data/course-files/a1b2c3d4-...pdf
   └─ 5. 返回 ETag: "d41d8cd98f00b204e9800998ecf8427e"
   ↓
4. 上传成功后数据状态:
   MinIO 对象列表:
   course-files/
   ├── a1b2c3d4-...-Java面向对象编程.pdf (2.5 MB)
   ├── b2c3d4e5-...-数据结构讲义.pptx (5.8 MB)
   └── c3d4e5f6-...-课程视频.mp4 (128 MB)
  ↓
文件上传完成 ✅
```

**涉及的类**:
- **Service**: `CourseResourceServiceImpl.java` (Line 92-101)
- **MinIO**: `MinioClient`
- **配置**: `MinioConfig.java`

#### 场景2: 获取文件预览 URL
```
前端操作: 学生点击课程资料的"预览"按钮
  ↓
前端: StudentMaterials.vue handlePreview()
  ↓
axios.get(`/api/resource/preview/${resourceId}`)
// 例: GET /api/resource/preview/15
  ↓
===== 后端生成临时访问链接 =====
后端: CourseResourceController.previewResource()
  ↓
1. 查询资源信息:
   CourseResource resource = resourceMapper.selectById(15L);

   MySQL 查询:
   SELECT * FROM t_course_resource WHERE id = 15

   结果:
   resource.getResourceUrl() = "a1b2c3d4-...pdf"
   resource.getResourceName() = "Java面向对象编程.pdf"
   resource.getResourceType() = "pdf"
   ↓
2. 生成预签名 URL (24小时有效):
   String url = minioClient.getPresignedObjectUrl(
     GetPresignedObjectUrlArgs.builder()
       .method(Method.GET)  // HTTP GET 方法
       .bucket("course-files")
       .object("a1b2c3d4-...pdf")
       .expiry(24 * 60 * 60)  // 过期时间: 24小时
       .build()
   );

   生成的 URL:
   http://localhost:9000/course-files/a1b2c3d4-...pdf
     ?X-Amz-Algorithm=AWS4-HMAC-SHA256
     &X-Amz-Credential=minioadmin/20251223/us-east-1/s3/aws4_request
     &X-Amz-Date=20251223T083000Z
     &X-Amz-Expires=86400
     &X-Amz-SignedHeaders=host
     &X-Amz-Signature=abc123def456...

   URL 特点:
   ├─ 无需认证即可访问（签名已包含）
   ├─ 24小时后自动失效
   ├─ 无法篡改（签名验证）
   └─ 可直接在浏览器打开
   ↓
3. 返回预览信息:
   {
     "url": "http://localhost:9000/course-files/...",
     "fileName": "Java面向对象编程.pdf",
     "fileType": "pdf"
   }
  ↓
前端:
- 如果是 PDF/图片: 在新标签页打开预览
- 如果是 PPT/Word: 提示下载
- 如果是视频: 使用视频播放器播放
```

**涉及的类**:
- **Controller**: `CourseResourceController.java`
- **Service**: `CourseResourceServiceImpl.java` (Line 146-175)
- **MinIO**: `MinioClient.getPresignedObjectUrl()`

**MinIO 配置 (application.yml)**:
```yaml
minio:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket-name: course-files
```

---

## 5. ✅ RabbitMQ - 消息队列

### 使用场景
异步发送短信通知（选课、退课、成绩发布等）

### 数据流详解

#### 场景1: 选课成功发送短信通知
```
(接 MySQL 章节场景1 - 步骤9)
  ↓
后端: TimetableController.selectCourse()
  ↓
1. 检查学生是否有手机号:
   Student student = studentMapper.selectById("2021003");
   if (student.getPhone() != null && !student.getPhone().isEmpty()) {
     // 有手机号，发送短信
   }

   student.getPhone() = "13800138000"
   student.getName() = "张三"
   ↓
2. 调用短信服务:
   smsService.sendCourseSelectionSms(
     "13800138000",  // 手机号
     "张三",          // 学生姓名
     "Java程序设计"   // 课程名称
   );
  ↓
===== Service 层 - 消息生产者 =====
后端: SmsService.sendCourseSelectionSms()
  ↓
1. 检查 RabbitMQ 是否可用:
   if (rabbitTemplate == null) {
     System.out.println("RabbitMQ未配置，跳过短信发送");
     return;  // 不影响主流程
   }
   ↓
2. 构建短信消息:
   Map<String, String> smsMap = new HashMap<>();
   smsMap.put("phone", "13800138000");
   smsMap.put("msg", "【教务系统】张三同学，您已成功选修《Java程序设计》课程。");
   smsMap.put("type", "COURSE_SELECTION");
   ↓
3. 发送消息到队列 (耗时 < 10ms):
   rabbitTemplate.convertAndSend("sms.queue", smsMap);

   RabbitMQ 内部操作:
   ├─ 1. 将 Map 序列化为 JSON (使用 Jackson2JsonMessageConverter)
   │   {
   │     "phone": "13800138000",
   │     "msg": "【教务系统】张三同学，您已成功选修《Java程序设计》课程。",
   │     "type": "COURSE_SELECTION"
   │   }
   ├─ 2. 发送到 RabbitMQ Server
   ├─ 3. 消息进入 "sms.queue" 队列
   └─ 4. 返回确认 (ACK)
   ↓
4. 控制台日志:
   选课短信已发送到消息队列: 13800138000
   ↓
主线程立即返回 (不等待短信发送完成) ✅
  ↓
===== 异步处理 - 消息消费者 =====
后端: SmsConsumer.process() (独立线程)
  ↓
1. @RabbitListener 自动监听队列:
   @RabbitListener(queues = "sms.queue")
   public void process(Map<String, String> smsMap)
   ↓
2. 接收消息 (从队列中取出):
   String phone = smsMap.get("phone");      // "13800138000"
   String message = smsMap.get("msg");       // "【教务系统】张三同学..."
   String type = smsMap.get("type");         // "COURSE_SELECTION"
   ↓
3. 控制台输出:
   === 短信发送任务 ===
   接收人: 13800138000
   消息类型: COURSE_SELECTION
   内容: 【教务系统】张三同学，您已成功选修《Java程序设计》课程。
   ↓
4. 检查是否配置阿里云短信服务:
   if (accessKeyId == null || accessKeyId.isEmpty()) {
     System.out.println(">>> 模拟发送成功 (未配置阿里云短信服务)");
     return;  // 开发环境：模拟发送
   }
   ↓
5. 调用阿里云短信 API (生产环境):
   sendAliYunSms(phone, message);

   阿里云短信 API 调用:
   POST https://dysmsapi.aliyuncs.com/
   {
     "PhoneNumbers": "13800138000",
     "SignName": "教务系统",
     "TemplateCode": "SMS_123456",
     "TemplateParam": "{\"message\":\"张三同学，您已成功选修《Java程序设计》课程。\"}"
   }
   ↓
6. 短信发送结果:
   if (response.getCode().equals("OK")) {
     System.out.println(">>> 短信发送成功");
   } else {
     System.err.println(">>> 短信发送失败: " + response.getMessage());
   }
  ↓
短信异步发送完成 ✅
用户手机收到短信 📱
```

**涉及的类**:
- **Service**: `SmsService.java` (消息生产者 Line 26-39)
- **Consumer**: `SmsConsumer.java` (消息消费者 Line 47-70)
- **Config**: `RabbitConfig.java` (队列配置)
- **RabbitMQ**: `RabbitTemplate`, `@RabbitListener`

**时间线对比**:
```
同步方式 (不使用 RabbitMQ):
├─ 1. 数据库写入 (50ms)
├─ 2. 调用阿里云短信 API (2000ms) ← 卡住！
└─ 3. 返回响应
⏱️ 总耗时: 2050ms

异步方式 (使用 RabbitMQ):
├─ 1. 数据库写入 (50ms)
├─ 2. 发送消息到 RabbitMQ (5ms) ← 快速！
└─ 3. 返回响应
⏱️ 总耗时: 55ms

... 3秒后，后台独立线程发送短信 (2000ms)
用户完全无感知 ✅
```

**RabbitMQ 队列配置**:
```java
@Bean
public Queue smsQueue() {
    return new Queue("sms.queue", true);  // durable=true 队列持久化
}

@Bean
public MessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();  // JSON 序列化
}
```

**RabbitMQ 配置 (application.yml)**:
```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
```

---

## 6. ✅ WebSocket - 实时通知

### 使用场景
站内实时消息推送（选课成功、退课成功、系统通知等）

### 数据流详解

#### 场景1: 用户登录建立 WebSocket 连接
```
前端操作: 学生登录系统
  ↓
前端: Login.vue handleLogin() 登录成功
  ↓
1. 保存登录信息:
   localStorage.setItem('token', 'eyJhbGciOiJIUzI1NiIs...');
   localStorage.setItem('username', '2021003');
   ↓
2. 跳转到主页:
   router.push('/main');
  ↓
前端: MainLayout.vue onMounted() 钩子执行
  ↓
3. 读取 userId:
   const userId = localStorage.getItem('username');  // "2021003"
   ↓
4. 建立 WebSocket 连接:
   import wsClient from '@/utils/websocket.js'
   wsClient.connect(userId);
   ↓
===== WebSocket 客户端 =====
前端: websocket.js connect()
  ↓
1. 构建 WebSocket URL:
   const wsUrl = `ws://localhost:8080/ws/${userId}`;
   // 完整 URL: ws://localhost:8080/ws/2021003
   ↓
2. 创建 WebSocket 连接:
   this.socket = new WebSocket(wsUrl);
   ↓
3. 注册事件监听:
   this.socket.onopen = () => { ... };     // 连接成功
   this.socket.onmessage = () => { ... };  // 接收消息
   this.socket.onclose = () => { ... };    // 连接关闭
   this.socket.onerror = () => { ... };    // 连接错误
   ↓
===== 后端接收连接 =====
后端: WebSocketServer.onOpen()
  ↓
1. @OnOpen 方法触发:
   @OnOpen
   public void onOpen(
     Session session,
     @PathParam("userId") String userId
   )

   参数:
   session = javax.websocket.Session@a1b2c3d4
   userId = "2021003"
   ↓
2. 存储到内存 Map:
   onlineUsers.put("2021003", session);

   ConcurrentHashMap<String, Session> onlineUsers = {
     "2021001": Session@xyz123,
     "2021003": Session@a1b2c3d4,  ← 新增
     "2021005": Session@def456,
     ...
   }
   ↓
3. 后端日志:
   [WebSocket] User connected: 2021003
   [WebSocket] Current online users: 3
   ↓
===== 前端连接成功 =====
前端: websocket.js onopen 回调
  ↓
1. 控制台输出:
   WebSocket connected successfully
   ↓
2. 重置重连计数:
   this.reconnectAttempts = 0;
   ↓
3. 启动心跳检测:
   this.startHeartbeat();

   setInterval(() => {
     if (this.socket.readyState === WebSocket.OPEN) {
       this.socket.send(JSON.stringify({ type: 'ping' }));
     }
   }, 30000);  // 每30秒发送一次心跳
   ↓
4. 通知所有消息处理器:
   this.notifyHandlers({
     type: 'connect',
     data: { userId: '2021003' }
   });
  ↓
WebSocket 连接建立成功 ✅
用户在线，可接收实时通知
```

**涉及的类**:
- **后端**: `WebSocketServer.java` (Line 29-38)
- **配置**: `WebSocketConfig.java`
- **前端**: `websocket.js` (Line 16-66)
- **组件**: `MainLayout.vue`

#### 场景2: 选课成功推送实时通知
```
(接 MySQL 章节场景1 - 步骤8)
  ↓
后端: TimetableController.selectCourse()
  ↓
1. 调用 WebSocket 推送:
   WebSocketServer.sendInfo(
     "2021003",  // 目标用户ID
     "选课成功：您已成功选修《Java程序设计》课程"  // 消息内容
   );
  ↓
===== WebSocket 服务端推送 =====
后端: WebSocketServer.sendInfo()
  ↓
1. 静态方法，从内存 Map 查找用户:
   Session session = onlineUsers.get("2021003");
   ↓
2. 检查会话是否有效:
   if (session == null) {
     System.out.println("[WebSocket] User not online: 2021003");
     return;  // 用户不在线，不发送
   }

   if (!session.isOpen()) {
     System.out.println("[WebSocket] Session closed: 2021003");
     onlineUsers.remove("2021003");  // 清理无效会话
     return;
   }
   ↓
3. 发送消息:
   session.getBasicRemote().sendText("选课成功：您已成功选修《Java程序设计》课程");

   底层操作:
   ├─ 1. 将字符串编码为 WebSocket 帧
   ├─ 2. 通过 TCP 连接发送
   └─ 3. 客户端接收 WebSocket 帧
   ↓
4. 后端日志:
   [WebSocket] Message sent to user: 2021003
   [WebSocket] Content: 选课成功：您已成功选修《Java程序设计》课程
  ↓
===== 前端接收消息 =====
前端: websocket.js onmessage 回调
  ↓
1. 接收消息:
   socket.onmessage = (event) => {
     const messageData = event.data;
     // messageData = "选课成功：您已成功选修《Java程序设计》课程"
   }
   ↓
2. 解析消息 (尝试 JSON，失败则作为文本):
   let message;
   try {
     message = JSON.parse(messageData);
   } catch (e) {
     message = { content: messageData };
   }

   最终:
   message = {
     content: "选课成功：您已成功选修《Java程序设计》课程"
   }
   ↓
3. 通知所有消息处理器:
   this.notifyHandlers({
     type: 'message',
     data: message
   });
  ↓
===== 前端组件处理 =====
前端: NotificationCenter.vue 接收消息
  ↓
1. 消息处理器触发:
   wsClient.onMessage((event) => {
     if (event.type === 'message') {
       handleNewMessage(event.data);
     }
   });
   ↓
2. 显示通知提示:
   message.success(event.data.content);
   // Naive UI 弹出提示框: "选课成功：您已成功选修《Java程序设计》课程"
   ↓
3. 刷新通知列表:
   loadNotifications();

   axios.get('/api/notification/list', {
     params: { userId: '2021003', isRead: false }
   })
   ↓
4. 更新未读数:
   unreadCount.value++;
   // 右上角小铃铛数字 +1
  ↓
前端页面效果:
✅ 右上角小铃铛数字 +1 (例: 3 → 4)
✅ 弹出提示: "选课成功：您已成功选修《Java程序设计》课程"
✅ 通知列表自动刷新
✅ 新通知标记为未读 (红点)
```

**涉及的类**:
- **后端**: `WebSocketServer.java` (Line 63-84)
- **Controller**: `TimetableController.java` (Line 164-165)
- **前端**: `websocket.js` (Line 68-87)
- **组件**: `NotificationCenter.vue`

**WebSocket 数据结构**:
```java
// 后端内存存储
ConcurrentHashMap<String, Session> onlineUsers = {
  "2021001": Session对象 (张三),
  "2021003": Session对象 (李四),  ← 目标用户
  "2021005": Session对象 (王五),
  "admin001": Session对象 (管理员)
}

// 可实现功能:
1. 单播: 向指定用户发送消息
   sendInfo("2021003", "消息内容");

2. 广播: 向所有在线用户发送消息
   onlineUsers.values().forEach(session -> {
     session.getBasicRemote().sendText("系统维护通知");
   });

3. 查询在线用户数
   int count = onlineUsers.size();
```

#### 场景3: 用户退出登录关闭连接
```
前端操作: 用户点击"退出登录"
  ↓
前端: TopBar.vue handleLogout()
  ↓
1. 断开 WebSocket:
   wsClient.disconnect();
   ↓
前端: websocket.js disconnect()
  ↓
2. 标记手动关闭 (不自动重连):
   this.isManualClose = true;
   ↓
3. 停止心跳检测:
   this.stopHeartbeat();
   clearInterval(this.heartbeatTimer);
   ↓
4. 关闭 WebSocket 连接:
   this.socket.close();
  ↓
===== 后端接收关闭事件 =====
后端: WebSocketServer.onClose()
  ↓
1. @OnClose 方法触发:
   @OnClose
   public void onClose(@PathParam("userId") String userId)
   ↓
2. 从内存 Map 移除用户:
   onlineUsers.remove("2021003");

   ConcurrentHashMap<String, Session> onlineUsers = {
     "2021001": Session@xyz123,
     // "2021003": Session@a1b2c3d4,  ← 已移除
     "2021005": Session@def456,
     ...
   }
   ↓
3. 后端日志:
   [WebSocket] User disconnected: 2021003
   [WebSocket] Current online users: 2
  ↓
4. 清除本地存储:
   localStorage.clear();
   ↓
5. 跳转登录页:
   router.push('/login');
  ↓
WebSocket 连接已关闭 ✅
用户已退出系统
```

**涉及的类**:
- **后端**: `WebSocketServer.java` (Line 40-48)
- **前端**: `websocket.js` (Line 89-97)
- **组件**: `TopBar.vue`

**WebSocket 配置**:
```java
@Configuration
public class WebSocketConfig {
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

@ServerEndpoint("/ws/{userId}")  // WebSocket 端点路径
@Component
public class WebSocketServer {
    // ...
}
```

---

## 7. ✅ Nacos - 服务注册与监控

### 使用场景
服务注册、健康检查、服务监控

### 数据流详解

#### 场景1: 应用启动注册到 Nacos
```
开发者操作: 启动 Spring Boot 应用
  ↓
IDE: Run CrudBackendApplication
  ↓
===== Spring Boot 启动流程 =====
1. 加载配置文件:
   读取 application.yml:
   spring:
     application:
       name: suep-student-service
     cloud:
       nacos:
         server-addr: localhost:8848
         discovery:
           enabled: true
           metadata:
             version: 1.0.0
             env: dev
             author: bao
             description: 学生选课管理系统后端服务
             components: MySQL,Redis,Elasticsearch,RabbitMQ,MinIO,WebSocket
   ↓
2. Spring Cloud 组件初始化:
   @EnableDiscoveryClient 注解生效
   ↓
3. 连接 Nacos Server:
   NacosNamingService 初始化
   连接到: http://localhost:8848
   ↓
4. 注册服务实例:
   Instance instance = new Instance();
   instance.setServiceName("suep-student-service");
   instance.setIp(InetAddress.getLocalHost().getHostAddress());  // 例: "192.168.1.100"
   instance.setPort(8080);
   instance.setMetadata(metadata);

   HTTP 请求:
   POST http://localhost:8848/nacos/v1/ns/instance
   {
     "serviceName": "suep-student-service",
     "ip": "192.168.1.100",
     "port": 8080,
     "healthy": true,
     "weight": 1.0,
     "metadata": {
       "version": "1.0.0",
       "env": "dev",
       "author": "bao",
       "description": "学生选课管理系统后端服务",
       "components": "MySQL,Redis,Elasticsearch,RabbitMQ,MinIO,WebSocket"
     }
   }
   ↓
5. Nacos Server 处理:
   ├─ 1. 接收注册请求
   ├─ 2. 保存实例信息到内存
   ├─ 3. 通知订阅者（如果有）
   └─ 4. 返回注册成功
   ↓
6. 启动心跳任务 (每5秒):
   ScheduledExecutorService.scheduleWithFixedDelay(() -> {
     // 发送心跳
     PUT http://localhost:8848/nacos/v1/ns/instance/beat
     {
       "serviceName": "suep-student-service",
       "ip": "192.168.1.100",
       "port": 8080
     }
   }, 5, 5, TimeUnit.SECONDS);
   ↓
7. 后端日志:
   [Nacos] Nacos registry, suep-student-service 192.168.1.100:8080 register finished
   ↓
8. Spring Boot 启动完成:
   Started CrudBackendApplication in 8.234 seconds
  ↓
===== Nacos Dashboard 显示 =====
Nacos 控制台 (http://localhost:8848/nacos):
服务列表:
├── suep-student-service
│   ├── 实例数: 1
│   ├── 健康实例数: 1 ✅
│   ├── 分组: DEFAULT_GROUP
│   └── 实例详情:
│       ├── IP: 192.168.1.100
│       ├── 端口: 8080
│       ├── 权重: 1.0
│       ├── 健康状态: ✅ 健康
│       ├── 元数据:
│       │   ├── version: 1.0.0
│       │   ├── env: dev
│       │   ├── author: bao
│       │   ├── description: 学生选课管理系统后端服务
│       │   └── components: MySQL,Redis,Elasticsearch,RabbitMQ,MinIO,WebSocket
│       └── 最后心跳时间: 2025-12-23 16:30:05
```

**涉及的类**:
- **启动类**: `CrudBackendApplication.java` (Line 8: @EnableDiscoveryClient)
- **配置**: `application.yml` (Line 28-42: Nacos Discovery 配置)
- **依赖**: `spring-cloud-starter-alibaba-nacos-discovery`

#### 场景2: Nacos 健康检查
```
===== Nacos Server 定期检查 =====
Nacos Server (每5秒执行):
  ↓
1. 检查所有注册实例的心跳:
   for (Instance instance : instances) {
     long lastHeartbeat = instance.getLastHeartbeatTime();
     long now = System.currentTimeMillis();

     if (now - lastHeartbeat > 15000) {  // 超过15秒未心跳
       instance.setHealthy(false);  // 标记为不健康
     }
   }
   ↓
2. 如果实例不健康:
   Nacos Dashboard 显示:
   ├── 健康实例数: 0 ❌
   ├── 不健康实例数: 1
   └── 实例状态: 红色 "不健康"
   ↓
3. 通知所有订阅者:
   // 如果有其他服务订阅了 suep-student-service
   // Nacos 会推送服务变更通知
  ↓
===== 应用端健康检查端点 =====
Nacos 也可以主动调用应用的健康检查端点:
  ↓
HTTP 请求:
GET http://192.168.1.100:8080/actuator/health
  ↓
后端: Spring Boot Actuator 处理
  ↓
1. 检查所有组件健康状态:
   ├─ 数据库: SELECT 1
   ├─ Redis: PING
   └─ 其他组件...
   ↓
2. 返回健康状态:
   {
     "status": "UP",
     "components": {
       "db": {
         "status": "UP",
         "details": {
           "database": "MySQL",
           "validationQuery": "isValid()"
         }
       },
       "redis": {
         "status": "UP",
         "details": {
           "version": "7.x"
         }
       },
       "ping": {
         "status": "UP"
       }
     }
   }
  ↓
Nacos: 根据健康检查结果更新实例状态
```

**健康检查配置 (application.yml)**:
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health  # 暴露健康检查端点
  endpoint:
    health:
      show-details: always  # 显示详细健康信息
  health:
    db:
      enabled: true  # 监控数据库
    redis:
      enabled: true  # 监控 Redis
```

#### 场景3: 在 Nacos Dashboard 查看服务监控
```
管理员操作: 查看服务运行状态
  ↓
1. 打开浏览器访问:
   http://localhost:8848/nacos
   ↓
2. 登录:
   用户名: nacos
   密码: nacos
   ↓
3. 点击左侧菜单:
   服务管理 → 服务列表
   ↓
4. 查看服务列表:
   ┌─────────────────────────────────────────┐
   │ 服务名              | 分组         | 健康实例数/总实例数 │
   ├─────────────────────────────────────────┤
   │ suep-student-service | DEFAULT_GROUP | 1/1 ✅          │
   └─────────────────────────────────────────┘
   ↓
5. 点击"详情"按钮:
   ↓
6. 查看服务详细信息:
   ┌─────────────────────────────────────────┐
   │ 服务详情                                  │
   ├─────────────────────────────────────────┤
   │ 服务名: suep-student-service             │
   │ 分组: DEFAULT_GROUP                      │
   │ 保护阈值: 0.0                            │
   │ 健康实例数: 1                            │
   │ 触发保护阈值: 否                         │
   └─────────────────────────────────────────┘

   实例列表:
   ┌────────────────────────────────────────────────────────────┐
   │ IP            | 端口  | 健康状态 | 权重 | 元数据                   │
   ├────────────────────────────────────────────────────────────┤
   │ 192.168.1.100 | 8080  | ✅ 健康  | 1.0  | [查看元数据] [上下线]      │
   └────────────────────────────────────────────────────────────┘
   ↓
7. 点击"查看元数据":
   ┌─────────────────────────────────────────┐
   │ 元数据详情                                │
   ├─────────────────────────────────────────┤
   │ version: 1.0.0                           │
   │ env: dev                                 │
   │ author: bao                              │
   │ description: 学生选课管理系统后端服务      │
   │ components: MySQL,Redis,Elasticsearch,   │
   │             RabbitMQ,MinIO,WebSocket     │
   └─────────────────────────────────────────┘
  ↓
管理员可以实时看到:
✅ 服务是否在线
✅ 服务健康状态
✅ 服务版本和环境信息
✅ 服务使用的技术栈
✅ 最后心跳时间
```

**Nacos Dashboard 功能**:
1. **服务列表** - 查看所有注册的服务
2. **服务详情** - 查看服务实例、元数据
3. **服务上下线** - 手动上下线服务实例
4. **权重调整** - 调整负载均衡权重
5. **元数据管理** - 查看和编辑元数据

**Nacos 配置总结**:
```yaml
spring:
  application:
    name: suep-student-service  # 服务名
  cloud:
    nacos:
      server-addr: localhost:8848  # Nacos 地址
      discovery:
        enabled: true  # 启用服务注册
        namespace: public  # 命名空间
        group: DEFAULT_GROUP  # 分组
        metadata:  # 元数据（显示在 Nacos Dashboard）
          version: 1.0.0
          env: dev
          author: bao
          description: 学生选课管理系统后端服务
          components: MySQL,Redis,Elasticsearch,RabbitMQ,MinIO,WebSocket
```

---

## 8. ✅ 架构层次信息流

### Controller → Service → Mapper 标准流程

#### 示例: 课程资料上传完整流程

```
===== 1. Controller 层 (HTTP 接口) =====
前端请求:
POST /api/resource/upload
Content-Type: multipart/form-data
{
  file: File对象,
  courseId: 1
}
  ↓
CourseResourceController.uploadResource()
  ↓
职责:
├─ 接收 HTTP 请求
├─ 参数校验 (文件大小、类型)
├─ 调用 Service 层
└─ 返回统一响应格式
  ↓
代码:
@PostMapping("/upload")
public Map<String, Object> uploadResource(
    @RequestParam("file") MultipartFile file,
    @RequestParam("courseId") Long courseId
) {
    return courseResourceService.uploadResource(courseId, file);
}
  ↓
===== 2. Service 层 (业务逻辑) =====
CourseResourceServiceImpl.uploadResource()
  ↓
职责:
├─ 核心业务逻辑
├─ 事务管理
├─ 调用多个 Mapper
├─ 调用外部服务 (MinIO, Elasticsearch)
└─ 异常处理
  ↓
代码流程:
1. 生成唯一文件名
   String fileName = UUID.randomUUID() + "-" + file.getOriginalFilename();

2. 上传到 MinIO
   minioClient.putObject(...);

3. 保存到数据库 (Mapper层)
   CourseResource resource = new CourseResource();
   resource.setCourseId(courseId);
   resource.setResourceName(file.getOriginalFilename());
   resource.setResourceType(getFileExtension(file.getOriginalFilename()));
   resource.setResourceUrl(fileName);
   resource.setCreateTime(LocalDateTime.now());

   resourceMapper.insert(resource);  ← 调用 Mapper

4. 同步到 Elasticsearch
   syncToElasticsearch(resource);

5. 返回结果
   Map<String, Object> result = new HashMap<>();
   result.put("success", true);
   result.put("resourceId", resource.getId());
   return result;
  ↓
===== 3. Mapper 层 (数据访问) =====
CourseResourceMapper.insert()
  ↓
职责:
├─ SQL 执行
├─ ORM 映射
├─ 返回数据
└─ 无业务逻辑
  ↓
MyBatis-Plus 自动生成 SQL:
INSERT INTO t_course_resource
(course_id, resource_name, resource_type, resource_url, create_time)
VALUES (1, 'Java面向对象编程.pdf', 'pdf', 'a1b2c3d4-...pdf', '2025-12-23 16:30:00')
  ↓
返回自增ID: 15
  ↓
===== 4. 层次职责总结 =====

Controller 层:
✅ HTTP 请求/响应处理
✅ 参数接收和基本校验
✅ 调用 Service
✅ 返回统一格式
❌ 不包含业务逻辑
❌ 不直接操作数据库

Service 层:
✅ 核心业务逻辑
✅ 事务管理
✅ 调用多个 Mapper
✅ 调用外部服务
✅ 异常处理
❌ 不处理 HTTP 细节

Mapper 层:
✅ SQL 执行
✅ 数据库 CRUD
✅ ORM 映射
❌ 无业务逻辑
❌ 不调用外部服务

外部服务:
├─ MinIO (对象存储)
├─ Elasticsearch (搜索引擎)
├─ Redis (缓存)
├─ RabbitMQ (消息队列)
└─ WebSocket (实时通信)
```

### 层次依赖关系图

```
┌────────────────────────────────────────┐
│           前端 (Vue 3)                  │
│  StudentMaterials.vue, CourseMaterial.vue│
└────────────────────────────────────────┘
                ↓ HTTP/WebSocket
┌────────────────────────────────────────┐
│         Controller 层                   │
│  CourseResourceController.java          │
│  CourseController.java                  │
│  TimetableController.java               │
└────────────────────────────────────────┘
                ↓ 方法调用
┌────────────────────────────────────────┐
│          Service 层                     │
│  ICourseResourceService                 │
│  CourseResourceServiceImpl              │
│  ICourseService                         │
│  CourseServiceImpl                      │
│  SmsService                             │
└────────────────────────────────────────┘
                ↓ 方法调用
┌────────────────────────────────────────┐
│          Mapper 层                      │
│  CourseResourceMapper                   │
│  CourseMapper                           │
│  StudentCourseMapper                    │
│  NotificationMapper                     │
└────────────────────────────────────────┘
                ↓ SQL执行
┌────────────────────────────────────────┐
│          数据库 (MySQL)                 │
│  t_course_resource                      │
│  t_course                               │
│  t_student_course                       │
│  t_notification                         │
└────────────────────────────────────────┘

同时, Service 层调用外部服务:
┌────────────────────────────────────────┐
│         外部服务                        │
│  ├─ MinIO (文件存储)                   │
│  ├─ Elasticsearch (搜索)               │
│  ├─ Redis (缓存)                       │
│  ├─ RabbitMQ (消息队列)                │
│  └─ WebSocket (实时通信)               │
└────────────────────────────────────────┘
```

---

## 9. ✅ 完整业务流程示例

### 学生选课完整信息流

```
┌─────────────────────────────────────────────────────────┐
│ 第1步: 前端发起请求                                       │
└─────────────────────────────────────────────────────────┘
前端: StudentCourse.vue handleSelectCourse()
POST /api/timetable/select
{
  "studentId": "2021003",
  "scheduleId": 5
}
  ↓
┌─────────────────────────────────────────────────────────┐
│ 第2步: Controller 层接收请求                              │
└─────────────────────────────────────────────────────────┘
TimetableController.selectCourse()
- 接收参数
- 调用业务逻辑
  ↓
┌─────────────────────────────────────────────────────────┐
│ 第3步: MySQL - 业务验证                                   │
└─────────────────────────────────────────────────────────┘
studentCourseMapper.selectList()
- 检查是否重复选课
- 检查课程容量
- 检查时间冲突
  ↓
┌─────────────────────────────────────────────────────────┐
│ 第4步: MySQL - 数据写入 (事务)                            │
└─────────────────────────────────────────────────────────┘
@Transactional
- INSERT INTO t_student_course
- UPDATE t_course_schedule SET current_count = current_count + 1
- INSERT INTO t_notification
  ↓
┌─────────────────────────────────────────────────────────┐
│ 第5步: WebSocket - 实时通知                              │
└─────────────────────────────────────────────────────────┘
WebSocketServer.sendInfo("2021003", "选课成功...")
- 从内存 Map 查找用户会话
- 发送 WebSocket 消息
- 前端页面实时显示通知 (小铃铛 +1)
  ↓
┌─────────────────────────────────────────────────────────┐
│ 第6步: RabbitMQ - 短信通知 (异步)                        │
└─────────────────────────────────────────────────────────┘
SmsService.sendCourseSelectionSms()
- 消息发送到队列 (5ms)
- 主线程立即返回
... 3秒后 ...
SmsConsumer.process()
- 调用阿里云短信 API (2000ms)
- 用户手机收到短信
  ↓
┌─────────────────────────────────────────────────────────┐
│ 第7步: 返回响应                                          │
└─────────────────────────────────────────────────────────┘
{
  "code": 200,
  "msg": "选课成功"
}
  ↓
前端: 显示成功提示，刷新课程列表
```

### 时间线分析

| 步骤 | 操作 | 耗时 | 备注 |
|------|------|------|------|
| 1 | 前端发起请求 | 10ms | 网络传输 |
| 2 | Controller 接收 | 1ms | 参数解析 |
| 3 | MySQL 验证查询 | 30ms | 3次 SELECT |
| 4 | MySQL 数据写入 | 50ms | 2次 INSERT + 1次 UPDATE |
| 5 | WebSocket 推送 | 5ms | 内存查找 + 网络发送 |
| 6 | RabbitMQ 发送 | 5ms | 消息入队 |
| 7 | 返回响应 | 10ms | 网络传输 |
| **用户感知总耗时** | **111ms** | 用户几乎无感知 |
| 8 | 短信异步发送 | 2000ms | 后台独立处理 |

**关键优化**:
- ✅ WebSocket 实时推送（毫秒级）
- ✅ RabbitMQ 异步短信（不阻塞主流程）
- ✅ 事务保证数据一致性
- ✅ 多层缓存和索引优化查询

---

## 📝 总结

### 各服务职责一览表

| 服务 | 职责 | 数据存储 | 访问方式 |
|------|------|----------|----------|
| **MySQL** | 核心业务数据 | 磁盘持久化 | MyBatis-Plus Mapper |
| **Redis** | 验证码、缓存 | 内存 + 持久化 | RedisTemplate |
| **Elasticsearch** | 全文搜索 | 磁盘 + 内存 | ElasticsearchOperations |
| **MinIO** | 文件存储 | 磁盘 | MinioClient |
| **RabbitMQ** | 异步消息 | 磁盘 + 内存 | RabbitTemplate + @RabbitListener |
| **WebSocket** | 实时通知 | 内存 (Session Map) | Session.sendText() |
| **Nacos** | 服务注册监控 | 内存 + 磁盘 | DiscoveryClient |

### 数据流向图

```
                    ┌─────────────┐
                    │  前端 Vue 3  │
                    └──────┬──────┘
                           │ HTTP/WebSocket
              ┌────────────┼────────────┐
              │            │            │
        ┌─────▼─────┐ ┌───▼────┐ ┌────▼────┐
        │ Controller│ │WebSocket│ │  Redis  │
        └─────┬─────┘ └────────┘ └─────────┘
              │                   验证码存取
        ┌─────▼─────┐
        │  Service  │
        └─────┬─────┘
              │
      ┌───────┼───────┬────────┬────────┐
      │       │       │        │        │
  ┌───▼──┐┌──▼───┐┌──▼──┐┌────▼───┐┌──▼──┐
  │Mapper││MinIO ││ElasticSearch││RabbitMQ│
  └───┬──┘└──────┘└─────┘└────────┘└──────┘
      │
  ┌───▼───┐
  │ MySQL │
  └───────┘
```

### 技术亮点汇总

1. **分层架构**: Controller → Service → Mapper 清晰分离
2. **异步处理**: RabbitMQ 消息队列，提升响应速度
3. **实时通信**: WebSocket 双向通信，毫秒级推送
4. **智能搜索**: Elasticsearch + IK 分词器，中文全文搜索
5. **对象存储**: MinIO 分布式文件存储
6. **服务监控**: Nacos 服务注册与健康检查
7. **事务管理**: @Transactional 保证数据一致性
8. **缓存优化**: Redis 缓存热点数据

---

📅 **文档版本**: v1.0
👨‍💻 **作者**: bao
🏗️ **最后更新**: 2025-12-23
