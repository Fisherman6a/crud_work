package com.example.crud_backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.crud_backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper<User> 后，MyBatis-Plus 会自动注入
    // selectOne, insert, updateById, deleteById 等通用方法。
    // 所以对于登录检查（查询）和注册（新增）来说，这里保持为空即可。
}
