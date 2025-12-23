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
学生选课、退课、成绩发布等事件的实时推送通知

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
