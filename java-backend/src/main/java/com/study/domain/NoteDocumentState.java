package com.study.domain;

/** note_documents.ydoc_state 的查询结果包装,避免 MyBatis 把 byte[] 误判成单个 Byte。 */
public class NoteDocumentState {

    private byte[] ydocState;

    public byte[] getYdocState() {
        return ydocState;
    }

    public void setYdocState(byte[] ydocState) {
        this.ydocState = ydocState;
    }
}
