-- 实时协作笔记:MVP 元数据 + 成员权限 + Yjs 二进制快照
CREATE TABLE notes (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    title      VARCHAR(200) NOT NULL,
    owner_id   BIGINT       NOT NULL,
    created_at DATETIME     NOT NULL,
    updated_at DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_notes_owner (owner_id),
    CONSTRAINT fk_notes_owner FOREIGN KEY (owner_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE note_members (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    note_id    BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    role       VARCHAR(20) NOT NULL,
    created_at DATETIME    NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_members_note_user (note_id, user_id),
    KEY idx_note_members_user (user_id),
    CONSTRAINT fk_note_members_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE,
    CONSTRAINT fk_note_members_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE TABLE note_documents (
    note_id    BIGINT   NOT NULL,
    ydoc_state LONGBLOB NULL,
    updated_at DATETIME NOT NULL,
    PRIMARY KEY (note_id),
    CONSTRAINT fk_note_documents_note FOREIGN KEY (note_id) REFERENCES notes (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;
