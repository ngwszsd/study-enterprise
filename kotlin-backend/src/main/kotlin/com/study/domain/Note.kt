package com.study.domain

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
 * 协作笔记元数据。正文由 Yjs 文档快照保存到 note_documents。
 *
 * @TableName 绑定 notes 表;@TableId/@TableField 负责主键和时间字段映射。
 */
@TableName("notes")
class Note {
    // @TableId(type = AUTO): 主键由 MySQL 自增生成。
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var title: String? = null

    var ownerId: Long? = null

    // @TableField(fill = INSERT): 插入时自动填 created_at。
    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    // @TableField(fill = INSERT_UPDATE): 插入/更新时自动填 updated_at。
    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: LocalDateTime? = null
}
