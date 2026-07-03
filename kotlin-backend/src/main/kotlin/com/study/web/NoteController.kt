package com.study.web

import com.study.exception.UnauthorizedException
import com.study.security.AuthUser
import com.study.service.NoteService
import com.study.web.dto.CollabTokenResponse
import com.study.web.dto.NoteDocumentStateRequest
import com.study.web.dto.NoteDocumentStateResponse
import com.study.web.dto.NoteMemberRequest
import com.study.web.dto.NoteMemberResponse
import com.study.web.dto.NoteRequest
import com.study.web.dto.NoteResponse
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

/** 协作笔记接口:业务权限走普通 JWT,文档快照内部接口供 Hocuspocus 服务调用。 */
@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteService: NoteService,
    @Value("\${collab.internal-secret:dev-collab-secret}") private val collabInternalSecret: String,
) {

    @GetMapping
    fun list(@AuthenticationPrincipal user: AuthUser): List<NoteResponse> = noteService.list(user.id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody request: NoteRequest, @AuthenticationPrincipal user: AuthUser): NoteResponse =
        noteService.create(request, user.id)

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long, @AuthenticationPrincipal user: AuthUser): NoteResponse =
        noteService.get(id, user.id)

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: NoteRequest,
        @AuthenticationPrincipal user: AuthUser,
    ): NoteResponse = noteService.update(id, request, user.id)

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable id: Long, @AuthenticationPrincipal user: AuthUser) = noteService.delete(id, user.id)

    @GetMapping("/{id}/members")
    fun members(@PathVariable id: Long, @AuthenticationPrincipal user: AuthUser): List<NoteMemberResponse> =
        noteService.members(id, user.id)

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    fun addMember(
        @PathVariable id: Long,
        @Valid @RequestBody request: NoteMemberRequest,
        @AuthenticationPrincipal user: AuthUser,
    ): NoteMemberResponse = noteService.addMember(id, user.id, request.userId, request.role)

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(
        @PathVariable id: Long,
        @PathVariable userId: Long,
        @AuthenticationPrincipal user: AuthUser,
    ) = noteService.removeMember(id, user.id, userId)

    @PostMapping("/{id}/collab-token")
    fun collabToken(@PathVariable id: Long, @AuthenticationPrincipal user: AuthUser): CollabTokenResponse =
        noteService.collabToken(id, user.id, user.username)

    @GetMapping("/internal/{id}/document")
    fun loadDocument(
        @PathVariable id: Long,
        @RequestHeader("X-Collab-Secret") secret: String,
    ): NoteDocumentStateResponse {
        requireInternalSecret(secret)
        return NoteDocumentStateResponse(noteService.loadDocumentState(id))
    }

    @PutMapping("/internal/{id}/document")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun saveDocument(
        @PathVariable id: Long,
        @RequestHeader("X-Collab-Secret") secret: String,
        @RequestBody request: NoteDocumentStateRequest,
    ) {
        requireInternalSecret(secret)
        noteService.saveDocumentState(id, request.state)
    }

    private fun requireInternalSecret(secret: String) {
        if (secret != collabInternalSecret) {
            throw UnauthorizedException("协作服务密钥无效")
        }
    }
}
