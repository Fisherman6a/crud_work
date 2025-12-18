# 课程-教师关联功能实现总结

## 一、数据库变更

### 新增表：t_course_teacher（课程-教师关联表）

```sql
CREATE TABLE t_course_teacher (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    UNIQUE KEY uk_course_teacher (course_id, teacher_id),
    KEY idx_course_id (course_id),
    KEY idx_teacher_id (teacher_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='课程-教师关联表';
```

## 二、后端修改

### 1. 新增实体类

- **CourseTeacher.java** - 课程教师关联实体
  - 路径：`crud_backend/src/main/java/com/example/crud_backend/entity/CourseTeacher.java`

- **CourseWithTeachers.java** - 带教师信息的课程DTO
  - 路径：`crud_backend/src/main/java/com/example/crud_backend/dto/CourseWithTeachers.java`

### 2. 新增Mapper

- **CourseTeacherMapper.java** - 课程教师关联Mapper
  - 路径：`crud_backend/src/main/java/com/example/crud_backend/mapper/CourseTeacherMapper.java`

### 3. 修改 CourseController.java

**新增/修改的接口**：

1. **GET /course/page** - 分页查询课程（带教师信息）
   - 返回类型：`Page<CourseWithTeachers>`
   - 自动关联查询每个课程的任课老师

2. **POST /course/save** - 保存课程（带教师关联）
   - 请求体：包含 `teacherIds` 数组
   - 支持同时保存课程和教师关联
   - 自动验证教师是否存在

3. **DELETE /course/{id}** - 删除课程
   - 自动删除关联的教师关系

4. **POST /course/{courseId}/teacher/{teacherId}** - 为课程添加教师
   - 验证课程和教师存在性
   - 防止重复添加

5. **DELETE /course/{courseId}/teacher/{teacherId}** - 删除课程的某个教师

## 三、前端修改

### 1. SelectionManage.vue（课程管理页面）

**主要改动**：

1. **表格新增"任课老师"列**
   - 显示该课程的所有任课老师（Tag形式）
   - 每个课程后有"管理"按钮，可以添加/删除教师

2. **课程编辑弹窗增强**
   - 新增"任课老师"字段（多选下拉框）
   - 支持选择多个教师
   - 教师为可选项（可以不选）

3. **新增教师管理弹窗**
   - 显示当前课程的所有任课老师
   - 可以动态添加/删除教师
   - 实时刷新数据

### 2. TeacherManager.vue（教师管理页面）

**主要改动**：

1. **编辑弹窗显示教师ID**
   - 编辑模式下显示ID（禁用状态）
   - 新增模式下ID自动生成

## 四、功能特性

### ✅ 已实现的功能

1. **课程-教师多对多关联**
   - 一个课程可以有多个任课老师
   - 一个老师可以教授多门课程

2. **数据验证**
   - 添加教师时验证教师是否存在
   - 防止重复添加同一教师
   - 课程ID查重（编辑时）

3. **级联删除**
   - 删除课程时自动删除关联关系
   - 保护数据完整性

4. **灵活的教师管理**
   - 列表中直接管理任课老师
   - 编辑课程时批量选择教师
   - 支持动态添加/删除

5. **友好的UI交互**
   - 任课老师以Tag形式展示
   - 支持多选和搜索教师
   - 实时反馈操作结果

## 五、使用说明

### 添加课程时关联教师

1. 点击"新增课程"按钮
2. 填写课程信息（名称、描述、学分）
3. 在"任课老师"下拉框中选择一个或多个教师（可选）
4. 点击"保存"

### 管理已有课程的教师

**方法1：通过列表的"管理"按钮**

1. 在课程列表的"任课老师"列找到"管理"按钮
2. 点击后弹出教师管理窗口
3. 可以添加新教师或删除现有教师

**方法2：通过编辑课程**

1. 点击"编辑"按钮
2. 在"任课老师"下拉框中修改选择
3. 点击"保存"

## 六、API接口文档

### 1. 获取课程列表（带教师信息）

```http
GET /course/page?pageNum=1&pageSize=10&search=数学
```

**响应示例**：
```json
{
  "records": [
    {
      "id": 1,
      "name": "高等数学",
      "description": "微积分基础",
      "credit": 4,
      "teachers": [
        {"id": 1, "name": "张三", "title": "教授"},
        {"id": 2, "name": "李四", "title": "副教授"}
      ]
    }
  ],
  "total": 100
}
```

### 2. 保存课程（带教师关联）

```http
POST /course/save
Content-Type: application/json

{
  "id": null,
  "name": "高等数学",
  "description": "微积分基础",
  "credit": 4,
  "teacherIds": [1, 2, 3]
}
```

**响应示例**：
```json
{
  "success": true,
  "courseId": 1
}
```

### 3. 为课程添加教师

```http
POST /course/1/teacher/2
```

**响应示例**：
```json
{
  "success": true
}
```

### 4. 删除课程的教师

```http
DELETE /course/1/teacher/2
```

**响应示例**：
```json
{
  "success": true
}
```

## 七、注意事项

1. **数据库迁移**：需要先执行SQL创建 `t_course_teacher` 表
2. **教师列表加载**：前端会自动从 `/teacher/list` 加载所有教师
3. **事务处理**：课程保存使用了 `@Transactional` 保证数据一致性
4. **性能优化**：如果教师数量很多，建议在前端添加分页加载

## 八、测试步骤

1. **创建测试数据**
   - 先添加几个教师
   - 再添加课程并关联教师

2. **测试添加功能**
   - 新增课程时选择多个教师
   - 验证保存后教师关联正确

3. **测试管理功能**
   - 使用"管理"按钮添加/删除教师
   - 验证实时更新

4. **测试删除功能**
   - 删除课程
   - 验证关联关系也被删除

## 九、文件清单

### 后端新增文件
- `crud_backend/src/main/java/com/example/crud_backend/entity/CourseTeacher.java`
- `crud_backend/src/main/java/com/example/crud_backend/dto/CourseWithTeachers.java`
- `crud_backend/src/main/java/com/example/crud_backend/mapper/CourseTeacherMapper.java`

### 后端修改文件
- `crud_backend/src/main/java/com/example/crud_backend/controller/CourseController.java`

### 前端修改文件
- `crud_frontend/src/views/SelectionManage.vue`
- `crud_frontend/src/views/TeacherManager.vue`

全部功能已完成！✅
