package com.study.config

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler
import java.time.LocalDateTime
import org.apache.ibatis.reflection.MetaObject
import org.springframework.stereotype.Component

/**
 * 自动填充:插入填 createdAt/updatedAt,更新填 updatedAt。
 *
 * @Component 是最通用的 Spring 组件注解,作用接近 Nest @Injectable:交给容器创建并被框架发现。
 */
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
