-- 诊断SQL：检查学生2021003的数据

-- 1. 检查学生是否存在
SELECT '=== 学生信息 ===' as info;
SELECT * FROM t_student WHERE student_number = '2021003';

-- 2. 检查该学生的选课记录（原始数据）
SELECT '=== 选课记录（t_student_course表） ===' as info;
SELECT * FROM t_student_course WHERE student_id = '2021003';

-- 3. 检查该学生的完整课表（JOIN查询 - 与后端查询相同）
SELECT '=== 完整课表（JOIN查询） ===' as info;
SELECT
    sc.id as selection_id,
    sc.student_id,
    sc.schedule_id,
    s.id as schedule_id_check,
    s.course_id,
    s.week_day,
    s.section_start,
    s.section_end,
    s.location,
    s.current_count,
    s.max_capacity,
    c.id as course_id_check,
    c.name as course_name,
    c.credit,
    c.description,
    t.id as teacher_id,
    t.name as teacher_name,
    t.title as teacher_title
FROM t_student_course sc
JOIN t_course_schedule s ON sc.schedule_id = s.id
JOIN t_course c ON s.course_id = c.id
JOIN t_teacher t ON s.teacher_id = t.id
WHERE sc.student_id = '2021003'
ORDER BY s.week_day, s.section_start;

-- 4. 检查所有排课信息
SELECT '=== 所有排课信息 ===' as info;
SELECT
    s.id as schedule_id,
    s.course_id,
    s.teacher_id,
    s.week_day,
    s.section_start,
    s.section_end,
    s.location,
    s.current_count,
    s.max_capacity,
    c.name as course_name,
    t.name as teacher_name
FROM t_course_schedule s
JOIN t_course c ON s.course_id = c.id
JOIN t_teacher t ON s.teacher_id = t.id
ORDER BY s.week_day, s.section_start;

-- 5. 检查所有学生及其选课数量
SELECT '=== 所有学生选课统计 ===' as info;
SELECT
    stu.student_number,
    stu.name as student_name,
    COUNT(sc.id) as course_count
FROM t_student stu
LEFT JOIN t_student_course sc ON stu.student_number = sc.student_id
GROUP BY stu.student_number, stu.name
ORDER BY stu.student_number;

-- 6. 检查字符编码问题
SELECT '=== 字符编码检查 ===' as info;
SELECT
    CHARSET(name) as name_charset,
    COLLATION(name) as name_collation,
    HEX(name) as name_hex,
    name
FROM t_teacher
LIMIT 5;
