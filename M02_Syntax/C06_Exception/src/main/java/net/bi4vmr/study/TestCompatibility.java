package net.bi4vmr.study;

import java.io.IOException;
import java.net.SocketException;

/**
 * Java兼容性测试。
 *
 * @author BI4VMR@outlook.com
 * @since 1.0.0
 */
public class TestCompatibility {

    public static void main(String[] args) throws IOException, SocketException {
        TestExceptionKt.javaExceptionTest();
    }
}
