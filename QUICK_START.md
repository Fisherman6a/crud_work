# 🚀 快速启动指南

## 前提条件

确保以下服务已安装并运行：

- ✅ JDK 21
- ✅ Node.js 16+
- ✅ MySQL 8.0
- ✅ Redis
- ✅ MinIO
- ✅ Elasticsearch 7.x
- ✅ RabbitMQ（可选）
- ✅ Nacos（可选）

---

## 第一步：数据库初始化

```bash
# 1. 登录MySQL
mysql -u root -p

# 2. 创建数据库（如果还没创建）
CREATE DATABASE student DEFAULT CHARACTER SET utf8mb4;

# 3. 执行SQL脚本
USE student;
SOURCE d:\Code\Development\crud_work\crud_backend\sql\table.sql;

# 4. 验证表是否创建成功
SHOW TABLES;

# 应该看到以下表：
# t_user, t_student, t_teacher, t_course, t_course_teacher,
# t_course_schedule, t_student_course, t_course_resource, t_notification
```

---

## 第二步：MinIO配置

```bash
# 1. 启动MinIO
# Windows:
minio.exe server D:\minio-data --console-address ":9001"

# Linux/Mac:
minio server /data/minio --console-address ":9001"

# 2. 访问Web控制台
http://localhost:9001

# 3. 登录
用户名: minioadmin
密码: minioadmin

# 4. 创建存储桶
名称: course-files
访问策略: Public（或使用预签名URL）
```

---

## 第三步：Elasticsearch配置

```bash
# 1. 启动Elasticsearch
# Windows:
elasticsearch-7.x.x\bin\elasticsearch.bat

# Linux/Mac:
elasticsearch-7.x.x/bin/elasticsearch

# 2. 验证服务
curl http://localhost:9200

# 应该返回版本信息

# 3. 安装IK分词器（中文分词，可选）
elasticsearch-plugin install https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.x.x/elasticsearch-analysis-ik-7.x.x.zip

# 4. 重启Elasticsearch
```

---

## 第四步：启动后端服务

```bash
# 1. 进入后端目录
cd d:\Code\Development\crud_work\crud_backend

# 2. 修改配置文件（如果需要）
# 编辑 src/main/resources/application.yml
# 确认数据库连接、MinIO、ES、Redis等配置

# 3. 使用Maven启动
mvn spring-boot:run

# 或使用IDE（推荐）
# 在IDEA中打开项目，运行CrudBackendApplication.java

# 4. 验证服务启动
curl http://localhost:8080/test/hello

# 应该返回: "Hello from Spring Boot!"
```

---

## 第五步：启动前端服务

```bash
# 1. 进入前端目录
cd d:\Code\Development\crud_work\crud_frontend

# 2. 安装依赖（首次运行）
npm install
# 或使用 yarn
yarn install

# 3. 启动开发服务器
npm run dev
# 或
yarn dev

# 4. 浏览器访问
http://localhost:5173
# 端口可能不同，查看终端输出
```

---

## 第六步：登录测试

### 管理员账号
```
用户名: admin
密码: 123456
```

**可访问页面：**
- ✅ 学生管理 (`/app/student`)
- ✅ 教师管理 (`/app/teacher-manager`)
- ✅ 课程管理 (`/app/selection-manage`)
- ✅ 课程资料 (`/app/course-manager`)
- ✅ 排课管理 (`/app/timetable`)

### 学生账号
```
用户名: user
密码: 123456
```

**可访问页面：**
- ✅ 学生选课 (`/app/student-course`)
- ✅ 我的课表 (`/app/my-timetable`)
- ✅ 课程资料（只读）(`/app/course-manager`)

---

## 第七步：功能测试

### 1. 测试通知系统

**方法一：使用Postman**
```http
POST http://localhost:8080/notification/send
Content-Type: application/json

{
  "userId": "admin",
  "type": "SYSTEM_NOTICE",
  "title": "系统通知",
  "content": "欢迎使用课程管理系统！",
  "courseId": null
}
```

**方法二：使用curl**
```bash
curl -X POST http://localhost:8080/notification/send \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "admin",
    "type": "SYSTEM_NOTICE",
    "title": "系统通知",
    "content": "欢迎使用课程管理系统！"
  }'
```

**预期结果：**
- 右上角铃铛显示红色徽章
- 点击铃铛看到新通知
- 浏览器弹出桌面通知（需授权）

### 2. 测试文件上传

