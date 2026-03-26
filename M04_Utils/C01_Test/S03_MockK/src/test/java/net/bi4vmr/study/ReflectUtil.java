package net.bi4vmr.study;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * JVM反射相关工具。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
@SuppressWarnings("unchecked")
public class ReflectUtil {

    /* ----- 字段相关操作 ----- */

    /**
     * 获取类的字段。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。如果方法存在访问限制，限制将被解除。
     * <p>
     * 本方法不会返回空值，但默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param clazz 目标类的Class。
     * @param name  目标字段名称。
     * @return 目标字段。
     * @throws NoSuchFieldException 若在当前类和父类中均未找到指定的属性，则抛出该异常。
     */
    public static Field requireField(Class<?> clazz, String name) throws NoSuchFieldException {
        Class<?> currentCls = clazz;

        /*
         * 遍历继承关系链，尝试查找目标字段。
         *
         * 如果一个类没有父类，说明该类已经是顶级类Object，目标字段确实不存在，可以终止循环。
         */
        while (currentCls != null) {
            // 首先尝试查找当前类的属性
            try {
                Field field = currentCls.getDeclaredField(name);
                // 如果已获取到字段，则解除访问限制，以便后续进一步操作。
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                // 如果当前类没有该字段，则更新当前Class为父类，下一轮循环在父类中继续查找。
                currentCls = currentCls.getSuperclass();
            }
        }

        // 如果循环内部并未找到字段提前返回，说明在当前类和父类中均未找到指定的字段，抛出异常。
        throw new NoSuchFieldException();
    }

