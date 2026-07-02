package com.study.config

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import java.time.LocalDateTime
import org.apache.ibatis.reflection.MetaObject
import org.springframework.stereotype.Component

/** 自动填充:插入填 createdAt/updatedAt,更新填 updatedAt。 */
@Component
class MyMetaObjectHandler : MetaObjectHandler {
    override fun insertFill(metaObject: MetaObject) {
        val now = LocalDateTime.now()
        strictInsertFill(metaObject, "createdAt", LocalDateTime::class.java, now)
        strictInsertFill(metaObject, "updatedAt", LocalDateTime::class.java, now)
    }

    override fun updateFill(metaObject: MetaObject) {
        strictUpdateFill(metaObject, "updatedAt", LocalDateTime::class.java, LocalDateTime.now())
    }
}
