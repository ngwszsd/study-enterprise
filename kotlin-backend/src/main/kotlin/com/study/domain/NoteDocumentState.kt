package com.study.domain

/** note_documents.ydoc_state 的查询结果包装,避免 MyBatis 把 ByteArray 误判成单个 Byte。 */
class NoteDocumentState {
    var ydocState: ByteArray? = null
}
