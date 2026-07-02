package com.study.exception

/** 资源不存在 → 404 */
class ResourceNotFoundException(message: String) : RuntimeException(message)

/** 无权限操作 → 403 */
class ForbiddenException(message: String) : RuntimeException(message)

/** 资源冲突(如用户名已存在)→ 409 */
class ConflictException(message: String) : RuntimeException(message)

/** 未认证/凭据错误 → 401 */
class UnauthorizedException(message: String) : RuntimeException(message)

/** 非法文件(过大/类型不符)→ 400 */
class InvalidFileException(message: String) : RuntimeException(message)
