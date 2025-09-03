package net.bi4vmr.study.oop.base;

/**
 * TODO 添加描述
 *
 * @author BI4VMR
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
