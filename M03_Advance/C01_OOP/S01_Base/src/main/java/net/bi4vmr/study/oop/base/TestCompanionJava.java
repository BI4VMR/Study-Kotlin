package net.bi4vmr.study.oop.base;

/**
 * 测试代码：伴生对象。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class TestCompanionJava {

    public static void main(String[] args) {
        exam();
    }

    static void exam() {
        // 变量"x"未添加"@JvmStatic"注解时，此时只能通过Companion类访问。
        TestCompanion.Companion.getX();

        // 变量"x"已添加"@JvmStatic"注解时，此时可以直接访问静态方法。
        TestCompanion.getX();
    }
}
