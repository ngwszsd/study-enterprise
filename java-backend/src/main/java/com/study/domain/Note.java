package com.study.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 协作笔记元数据。正文由 Yjs 文档快照保存到 note_documents。
 *
 * @TableName 绑定 notes 表;@TableId/@TableField 负责主键和时间字段映射。
 */
@TableName("notes")
public class Note {

    // @TableId(type = AUTO): 主键由 MySQL 自增生成。
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Long ownerId;

    // @TableField(fill = INSERT): 插入时自动填 created_at。
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    // @TableField(fill = INSERT_UPDATE): 插入/更新时自动填 updated_at。
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

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
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
