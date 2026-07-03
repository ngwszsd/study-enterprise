package com.study.service

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper
import com.study.domain.Note
import com.study.domain.NoteMember
import com.study.exception.ForbiddenException
import com.study.exception.ResourceNotFoundException
import com.study.mapper.NoteDocumentMapper
import com.study.mapper.NoteMapper
import com.study.mapper.NoteMemberMapper
import com.study.mapper.UserMapper
import com.study.security.JwtService
import com.study.web.dto.CollabTokenResponse
import com.study.web.dto.NoteMemberResponse
import com.study.web.dto.NoteRequest
import com.study.web.dto.NoteResponse
import java.util.Base64
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 实时协作笔记业务:元数据、成员权限、协作 token、Yjs 快照持久化。 */
@Service
class NoteService(
    private val noteMapper: NoteMapper,
    private val noteMemberMapper: NoteMemberMapper,
    private val noteDocumentMapper: NoteDocumentMapper,
    private val userMapper: UserMapper,
    private val jwtService: JwtService,
    @Value("\${collab.url:ws://localhost:19082}") private val collabUrl: String,
) {
    companion object {
        private const val COLLAB_TOKEN_TTL_SECONDS = 300L
    }

    @Transactional(readOnly = true)
    fun list(userId: Long): List<NoteResponse> {
        val memberships = noteMemberMapper.selectList(
            QueryWrapper<NoteMember>()
                .eq("user_id", userId)
                .orderByDesc("created_at"),
        )
        if (memberships.isEmpty()) return emptyList()
        val roles = memberships.associate { it.noteId!! to it.role!! }
        val noteIds = memberships.mapNotNull { it.noteId }
        return noteMapper.selectBatchIds(noteIds).map { toResponse(it, roles[it.id] ?: "VIEWER") }
    }

    @Transactional(readOnly = true)
    fun get(id: Long, userId: Long): NoteResponse {
        val note = requireNote(id)
        val role = requireRole(id, userId)
        return toResponse(note, role)
    }

    @Transactional
    fun create(request: NoteRequest, ownerId: Long): NoteResponse {
        val note = Note().apply {
            title = request.title
            this.ownerId = ownerId
        }
        noteMapper.insert(note)

        val owner = NoteMember().apply {
            noteId = note.id
            userId = ownerId
            role = "OWNER"
        }
        noteMemberMapper.insert(owner)
        noteDocumentMapper.insertEmpty(note.id!!, ByteArray(0))
        return toResponse(note, "OWNER")
    }

    @Transactional
    fun update(id: Long, request: NoteRequest, userId: Long): NoteResponse {
        val note = requireNote(id)
        requireOwner(note, userId)
        note.title = request.title
        noteMapper.updateById(note)
        return toResponse(note, "OWNER")
    }

    @Transactional
    fun delete(id: Long, userId: Long) {
        val note = requireNote(id)
        requireOwner(note, userId)
        noteMapper.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun members(noteId: Long, userId: Long): List<NoteMemberResponse> {
        requireRole(noteId, userId)
        val members = noteMemberMapper.selectList(QueryWrapper<NoteMember>().eq("note_id", noteId))
        val userIds = members.mapNotNull { it.userId }
        val usernames = userMapper.selectBatchIds(userIds).associate { it.id!! to it.username }
        return members.map { NoteMemberResponse(it.userId, usernames[it.userId], it.role) }
    }

    @Transactional
    fun addMember(noteId: Long, ownerId: Long, memberUserId: Long, role: String?): NoteMemberResponse {
        val note = requireNote(noteId)
        requireOwner(note, ownerId)
        val user = userMapper.selectById(memberUserId) ?: throw ResourceNotFoundException("用户不存在")
        val existing = findMember(noteId, memberUserId)
        if (existing == null) {
            val member = NoteMember().apply {
                this.noteId = noteId
                userId = memberUserId
                this.role = role ?: "EDITOR"
            }
            noteMemberMapper.insert(member)
            return NoteMemberResponse(memberUserId, user.username, member.role)
        }
        if (existing.role == "OWNER") {
            throw ForbiddenException("不能修改笔记拥有者角色")
        }
        existing.role = role ?: "EDITOR"
        noteMemberMapper.updateById(existing)
        return NoteMemberResponse(memberUserId, user.username, existing.role)
    }

    @Transactional
    fun removeMember(noteId: Long, ownerId: Long, memberUserId: Long) {
        val note = requireNote(noteId)
        requireOwner(note, ownerId)
        if (note.ownerId == memberUserId) {
            throw ForbiddenException("不能移除笔记拥有者")
        }
        findMember(noteId, memberUserId)?.let { noteMemberMapper.deleteById(it.id) }
    }

    @Transactional(readOnly = true)
    fun collabToken(noteId: Long, userId: Long, username: String): CollabTokenResponse {
        val role = requireRole(noteId, userId)
        if (role == "VIEWER") {
            throw ForbiddenException("只读成员暂不能进入协作编辑")
        }
        val token = jwtService.generateCollabToken(userId, username, noteId, role, COLLAB_TOKEN_TTL_SECONDS)
        return CollabTokenResponse(token, COLLAB_TOKEN_TTL_SECONDS, "note:$noteId", role, collabUrl)
    }

    @Transactional(readOnly = true)
    fun loadDocumentState(noteId: Long): String {
        requireNote(noteId)
        val state = noteDocumentMapper.selectState(noteId)
        return if (state == null || state.isEmpty()) "" else Base64.getEncoder().encodeToString(state)
    }

    @Transactional
    fun saveDocumentState(noteId: Long, base64State: String?) {
        requireNote(noteId)
        val state = if (base64State.isNullOrBlank()) ByteArray(0) else Base64.getDecoder().decode(base64State)
        val updated = noteDocumentMapper.updateState(noteId, state)
        if (updated == 0) {
            noteDocumentMapper.insertEmpty(noteId, state)
        }
    }

    private fun requireNote(id: Long): Note =
        noteMapper.selectById(id) ?: throw ResourceNotFoundException("笔记不存在")

    private fun requireRole(noteId: Long, userId: Long): String =
        findMember(noteId, userId)?.role ?: throw ForbiddenException("无权访问该笔记")

    private fun requireOwner(note: Note, userId: Long) {
        if (note.ownerId != userId) {
            throw ForbiddenException("只有笔记拥有者可以操作")
        }
    }

    private fun findMember(noteId: Long, userId: Long): NoteMember? =
        noteMemberMapper.selectOne(
            QueryWrapper<NoteMember>()
                .eq("note_id", noteId)
                .eq("user_id", userId),
        )

    private fun toResponse(note: Note, role: String): NoteResponse {
        val owner = note.ownerId?.let { userMapper.selectById(it) }
        return NoteResponse(note.id, note.title, note.ownerId, owner?.username, role, note.createdAt, note.updatedAt)
    }
}
