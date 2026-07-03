package com.study.web;

import com.study.exception.UnauthorizedException;
import com.study.security.AuthUser;
import com.study.service.NoteService;
import com.study.web.dto.CollabTokenResponse;
import com.study.web.dto.NoteDocumentStateRequest;
import com.study.web.dto.NoteDocumentStateResponse;
import com.study.web.dto.NoteMemberRequest;
import com.study.web.dto.NoteMemberResponse;
import com.study.web.dto.NoteRequest;
import com.study.web.dto.NoteResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 协作笔记接口:业务权限走普通 JWT,文档快照内部接口供 Hocuspocus 服务调用。 */
@RestController
@RequestMapping("/api/notes")
public class NoteController {

    private final NoteService noteService;
    private final String collabInternalSecret;

    public NoteController(NoteService noteService,
                          @Value("${collab.internal-secret:dev-collab-secret}") String collabInternalSecret) {
        this.noteService = noteService;
        this.collabInternalSecret = collabInternalSecret;
    }

    @GetMapping
    public List<NoteResponse> list(@AuthenticationPrincipal AuthUser user) {
        return noteService.list(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@Valid @RequestBody NoteRequest request, @AuthenticationPrincipal AuthUser user) {
        return noteService.create(request, user.id());
    }

    @GetMapping("/{id}")
    public NoteResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return noteService.get(id, user.id());
    }

    @PutMapping("/{id}")
    public NoteResponse update(@PathVariable Long id,
                               @Valid @RequestBody NoteRequest request,
                               @AuthenticationPrincipal AuthUser user) {
        return noteService.update(id, request, user.id());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        noteService.delete(id, user.id());
    }

    @GetMapping("/{id}/members")
    public List<NoteMemberResponse> members(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return noteService.members(id, user.id());
    }

    @PostMapping("/{id}/members")
    @ResponseStatus(HttpStatus.CREATED)
    public NoteMemberResponse addMember(@PathVariable Long id,
                                        @Valid @RequestBody NoteMemberRequest request,
                                        @AuthenticationPrincipal AuthUser user) {
        return noteService.addMember(id, user.id(), request.userId(), request.role());
    }

    @DeleteMapping("/{id}/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeMember(@PathVariable Long id,
                             @PathVariable Long userId,
                             @AuthenticationPrincipal AuthUser user) {
        noteService.removeMember(id, user.id(), userId);
    }

    @PostMapping("/{id}/collab-token")
    public CollabTokenResponse collabToken(@PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return noteService.collabToken(id, user.id(), user.username());
    }

    @GetMapping("/internal/{id}/document")
    public NoteDocumentStateResponse loadDocument(@PathVariable Long id,
                                                  @RequestHeader("X-Collab-Secret") String secret) {
        requireInternalSecret(secret);
        return new NoteDocumentStateResponse(noteService.loadDocumentState(id));
    }

    @PutMapping("/internal/{id}/document")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void saveDocument(@PathVariable Long id,
                             @RequestHeader("X-Collab-Secret") String secret,
                             @RequestBody NoteDocumentStateRequest request) {
        requireInternalSecret(secret);
        noteService.saveDocumentState(id, request.state());
    }

    private void requireInternalSecret(String secret) {
        if (!collabInternalSecret.equals(secret)) {
            throw new UnauthorizedException("协作服务密钥无效");
        }
    }
}
