package net.bi4vmr.study

/**
 * Mock工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */

/**
 * 将Mock对象注入到目标对象中（非安全）。
 *
 * @param[target] 目标对象。
 * @param[fieldName] 目标属性名称。
 */
inline fun <reified T : Any> T.injectMockUnsafe(target: Any, fieldName: String) {
    target.javaClass.getDeclaredField(fieldName)
        .apply {
            isAccessible = true
            set(target, this@injectMockUnsafe)
        }
}

/**
 * 将Mock对象注入到目标对象中。
 *
 * @param[target] 目标对象。
 * @param[fieldName] 目标属性名称。
 */
inline fun <reified T : Any> T.injectMock(target: Any, fieldName: String) {
    try {
        injectMockUnsafe(target, fieldName)
    } catch (e: Exception) {
        System.err.println("Inject mock failed! Reason:[${e.message}]")
        // e.printStackTrace()
    }
}
