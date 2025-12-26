package net.bi4vmr.study

import java.lang.reflect.Method

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

/**
 * 获取目标对象中的属性。
 *
 * @param[target] 目标对象。
 * @param[fieldName] 目标属性名称。
 */
inline fun <reified T : Any> getField(target: Any, fieldName: String): T? {
    try {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(target) as T
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}

fun Any.getMethod(methodName: String, vararg parameterTypes: Class<*>): Method? {
    return try {
        val method = this.javaClass.getDeclaredMethod(methodName, *parameterTypes)
        method.isAccessible = true
        method
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
