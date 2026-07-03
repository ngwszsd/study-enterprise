package com.study.mapper

import org.apache.ibatis.annotations.Insert
import org.apache.ibatis.annotations.Param
import org.apache.ibatis.annotations.Select
import org.apache.ibatis.annotations.Update

/** Yjs 文档快照 Mapper。ydoc_state 是 Hocuspocus 保存的二进制 update。 */
interface NoteDocumentMapper {
    @Insert("INSERT INTO note_documents(note_id, ydoc_state, updated_at) VALUES(#{noteId}, #{state}, NOW())")
    fun insertEmpty(@Param("noteId") noteId: Long, @Param("state") state: ByteArray)

    @Select("SELECT ydoc_state FROM note_documents WHERE note_id = #{noteId}")
    fun selectState(@Param("noteId") noteId: Long): ByteArray?

    @Update("UPDATE note_documents SET ydoc_state = #{state}, updated_at = NOW() WHERE note_id = #{noteId}")
    fun updateState(@Param("noteId") noteId: Long, @Param("state") state: ByteArray): Int
}
