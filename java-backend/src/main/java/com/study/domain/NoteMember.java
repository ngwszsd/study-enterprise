package com.study.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 笔记成员权限。MVP 支持 OWNER / EDITOR / VIEWER 三种角色。
 *
 * @TableName 绑定 note_members 表;@TableId/@TableField 负责主键和创建时间映射。
 */
@TableName("note_members")
public class NoteMember {

    // @TableId(type = AUTO): 主键由 MySQL 自增生成。
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long noteId;

    private Long userId;

    private String role;

    // @TableField(fill = INSERT): 插入成员关系时自动填 created_at。
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getNoteId() {
        return noteId;
    }

    public void setNoteId(Long noteId) {
        this.noteId = noteId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
