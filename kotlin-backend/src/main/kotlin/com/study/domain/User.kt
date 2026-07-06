package com.study.domain

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
 * 用户实体(MyBatis-Plus)。用可变 var + 默认值,保证 MP 反射所需的无参构造。
 *
 * @TableName 绑定 users 表;@TableId/@TableField 负责主键和自动填充字段。
 */
@TableName("users")
class User {
    // @TableId(type = AUTO): 主键由 MySQL 自增生成。
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var username: String? = null

    /** BCrypt 哈希,绝不存明文。映射 password_hash。 */
    var passwordHash: String? = null

    // @TableField(fill = INSERT): 插入时由 MyMetaObjectHandler 自动写入创建时间。
    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null
}
