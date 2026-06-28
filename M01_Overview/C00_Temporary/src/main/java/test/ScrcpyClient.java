package test;

import java.io.IOException;
import java.net.Socket;

/**
 * scrcpy Java 客户端入口，对应 C 客户端的 scrcpy.c。
 *
 * <p><b>使用步骤：</b>
 * <ol>
 *   <li>启动 scrcpy server（audio=false 以省去第二路连接）：
 *       <pre>adb shell CLASSPATH=/data/local/tmp/scrcpy-server.jar \
 *   app_process / com.genymobile.scrcpy.Server 2.7 \
 *   tunnel_forward=true video=true audio=false control=true \
 *   send_device_meta=true send_frame_meta=true send_dummy_byte=true \
 *   send_stream_meta=true</pre></li>
 *   <li>ADB 端口转发：{@code adb forward tcp:27183 localabstract:scrcpy}</li>
 *   <li>运行本程序（可选参数 host port）</li>
 * </ol>
 *
 * <p>连接顺序（tunnelForward=true 时服务端按 accept 顺序分配）：
 * <ol>
 *   <li>第 1 个连接 → 视频流 Socket</li>
 *   <li>第 2 个连接 → 控制 Socket（audio=false 时跳过音频）</li>
 * </ol>
 */
public class ScrcpyClient {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final int DEFAULT_PORT = 27183;

    public static void main(String[] args) throws IOException {
        String host = args.length > 0 ? args[0] : DEFAULT_HOST;
        int port = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_PORT;

        System.out.printf("[ScrcpyClient] Connecting to %s:%d ...%n", host, port);

        // 第 1 个连接：视频流
        Socket videoSocket = new Socket(host, port);
        videoSocket.setTcpNoDelay(true);
        System.out.println("[ScrcpyClient] Video socket connected.");

        // 第 2 个连接：控制（服务端以 accept 顺序分配，audio=false 时紧跟视频）
        Socket controlSocket = new Socket(host, port);
        controlSocket.setTcpNoDelay(true);
        System.out.println("[ScrcpyClient] Control socket connected.");

        ScreenDisplay display = new ScreenDisplay("Connecting...");
        InputController controller = new InputController(controlSocket.getOutputStream());
        display.setInputController(controller);

        VideoStream stream = new VideoStream(videoSocket.getInputStream(), display);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            stream.stop();
            try {
                videoSocket.close();
            } catch (IOException ignored) {
            }
            try {
                controlSocket.close();
            } catch (IOException ignored) {
            }
        }));

        try {
            Thread.sleep(2000L);

            stream.start();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            videoSocket.close();
            controlSocket.close();
        }

        System.out.println("[ScrcpyClient] Disconnected.");
    }
}