    /**
     * 获取类的字段。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。
     * <p>
     * 对于私有字段不会自动解除访问限制，调用者需要自行处理。
     * <p>
     * 本方法不会抛出异常，但如果目标属性不存在或出现其他错误则返回空值。
     *
     * @param clazz 目标类的Class。
     * @param name  目标字段名称。
     * @return 目标字段。
     */
    public static Field getField(Class<?> clazz, String name) {
        try {
            return requireField(clazz, name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取对象的字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并读取属性值，然后转换为泛型
     * 所指定的类型并返回。
     * <p>
     * 本方法不会返回空值，但默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param <T>    字段类型。
     * @param target 目标对象实例。
     * @param name   目标字段名称。
     * @return 目标字段值。
     * @throws NoSuchFieldException   若在当前类和父类中均未找到指定的属性，则抛出该异常。
     * @throws IllegalAccessException 字段不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static <T> T requireFieldValue(Object target, String name) throws NoSuchFieldException, IllegalAccessException {
        Field field = requireField(target.getClass(), name);
        // 将属性值转为目标类型并返回
        return (T) field.get(target);
    }

    /**
     * 获取对象的字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并读取属性值，然后转换为泛型
     * 所指定的类型并返回。
     * <p>
     * 本方法不会抛出异常，但如果目标属性不存在或出现其他错误则返回空值。
     *
     * @param <T>    字段类型。
     * @param target 目标对象实例。
     * @param name   目标字段名称。
     * @return 目标字段值。
     */
    public static <T> T getFieldValue(Object target, String name) {
        try {
            return requireFieldValue(target, name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取静态字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并读取属性值，然后转换为泛型
     * 所指定的类型并返回。
     * <p>
     * 本方法不会返回空值，但默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param <T>   字段类型。
     * @param clazz 目标类Class。
     * @param name  目标字段名称。
     * @return 目标字段值。
     * @throws NoSuchFieldException     若在当前类和父类中均未找到指定的属性，则抛出该异常。
     * @throws IllegalArgumentException 若指定的属性不是静态成员，则抛出该异常。
     * @throws IllegalAccessException   字段不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static <T> T requireFieldValue(Class<?> clazz, String name)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = requireField(clazz, name);

        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Field [" + name + "] is not static!");
        }

        // 将属性值转为目标类型并返回
        return (T) field.get(null);
    }

    /**
     * 获取静态字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并读取属性值，然后转换为泛型
     * 所指定的类型并返回。
     * <p>
     * 本方法不会抛出异常，但如果目标属性不存在或出现其他错误则返回空值。
     *
     * @param <T>   字段类型。
     * @param clazz 目标类Class。
     * @param name  目标字段名称。
     * @return 目标字段值。
     */
    public static <T> T getFieldValue(Class<?> clazz, String name) {
        try {
            return requireFieldValue(clazz, name);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 设置对象的字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并设置属性值。
     * <p>
     * 本方法默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param target 目标对象实例。
     * @param name   目标字段名称。
     * @param value  字段值。
     * @throws NoSuchFieldException   若在当前类和父类中均未找到指定的属性，则抛出该异常。
     * @throws IllegalAccessException 字段不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static void setFieldValueUnsafe(Object target, String name, Object value) throws NoSuchFieldException, IllegalAccessException {
        Field field = requireField(target.getClass(), name);
        field.set(target, value);
    }

    /**
     * 设置对象的字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并设置属性值。
     * <p>
     * 本方法不会抛出异常，如果目标属性不存在或出现其他错误，则忽略这些错误。。
     *
     * @param target 目标对象实例。
     * @param name   目标字段名称。
     * @param value  字段值。
     * @return `true` 表示设置成功， `false` 表示设置失败，有异常出现。
     */
    public static boolean setFieldValue(Object target, String name, Object value) {
        try {
            setFieldValueUnsafe(target, name, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置静态字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并设置属性值。
     * <p>
     * 本方法默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param clazz 目标类Class。
     * @param name  目标字段名称。
     * @param value 字段值。
     * @throws NoSuchFieldException     若在当前类和父类中均未找到指定的属性，则抛出该异常。
     * @throws IllegalArgumentException 若指定的属性不是静态成员，则抛出该异常。
     * @throws IllegalAccessException   字段不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static void setFieldValueUnsafe(Class<?> clazz, String name, Object value)
            throws NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Field field = requireField(clazz, name);

        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalArgumentException("Field [" + name + "] is not static!");
        }

        field.set(null, value);
    }

    /**
     * 设置静态字段值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并设置属性值。
     * <p>
     * 本方法不会抛出异常，如果目标属性不存在或出现其他错误，则忽略这些错误。。
     *
     * @param clazz 目标类Class。
     * @param name  目标字段名称。
     * @param value 字段值。
     * @return `true` 表示设置成功， `false` 表示设置失败，有异常出现。
     */
    public static boolean setFieldValue(Class<?> clazz, String name, Object value) {
        try {
            setFieldValueUnsafe(clazz, name, value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 设置对象的字段为空值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并将值设为 `NULL` 。
     * <p>
     * 本方法默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param target 目标对象实例。
     * @param name   目标字段名称。
     * @throws NoSuchFieldException   若在当前类和父类中均未找到指定的属性，则抛出该异常。
     * @throws IllegalAccessException 字段不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static void setFieldToNullUnsafe(Object target, String name) throws NoSuchFieldException, IllegalAccessException {
        setFieldValueUnsafe(target, name, null);
    }

    /**
     * 设置对象的字段为空值。
     * <p>
     * 首先在当前类声明的公开和私有属性中查找，如果未找到则尝试从父类继承的属性中查找。找到属性后自动解除访问限制，并将值设为 `NULL` 。
     * <p>
     * 本方法不会抛出异常，如果目标属性不存在或出现其他错误，则忽略这些错误。。
     *
     * @param target 目标对象实例。
     * @param name   目标字段名称。
     * @return `true` 表示设置成功， `false` 表示设置失败，有异常出现。
     */
    public static boolean setFieldToNull(Object target, String name) {
        try {
            setFieldToNullUnsafe(target, name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }


    /* ----- 方法相关操作 ----- */

    /**
     * 获取方法。
     * <p>
     * 首先在当前类声明的公开和私有方法中查找，如果未找到则尝试从父类继承的方法中查找。如果方法存在访问限制，限制将被解除。
     * <p>
     * 本方法不会返回空值，但默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param clazz 目标类Class。
     * @param name  目标方法名称。
     * @return 目标方法。
     * @throws NoSuchMethodException 若在当前类和父类中均未找到指定的方法，则抛出该异常。
     */
    public static Method requireMethod(Class<?> clazz, String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        Class<?> currentCls = clazz;

        /*
         * 遍历继承关系链，尝试查找目标方法。
         *
         * 如果一个类没有父类，说明该类已经是顶级类Object，目标方法确实不存在，可以终止循环。
         */
        while (currentCls != null) {
            try {
                Method method = currentCls.getDeclaredMethod(name, parameterTypes);
                // 如果已获取到方法，则解除访问限制，以便后续进一步操作。
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                // 如果当前类没有该方法，则更新当前Class为父类，下一轮循环在父类中继续查找。
                currentCls = currentCls.getSuperclass();
            }
        }

        // 如果循环内部并未找到方法提前返回，说明在当前类和父类中均未找到指定的方法，抛出异常。
        throw new NoSuchMethodException();
    }

    /**
     * 获取方法。
     * <p>
     * 首先在当前类声明的公开和私有方法中查找，如果未找到则尝试从父类继承的方法中查找。
     * <p>
     * 本方法不会抛出异常，但如果目标方法不存在或出现其他错误则返回空值。
     *
     * @param clazz 目标类Class。
     * @param name  目标方法名称。
     * @return 目标方法。
     */
    public static Method getMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return requireMethod(clazz, name, parameterTypes);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 调用方法。
     * <p>
     * 由于单个方法只能有一个可变参数，无法实现在一个方法内既查找目标方法又调用目标方法，因此我们需要先通过
     * {@link #requireMethod(Class, String, Class[])} 或 {@link #getMethod(Class, String, Class[])} 方法获取Method后再使用
     * 本方法。
     * <p>
     * 本方法默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param target     目标对象，对于静态方法可传入 {@code null} 。
     * @param method     目标方法。
     * @param parameters 参数列表。
     * @throws InvocationTargetException 目标方法执行期间出现异常。
     * @throws IllegalAccessException    目标方法不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static void callWithUnsafe(Object target, Method method, Object... parameters)
            throws InvocationTargetException, IllegalAccessException {
        // 如果这是一个静态方法，则将目标对象置为 `NULL` 。
        Object actualTarget = Modifier.isStatic(method.getModifiers()) ? null : target;
        method.invoke(actualTarget, parameters);
    }

    /**
     * 调用方法。
     * <p>
     * 由于单个方法只能有一个可变参数，无法实现在一个方法内既查找目标方法又调用目标方法，因此我们需要先通过
     * {@link #requireMethod(Class, String, Class[])} 或 {@link #getMethod(Class, String, Class[])} 方法获取Method后再使用
     * 本方法。
     * <p>
     * 本方法不会抛出异常，但如果目标方法不存在或出现其他错误则返回空值。
     *
     * @param target     目标对象，对于静态方法可传入 {@code null} 。
     * @param method     目标方法。
     * @param parameters 参数列表。
     * @return `true` 表示调用成功， `false` 表示调用失败，有异常出现。
     */
    public static boolean callWith(Object target, Method method, Object... parameters) {
        try {
            callWithUnsafe(target, method, parameters);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 调用方法并获取返回值。
     * <p>
     * 由于单个方法只能有一个可变参数，无法实现在一个方法内既查找目标方法又调用目标方法，因此我们需要先通过
     * {@link #requireMethod(Class, String, Class[])} 或 {@link #getMethod(Class, String, Class[])} 方法获取Method后再使用
     * 本方法。
     * <p>
     * 本方法会将目标方法的返回值转换为泛型所指定的类型并返回，如果目标方法返回空值则本方法也返回空值。
     * <p>
     * 本方法默认不处理异常，遇到异常将会传递给调用者。
     *
     * @param <T>        返回值类型。
     * @param target     目标对象，对于静态方法可传入 {@code null} 。
     * @param method     目标方法。
     * @param parameters 参数列表。
     * @throws InvocationTargetException 目标方法执行期间出现异常。
     * @throws IllegalAccessException    目标方法不可访问，由于本方法已经解除了访问限制，通常不会出现该异常。
     */
    public static <T> T callWithForUnsafe(Object target, Method method, Object... parameters)
            throws InvocationTargetException, IllegalAccessException {
        // 如果这是一个静态方法，则将目标对象置为 `NULL` 。
        Object actualTarget = Modifier.isStatic(method.getModifiers()) ? null : target;
        Object result = method.invoke(actualTarget, parameters);
        return (T) result;
    }

    /**
     * 调用方法并获取返回值。
     * <p>
     * 由于单个方法只能有一个可变参数，无法实现在一个方法内既查找目标方法又调用目标方法，因此我们需要先通过
     * {@link #requireMethod(Class, String, Class[])} 或 {@link #getMethod(Class, String, Class[])} 方法获取Method后再使用
     * 本方法。
     * <p>
     * 本方法会将目标方法的返回值转换为泛型所指定的类型并返回，如果目标方法返回空值则本方法也返回空值。
     * <p>
     * 本方法不会抛出异常，但如果目标方法不存在或出现其他错误则返回空值。
     *
     * @param <T>        返回值类型。
     * @param target     目标对象，对于静态方法可传入 {@code null} 。
     * @param method     目标方法。
     * @param parameters 参数列表。
     */
    public static <T> T callWithFor(Object target, Method method, Object... parameters) {
        try {
            return callWithForUnsafe(target, method, parameters);
        } catch (Exception e) {
            return null;
        }
    }
}
