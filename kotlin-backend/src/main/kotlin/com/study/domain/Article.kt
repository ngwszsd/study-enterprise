package com.study.domain

import com.baomidou.mybatisplus.annotation.FieldFill
import com.baomidou.mybatisplus.annotation.IdType
import com.baomidou.mybatisplus.annotation.TableField
import com.baomidou.mybatisplus.annotation.TableId
import com.baomidou.mybatisplus.annotation.TableName
import java.time.LocalDateTime

/** 文章实体(MyBatis-Plus)。创建/更新时间由 MyMetaObjectHandler 自动填充。 */
@TableName("articles")
class Article {
    @TableId(type = IdType.AUTO)
    var id: Long? = null

    var title: String? = null

    var content: String? = null

    var category: String? = null

    /** MinIO 对象 key;映射 cover_image_key。 */
    var coverImageKey: String? = null

    /** 映射 author_id。 */
    var authorId: Long? = null

    @TableField(fill = FieldFill.INSERT)
    var createdAt: LocalDateTime? = null

    @TableField(fill = FieldFill.INSERT_UPDATE)
    var updatedAt: LocalDateTime? = null
}
