package com.study.domain

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/** 用户实体(MyBatis-Plus)。用可变 var + 默认值,保证 MP 反射所需的无参构造。 */
@TableName("users")
class User {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var username: String? = null

    /** BCrypt 哈希,绝不存明文。映射 password_hash。 */
    var passwordHash: String? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null
}
