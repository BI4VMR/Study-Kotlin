package net.bi4vmr.study.oop.base;

/**
 * 示例代码：伴生对象。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class TestCompanionJava {

    public static void main(String[] args) {
        example09();
    }

    static void example09() {
        // 伴生对象中的变量未添加 `@JvmStatic` 注解时，Java只能通过Companion类的Getter方法访问。
        TestCompanion.Companion.getX();
        // 伴生对象中的方法未添加 `@JvmStatic` 注解时，Java只能通过Companion类访问。
        TestCompanion.Companion.avg(2, 6);

        // 伴生对象中的变量与方法已添加 `@JvmStatic` 注解时，Java可以直接访问静态方法。
        TestCompanion3.getX();
        TestCompanion3.avg(2, 6);

        // 伴生对象中的变量为 `const` 常量时，已经被编译器优化为静态常量，不能添加 `@JvmStatic` 注解，Java可以直接访问静态常量。
        final float PI = TestCompanion2.PI;
    }
}
