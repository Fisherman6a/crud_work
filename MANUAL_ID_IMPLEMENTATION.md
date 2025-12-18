# 手动ID输入功能实现总结

## 问题修复

### 1. Integer to Long 类型转换错误修复

**问题**: `class java.lang.Integer cannot be cast to class java.lang.Long`

**原因**: 前端发送的 teacherIds 数组被后端解析为 `List<Integer>`，但代码尝试直接转换为 `List<Long>`

**修复位置**: [CourseController.java:213-217](crud_backend/src/main/java/com/example/crud_backend/controller/CourseController.java#L213-L217)

**修复方案**:
```java
// 原代码（错误）
List<Long> teacherIds = (List<Long>) request.get("teacherIds");

// 修复后
List<?> rawTeacherIds = (List<?>) request.get("teacherIds");
List<Long> teacherIds = rawTeacherIds.stream()
        .map(id -> Long.valueOf(id.toString()))
        .collect(Collectors.toList());
```

---

## 手动ID输入功能实现

### 2. 后端修改

#### 2.1 Course 实体类修改

**文件**: [Course.java:11](crud_backend/src/main/java/com/example/crud_backend/entity/Course.java#L11)

**修改**:
```java
// 从 AUTO 改为 INPUT
@TableId(type = IdType.INPUT)
private Long id;
```

#### 2.2 Teacher 实体类修改

**文件**: [Teacher.java:11](crud_backend/src/main/java/com/example/crud_backend/entity/Teacher.java#L11)

**修改**:
```java
// 从 AUTO 改为 INPUT
@TableId(type = IdType.INPUT)
private Long id;
```

#### 2.3 CourseController ID验证逻辑

**文件**: [CourseController.java:195-211](crud_backend/src/main/java/com/example/crud_backend/controller/CourseController.java#L195-L211)

**修改内容**:
- 添加ID必填验证
- ID为空时返回错误提示
- 支持手动输入ID进行新增或更新

```java
// ID duplicate checking: if courseId is provided, check if it already exists
if (courseId != null) {
    Course existing = courseService.getById(courseId);
    // If exists, this is an update; if not, this is a new course
} else {
    // No ID provided, reject (manual ID is required now)
    result.put("success", false);
    result.put("message", "课程ID不能为空");
    return result;
}
```

#### 2.4 TeacherController ID验证逻辑

**文件**: [TeacherController.java:41-64](crud_backend/src/main/java/com/example/crud_backend/controller/TeacherController.java#L41-L64)

**修改内容**:
- 添加ID必填验证
- 根据ID是否存在判断新增还是更新
- 返回统一的错误格式（Map对象）

```java
@PostMapping("/save")
public Object save(@RequestBody Teacher teacher) {
    // ID is now required for manual input
    if (teacher.getId() == null) {
        return createErrorResponse("教师ID不能为空");
    }

    // Check if ID already exists
    Teacher existing = teacherMapper.selectById(teacher.getId());

    if (existing == null) {
        // New teacher
        return teacherMapper.insert(teacher) > 0;
    } else {
        // Update existing teacher
        return teacherMapper.updateById(teacher) > 0;
    }
}
```

---

### 3. 前端修改

#### 3.1 SelectionManage.vue（课程管理）

**修改位置**:

1. **表单字段修改** ([SelectionManage.vue:29-32](crud_frontend/src/views/SelectionManage.vue#L29-L32)):
   ```vue
   <n-form-item label="课程ID" path="id">
       <n-input-number v-model:value="form.id" :disabled="!!originalId"
                       placeholder="请输入课程ID"
                       style="width: 100%" :show-button="false" />
   </n-form-item>
   ```
   - **变更**: ID字段始终显示（无论新增还是编辑）
   - **行为**: 新增时可编辑，编辑时禁用（通过 `originalId` 判断）

2. **添加 originalId 变量** ([SelectionManage.vue:98](crud_frontend/src/views/SelectionManage.vue#L98)):
   ```javascript
   const originalId = ref(null) // Track original ID for edit mode
   ```

3. **修改 openModal 函数** ([SelectionManage.vue:181-200](crud_frontend/src/views/SelectionManage.vue#L181-L200)):
   ```javascript
   const openModal = (row) => {
       if (row) {
           // Edit mode
           form.id = row.id
           // ... other fields
           originalId.value = row.id // Store original ID
       } else {
           // Add mode
           form.id = null
           // ... other fields
           originalId.value = null
       }
       showModal.value = true
   }
   ```

4. **添加ID验证** ([SelectionManage.vue:257-262](crud_frontend/src/views/SelectionManage.vue#L257-L262)):
   ```javascript
   const handleSubmit = async () => {
       // Validate ID
       if (!form.id) {
           message.error('请输入课程ID')
           return
       }
       // ... submit logic
   }
   ```

#### 3.2 TeacherManager.vue（教师管理）

**修改位置**:

1. **表单字段修改** ([TeacherManager.vue:29-32](crud_frontend/src/views/TeacherManager.vue#L29-L32)):
   ```vue
   <n-form-item label="教师ID" path="id">
       <n-input-number v-model:value="form.id" :disabled="!!originalId"
                       placeholder="请输入教师ID"
                       style="width: 100%" :show-button="false" />
   </n-form-item>
   ```

2. **添加 originalId 变量** ([TeacherManager.vue:67](crud_frontend/src/views/TeacherManager.vue#L67)):
   ```javascript
   const originalId = ref(null) // Track original ID for edit mode
   ```

3. **修改 openModal 函数** ([TeacherManager.vue:124-133](crud_frontend/src/views/TeacherManager.vue#L124-L133)):
   ```javascript
   const openModal = (row) => {
       if (row) {
           Object.assign(form, row)
           originalId.value = row.id // Store original ID for edit mode
       } else {
           Object.assign(form, { id: null, name: '', title: '讲师', phone: '' })
           originalId.value = null
       }
       showModal.value = true
   }
   ```

4. **添加ID验证和响应处理** ([TeacherManager.vue:135-155](crud_frontend/src/views/TeacherManager.vue#L135-L155)):
   ```javascript
   const handleSubmit = async () => {
       // Validate ID
       if (!form.id) {
           message.error('请输入教师ID')
           return
       }

       try {
           // TeacherController /save 接口现在返回 boolean 或 Map
           const res = await axios.post(`${API_URL}/save`, form)
           if (res.data === true || res.data.success === true) {
               message.success('操作成功')
               showModal.value = false
               loadData()
           } else if (res.data.success === false) {
               message.error(res.data.message || '操作失败')
           } else {
               message.error('操作失败')
           }
       } catch (e) { message.error('请求错误') }
   }
   ```

---

### 4. 数据库修改

**文件**: [table.sql:35-49](crud_backend/sql/table.sql#L35-L49)

**修改内容**:

```sql
DROP TABLE IF EXISTS t_teacher;
-- 2. 教师表 (手动输入ID，不使用AUTO_INCREMENT)
CREATE TABLE t_teacher (
    id BIGINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    title VARCHAR(50) COMMENT '职称',
    phone VARCHAR(20)
);

-- 3. 课程基本信息表 (手动输入ID，不使用AUTO_INCREMENT)
CREATE TABLE t_course (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '课程名称',
    description TEXT,
    credit INT COMMENT '学分'
);
```

**重要**: 需要重新执行SQL以应用数据库结构变更！

---

## 使用说明

### 新增课程

1. 点击"新增课程"按钮
2. **手动输入课程ID**（必填）
3. 填写课程名称、描述、学分
4. 选择任课老师（可选）
5. 点击"保存"

### 编辑课程

1. 点击"编辑"按钮
2. **课程ID字段为禁用状态**（不可修改）
3. 修改其他信息
4. 点击"保存"

### 新增教师

1. 点击"新增教师"按钮
2. **手动输入教师ID**（必填）
3. 填写姓名、职称、电话
4. 点击"确定"

### 编辑教师

1. 点击"编辑"按钮
2. **教师ID字段为禁用状态**（不可修改）
3. 修改其他信息
4. 点击"确定"

---

## 验证逻辑

### 前端验证

1. **ID必填检查**: 提交前检查 `form.id` 是否为空
2. **编辑模式ID禁用**: 通过 `:disabled="!!originalId"` 防止修改已有记录的ID

### 后端验证

1. **Course**:
   - ID为空返回错误: "课程ID不能为空"
   - 根据ID是否存在数据库判断新增还是更新

2. **Teacher**:
   - ID为空返回错误: "教师ID不能为空"
   - 根据ID是否存在数据库判断新增还是更新

---

## 已完成的修改清单

### 后端文件

- ✅ [Course.java](crud_backend/src/main/java/com/example/crud_backend/entity/Course.java) - IdType.AUTO → IdType.INPUT
- ✅ [Teacher.java](crud_backend/src/main/java/com/example/crud_backend/entity/Teacher.java) - IdType.AUTO → IdType.INPUT
- ✅ [CourseController.java](crud_backend/src/main/java/com/example/crud_backend/controller/CourseController.java) - 修复类型转换错误 + ID验证
- ✅ [TeacherController.java](crud_backend/src/main/java/com/example/crud_backend/controller/TeacherController.java) - ID验证逻辑

### 前端文件

- ✅ [SelectionManage.vue](crud_frontend/src/views/SelectionManage.vue) - 手动ID输入 + 验证
- ✅ [TeacherManager.vue](crud_frontend/src/views/TeacherManager.vue) - 手动ID输入 + 验证

### 数据库文件

- ✅ [table.sql](crud_backend/sql/table.sql) - 移除 AUTO_INCREMENT

---

## 注意事项

1. **数据库迁移**:
   - 如果数据库中已有数据，需要先备份
   - 删除现有的 `t_course` 和 `t_teacher` 表
   - 重新执行 `table.sql` 创建新表结构

2. **ID分配策略**:
   - 课程ID和教师ID需要手动输入
   - 建议使用有意义的ID编号规则（如课程ID从1001开始，教师ID从2001开始）

3. **重复ID检测**:
   - 后端会自动检测ID是否已存在
   - 新增时如果ID已存在，会执行更新操作
   - 编辑时ID字段禁用，无法修改

4. **级联影响**:
   - `t_course_teacher` 表的外键依然有效
   - 删除课程或教师时，关联关系会自动删除（ON DELETE CASCADE）

---

## 测试步骤

1. **重建数据库表**:
   ```sql
   SOURCE d:/Code/Development/crud_work/crud_backend/sql/table.sql;
   ```

2. **测试新增课程**:
   - 输入课程ID: 1001
   - 验证能否成功保存
   - 验证教师关联是否正常

3. **测试新增教师**:
   - 输入教师ID: 2001
   - 验证能否成功保存

4. **测试编辑功能**:
   - 编辑已有课程，验证ID字段是否禁用
   - 编辑已有教师，验证ID字段是否禁用

5. **测试ID重复**:
   - 尝试新增一个已存在的ID
   - 验证是否执行更新操作

---

全部功能已完成！✅