1. 以管理员身份登录
2. 访问"课程资料"页面
3. 左侧点击任意课程
4. 右侧拖拽文件到上传区
5. 等待上传完成
6. 查看资源列表
7. 点击"预览"测试各种格式

**测试文件：**
- ✅ PDF文档
- ✅ Word文档 (.docx)
- ✅ PPT演示 (.pptx)
- ✅ MP4视频
- ✅ MP3音频

### 3. 测试学生选课

1. 以学生身份登录
2. 访问"学生选课"页面
3. 浏览课程卡片
4. 使用搜索和筛选功能
5. 点击"查看详情"
6. 点击"选课"按钮
7. 确认选课成功
8. 点击"我的课表"查看

**检查点：**
- ✅ 卡片样式美观
- ✅ 搜索和筛选有效
- ✅ 进度条颜色正确
- ✅ 已选课程高亮显示
- ✅ 容量已满时禁用选课

---

## 常见问题排查

### 问题1：后端启动失败

**错误：连接MySQL失败**
```
Solution:
1. 检查MySQL是否运行: netstat -an | findstr 3306
2. 验证用户名密码是否正确
3. 确认数据库student已创建
```

**错误：连接Redis失败**
```
Solution:
1. 启动Redis: redis-server
2. 测试连接: redis-cli ping (应返回PONG)
```

### 问题2：前端无法连接后端

**错误：Network Error**
```
Solution:
1. 检查后端是否启动: curl http://localhost:8080/test/hello
2. 检查跨域配置: 所有Controller应有@CrossOrigin注解
3. 检查浏览器控制台错误信息
```

### 问题3：WebSocket连接失败

**错误：WebSocket connection failed**
```
Solution:
1. 检查后端WebSocket端点: ws://localhost:8080/ws/admin
2. 查看浏览器控制台Network标签
3. 确认防火墙未阻止WebSocket连接
4. 尝试使用不同浏览器（推荐Chrome）
```

### 问题4：文件上传失败

**错误：上传失败**
```
Solution:
1. 检查MinIO是否运行: curl http://localhost:9000/minio/health/live
2. 验证存储桶是否创建: 访问 http://localhost:9001
3. 检查后端MinIO配置（application.yml）
4. 查看后端日志错误信息
5. 检查文件大小限制（默认10MB）
```

### 问题5：全文搜索无结果

**错误：搜索总是返回空**
```
Solution:
1. 检查Elasticsearch是否运行: curl http://localhost:9200
2. 验证索引是否创建: curl http://localhost:9200/_cat/indices
3. 检查后端ES配置（application.yml）
4. 确认上传文件后已触发索引
5. 查看后端日志是否有Tika错误
```

---

## 开发工具推荐

### 后端开发
- **IDE**: IntelliJ IDEA 2023+
- **数据库客户端**: DataGrip / Navicat
- **API测试**: Postman / Apifox
- **Redis客户端**: RedisInsight

### 前端开发
- **IDE**: Visual Studio Code
- **插件**:
  - Volar (Vue 3)
  - ESLint
  - Prettier
- **浏览器**: Chrome + Vue DevTools

---

## 性能优化建议

### 开发环境
```yaml
# application-dev.yml
spring:
  devtools:
    restart:
      enabled: true  # 热重载
  jpa:
    show-sql: true   # 显示SQL（调试用）

logging:
  level:
    com.example.crud_backend: DEBUG
```

### 生产环境
```yaml
# application-prod.yml
spring:
  devtools:
    restart:
      enabled: false
  jpa:
    show-sql: false

logging:
  level:
    com.example.crud_backend: INFO
```

---

## 下一步建议

1. **添加测试数据**
   - 创建几门课程
   - 添加教师信息
   - 设置排课时间
   - 上传课程资料

2. **测试完整流程**
   - 管理员创建课程
   - 学生浏览和选课
   - 查看课程资料
   - 接收通知消息

3. **性能测试**
   - 并发选课测试
   - 大文件上传测试
   - WebSocket稳定性测试
   - 全文搜索性能测试

4. **安全加固**
   - 添加JWT认证
   - API接口权限控制
   - 文件上传安全检查
   - SQL注入防护

---

## 技术支持

如有问题，请查看：
1. 📄 [实施总结文档](IMPLEMENTATION_SUMMARY.md)
2. 📄 [功能实现计划](FEATURE_IMPLEMENTATION_PLAN.md)
3. 🐛 [GitHub Issues](https://github.com/your-repo/issues)
4. 💬 项目文档和代码注释

---

**祝您使用愉快！🎉**
