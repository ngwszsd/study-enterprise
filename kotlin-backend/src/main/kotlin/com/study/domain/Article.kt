package com.study.domain

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/**
 * 文章实体(MyBatis-Plus)。创建/更新时间由 MyMetaObjectHandler 自动填充。
 *
 * @TableName 绑定数据库表;@TableId 声明主键策略;@TableField(fill=...) 声明自动填充字段。
 */
@TableName("articles")
class Article {
    // @TableId(type = AUTO): 主键由 MySQL 自增生成。
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var title: String? = null

    var content: String? = null

    var category: String? = null

    /** MinIO 对象 key;映射 cover_image_key。 */
    var coverImageKey: String? = null

    /** 映射 author_id。 */
    var authorId: Long? = null

    // @TableField(fill = INSERT): 插入时由 MyMetaObjectHandler 自动写入创建时间。
    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    // @TableField(fill = INSERT_UPDATE): 插入和更新时自动维护更新时间。
    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: LocalDateTime? = null
}
