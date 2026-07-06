package com.study.mapper;

import com.study.domain.NoteDocumentState;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** Yjs 文档快照 Mapper。ydoc_state 是 Hocuspocus 保存的二进制 update。 */
public interface NoteDocumentMapper {

    @Insert("INSERT INTO note_documents(note_id, ydoc_state, updated_at) VALUES(#{noteId}, #{state}, NOW())")
    void insertEmpty(@Param("noteId") Long noteId, @Param("state") byte[] state);

    @Select("SELECT ydoc_state AS ydocState FROM note_documents WHERE note_id = #{noteId}")
    NoteDocumentState selectState(@Param("noteId") Long noteId);

    @Update("UPDATE note_documents SET ydoc_state = #{state}, updated_at = NOW() WHERE note_id = #{noteId}")
    int updateState(@Param("noteId") Long noteId, @Param("state") byte[] state);
}
