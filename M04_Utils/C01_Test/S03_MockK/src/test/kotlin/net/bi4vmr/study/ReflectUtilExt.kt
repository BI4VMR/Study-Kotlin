@file:Suppress("UNCHECKED_CAST")

/*
 * JVM反射相关工具的扩展方法。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */

package net.bi4vmr.study

import java.lang.reflect.Field
import java.lang.reflect.Method

fun Any.requireField(name: String): Field = ReflectUtil.requireField(this.javaClass, name)

fun Any.getField(name: String): Field? = ReflectUtil.getField(this.javaClass, name)

fun <T : Any> Any.requireFieldValue(name: String): T = ReflectUtil.requireFieldValue(this, name)

fun <T : Any> Any.getFieldValue(name: String): T? = ReflectUtil.getFieldValue(this, name)

fun <T : Any> Class<*>.requireFieldValue(name: String): T = ReflectUtil.requireFieldValue<T>(this, name)

fun <T : Any> Class<*>.getFieldValue(name: String): T = ReflectUtil.getFieldValue<T>(this, name)

fun Any.setFieldValueUnsafe(name: String, value: Any) = ReflectUtil.setFieldValueUnsafe(this, name, value)

fun Class<*>.setFieldValueUnsafe(name: String, value: Any) = ReflectUtil.setFieldValueUnsafe(this, name, value)

fun Any.setFieldValue(name: String, value: Any): Boolean = ReflectUtil.setFieldValue(this, name, value)

fun Class<*>.setFieldValue(name: String, value: Any): Boolean = ReflectUtil.setFieldValue(this, name, value)

fun Any.setFieldToNullUnsafe(name: String) = ReflectUtil.setFieldToNullUnsafe(this, name)

fun Any.setFieldToNull(name: String): Boolean = ReflectUtil.setFieldToNull(this, name)

fun Any.requireMethod(name: String, vararg parameterTypes: Class<*>?): Method =
    ReflectUtil.requireMethod(this.javaClass, name, *parameterTypes)

fun Any.getMethod(name: String, vararg parameterTypes: Class<*>?): Method? =
    ReflectUtil.getMethod(this.javaClass, name, *parameterTypes)

fun Method.callWithUnsafe(target: Any?, vararg parameters: Any?) = ReflectUtil.callWithUnsafe(target, this, *parameters)

fun Method.callWith(target: Any?, vararg parameters: Any?): Boolean = ReflectUtil.callWith(target, this, *parameters)

fun <T : Any> Method.callWithForUnsafe(target: Any?, vararg parameters: Any?): T? =
    ReflectUtil.callWithForUnsafe<T>(target, this, *parameters)

fun <T : Any> Method.callWithFor(target: Any?, vararg parameters: Any?): T? =
    ReflectUtil.callWithFor<T>(target, this, *parameters)
