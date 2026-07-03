package com.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.study.domain.User;

/**
 * 用户 Mapper —— 打数据库的接口。
 *
 * 【前端类比】像你前端"调后端的 api 层",只不过它调的是数据库。继承 MyBatis-Plus 的 BaseMapper
 * 就自动拥有增删改查(insert/selectById/updateById/selectCount...),不用手写 SQL;
 * 复杂条件用 QueryWrapper(见 AuthService)。
 */
public interface UserMapper extends BaseMapper<User> {
}
