package com.study.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.domain.Note;
import com.study.domain.NoteDocumentState;
import com.study.domain.NoteMember;
import com.study.domain.User;
import com.study.exception.ForbiddenException;
import com.study.exception.ResourceNotFoundException;
import com.study.mapper.NoteDocumentMapper;
import com.study.mapper.NoteMapper;
import com.study.mapper.NoteMemberMapper;
import com.study.mapper.UserMapper;
import com.study.security.JwtService;
import com.study.web.dto.CollabTokenResponse;
import com.study.web.dto.NoteMemberResponse;
import com.study.web.dto.NoteRequest;
import com.study.web.dto.NoteResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 实时协作笔记业务:元数据、成员权限、协作 token、Yjs 快照持久化。 */
@Service
public class NoteService {

    private static final long COLLAB_TOKEN_TTL_SECONDS = 300L;

    private final NoteMapper noteMapper;
    private final NoteMemberMapper noteMemberMapper;
    private final NoteDocumentMapper noteDocumentMapper;
    private final UserMapper userMapper;
    private final JwtService jwtService;
    private final String collabUrl;

    public NoteService(NoteMapper noteMapper,
                       NoteMemberMapper noteMemberMapper,
                       NoteDocumentMapper noteDocumentMapper,
                       UserMapper userMapper,
                       JwtService jwtService,
                       @Value("${collab.url:ws://localhost:19082}") String collabUrl) {
        this.noteMapper = noteMapper;
        this.noteMemberMapper = noteMemberMapper;
        this.noteDocumentMapper = noteDocumentMapper;
        this.userMapper = userMapper;
        this.jwtService = jwtService;
        this.collabUrl = collabUrl;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(Long userId) {
        List<NoteMember> memberships = noteMemberMapper.selectList(new LambdaQueryWrapper<NoteMember>()
                .eq(NoteMember::getUserId, userId)
                .orderByDesc(NoteMember::getCreatedAt));
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, String> roles = memberships.stream()
                .collect(Collectors.toMap(NoteMember::getNoteId, NoteMember::getRole, (a, b) -> a));
        List<Long> noteIds = memberships.stream().map(NoteMember::getNoteId).toList();
        return noteMapper.selectBatchIds(noteIds).stream()
                .map(note -> toResponse(note, roles.get(note.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public NoteResponse get(Long id, Long userId) {
        Note note = requireNote(id);
        String role = requireRole(id, userId);
        return toResponse(note, role);
    }

    @Transactional
    public NoteResponse create(NoteRequest request, Long ownerId) {
        Note note = new Note();
        note.setTitle(request.title());
        note.setOwnerId(ownerId);
        noteMapper.insert(note);

        NoteMember owner = new NoteMember();
        owner.setNoteId(note.getId());
        owner.setUserId(ownerId);
        owner.setRole("OWNER");
        noteMemberMapper.insert(owner);
        noteDocumentMapper.insertEmpty(note.getId(), new byte[0]);
        return toResponse(note, "OWNER");
    }

    @Transactional
    public NoteResponse update(Long id, NoteRequest request, Long userId) {
        Note note = requireNote(id);
        requireOwner(note, userId);
        note.setTitle(request.title());
        noteMapper.updateById(note);
        return toResponse(note, "OWNER");
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Note note = requireNote(id);
        requireOwner(note, userId);
        noteMapper.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<NoteMemberResponse> members(Long noteId, Long userId) {
        requireRole(noteId, userId);
        List<NoteMember> members = noteMemberMapper.selectList(new LambdaQueryWrapper<NoteMember>()
                .eq(NoteMember::getNoteId, noteId));
        List<Long> userIds = members.stream().map(NoteMember::getUserId).toList();
        Map<Long, String> usernames = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getUsername));
        return members.stream()
                .map(member -> new NoteMemberResponse(
                        member.getUserId(), usernames.get(member.getUserId()), member.getRole()))
                .toList();
    }

    @Transactional
    public NoteMemberResponse addMember(Long noteId, Long ownerId, Long memberUserId, String role) {
        Note note = requireNote(noteId);
        requireOwner(note, ownerId);
        User user = userMapper.selectById(memberUserId);
        if (user == null) {
            throw new ResourceNotFoundException("用户不存在");
        }
        NoteMember existing = findMember(noteId, memberUserId);
        if (existing == null) {
            NoteMember member = new NoteMember();
            member.setNoteId(noteId);
            member.setUserId(memberUserId);
            member.setRole(role == null ? "EDITOR" : role);
            noteMemberMapper.insert(member);
            return new NoteMemberResponse(memberUserId, user.getUsername(), member.getRole());
        }
        if ("OWNER".equals(existing.getRole())) {
            throw new ForbiddenException("不能修改笔记拥有者角色");
        }
        existing.setRole(role == null ? "EDITOR" : role);
        noteMemberMapper.updateById(existing);
        return new NoteMemberResponse(memberUserId, user.getUsername(), existing.getRole());
    }

    @Transactional
    public void removeMember(Long noteId, Long ownerId, Long memberUserId) {
        Note note = requireNote(noteId);
        requireOwner(note, ownerId);
        if (note.getOwnerId().equals(memberUserId)) {
            throw new ForbiddenException("不能移除笔记拥有者");
        }
        NoteMember member = findMember(noteId, memberUserId);
        if (member != null) {
            noteMemberMapper.deleteById(member.getId());
        }
    }

    @Transactional(readOnly = true)
    public CollabTokenResponse collabToken(Long noteId, Long userId, String username) {
        String role = requireRole(noteId, userId);
        if ("VIEWER".equals(role)) {
            throw new ForbiddenException("只读成员暂不能进入协作编辑");
        }
        String token = jwtService.generateCollabToken(userId, username, noteId, role, COLLAB_TOKEN_TTL_SECONDS);
        return new CollabTokenResponse(token, COLLAB_TOKEN_TTL_SECONDS, "note:" + noteId, role, collabUrl);
    }

    @Transactional(readOnly = true)
    public String loadDocumentState(Long noteId) {
        requireNote(noteId);
        NoteDocumentState document = noteDocumentMapper.selectState(noteId);
        byte[] state = document == null ? null : document.getYdocState();
        if (state == null || state.length == 0) {
            return "";
        }
        return Base64.getEncoder().encodeToString(state);
    }

    @Transactional
    public void saveDocumentState(Long noteId, String base64State) {
        requireNote(noteId);
        byte[] state = base64State == null || base64State.isBlank()
                ? new byte[0]
                : Base64.getDecoder().decode(base64State);
        int updated = noteDocumentMapper.updateState(noteId, state);
        if (updated == 0) {
            noteDocumentMapper.insertEmpty(noteId, state);
        }
    }

    private Note requireNote(Long id) {
        Note note = noteMapper.selectById(id);
        if (note == null) {
            throw new ResourceNotFoundException("笔记不存在");
        }
        return note;
    }

    private String requireRole(Long noteId, Long userId) {
        NoteMember member = findMember(noteId, userId);
        if (member == null) {
            throw new ForbiddenException("无权访问该笔记");
        }
        return member.getRole();
    }

    private void requireOwner(Note note, Long userId) {
        if (!note.getOwnerId().equals(userId)) {
            throw new ForbiddenException("只有笔记拥有者可以操作");
        }
    }

    private NoteMember findMember(Long noteId, Long userId) {
        return noteMemberMapper.selectOne(new LambdaQueryWrapper<NoteMember>()
                .eq(NoteMember::getNoteId, noteId)
                .eq(NoteMember::getUserId, userId));
    }

    private NoteResponse toResponse(Note note, String role) {
        User owner = userMapper.selectById(note.getOwnerId());
        String ownerUsername = owner == null ? null : owner.getUsername();
        return new NoteResponse(
                note.getId(), note.getTitle(), note.getOwnerId(), ownerUsername,
                role, note.getCreatedAt(), note.getUpdatedAt());
    }
}
