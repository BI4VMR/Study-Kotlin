package net.bi4vmr.tool.java.external.adb;

/**
 * ADB服务监听器。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public interface ADBServiceListener {

    /**
     * ADB服务就绪事件。
     * <p>
     * 同步执行 {@link ADBController#init()} 方法时不需要关心本回调；异步执行 {@link ADBController#init()} 方法时可以通过本回调
     * 监听初始化状态。
     */
    default void onInitDone() {
        // 默认不作响应
    }

    /**
     * ADB服务终止事件。
     */
    default void onTerminate() {
        // 默认不作响应
    }
}
