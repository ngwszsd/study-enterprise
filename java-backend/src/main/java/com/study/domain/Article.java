package com.study.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 文章实体(MyBatis-Plus)。创建/更新时间由 MyMetaObjectHandler 自动填充。
 *
 * @TableName 绑定数据库表;@TableId 声明主键策略;@TableField(fill=...) 声明自动填充字段。
 */
@TableName("articles")
public class Article {

    // @TableId(type = AUTO): 主键由 MySQL 自增生成。
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    private String category;

    /** MinIO 对象 key;映射 cover_image_key。 */
    private String coverImageKey;

    /** 映射 author_id。 */
    private Long authorId;

    // @TableField(fill = INSERT): 插入时由 MyMetaObjectHandler 自动写入创建时间。
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // @TableField(fill = INSERT_UPDATE): 插入和更新时自动维护更新时间。
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
