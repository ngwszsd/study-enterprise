package com.study.domain

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/** 笔记成员权限。MVP 支持 OWNER / EDITOR / VIEWER 三种角色。 */
@TableName("note_members")
class NoteMember {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var noteId: Long? = null

    var userId: Long? = null

    var role: String? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null
}
