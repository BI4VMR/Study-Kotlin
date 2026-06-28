package net.bi4vmr.tool.java.common.base;

import net.bi4vmr.tool.java.common.base.io.BaseIOUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 命令行工具。
 * <p>
 * 执行命令并获取输出消息或状态码。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
public class CLIUtil {

    /**
     * 执行命令并返回进程实例。
     * <p>
     * 本方法只能处理拆分后的命令数组，例如： {@code "ipconfig", "/all" } ，对于完整的命令语句（例如： {@code "ipconfig /all" } ），
     * 请使用 {@link #run(String, boolean)} 方法。
     *
     * @param command       命令数组。
     * @param redirectError 是否将错误输出重定向到标准输出。
     * @return 已启动的子进程。
     * @throws IOException 进程启动失败。
     */
    public static Process run(String[] command, boolean redirectError) throws IOException {
        return new ProcessBuilder(command)
                .redirectErrorStream(redirectError)
                .start();
    }

    /**
     * 执行命令并返回进程实例。
     * <p>
     * 简化方法，将错误输出合并到标准输出，其他行为详见 {@link #run(String[], boolean)} 。
     *
     * @param command 命令数组。
     * @return 已启动的子进程。
     * @throws IOException 进程启动失败。
     */
    public static Process run(String[] command) throws IOException {
        return run(command, true);
    }

    /**
     * 执行命令并返回进程实例。
     * <p>
     * 本方法将对完整的命令语句（例如： {@code "ipconfig /all" } ）进行拆分，再传递给 {@link ProcessBuilder } 执行。
     *
     * @param command       命令语句。
     * @param redirectError 是否将错误输出重定向到标准输出。
     * @return 已启动的子进程。
     * @throws IOException 进程启动失败。
     */
    public static Process run(String command, boolean redirectError) throws IOException {
        return run(parseCommand(command), redirectError);
    }

    /**
     * 执行命令并返回进程实例。
     * <p>
     * 简化方法，将错误输出合并到标准输出，其他行为详见 {@link #run(String, boolean)} 。
     *
     * @param command 命令语句。
     * @return 已启动的子进程。
     * @throws IOException 进程启动失败。
     */
    public static Process run(String command) throws IOException {
        return run(command, true);
    }

    /**
     * 执行命令并返回状态码。
     * <p>
     * 本方法只能处理拆分后的命令数组，例如： {@code "ipconfig", "/all" } ，对于完整的命令语句（例如： {@code "ipconfig /all" } ），
     * 请使用 {@link #runForStatus(String)} 方法。
     *
     * @param command 命令数组。
     * @return 状态码。若出现异常则返回 {@code -1}。
     * @see #runForStatus(String)
     */
    public static int runForStatus(String[] command) {
        int status = -1;

        try {
            Process process = new ProcessBuilder(command)
                    // 不关心输出内容，将它们丢弃，防止占用内存与死锁。
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            status = process.waitFor();
        } catch (Exception e) {
            System.err.println("CLIUtil: Failed to run command, Reason:[" + e.getMessage() + "]");
        }

        return status;
    }

    /**
     * 执行命令并返回状态码。
     * <p>
     * 本方法将对完整的命令语句（例如： {@code "ipconfig /all" } ）进行拆分，再传递给 {@link ProcessBuilder } 执行。
     *
     * @param command 命令语句。
     * @return 状态码。若出现异常则返回 {@code -1}。
     */
    public static int runForStatus(String command) {
        return runForStatus(parseCommand(command));
    }

    /**
     * 判断命令是否执行成功。
     * <p>
     * 基于通用语义进行判断：状态码为 {@code 0} 时返回 {@code true} ，否则返回 {@code false} 。
     *
     * @param status 状态码。
     * @return 执行结果。
     */
    public static boolean isSuccess(int status) {
        return status == 0;
    }

    public static List<String> runForLinesUnsafe(String[] command, boolean redirectError, String charset) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .redirectErrorStream(redirectError)
                .start();
        List<String> result = BaseIOUtil.readAsLinesUnsafe(process.getInputStream(), selectCharset(charset));
        process.destroy();
        process.waitFor();
        return result;
    }

    public static List<String> runForLinesUnsafe(String[] command) throws IOException, InterruptedException {
        return runForLinesUnsafe(command, true, null);
    }

    public static List<String> runForLinesUnsafe(String command) throws IOException, InterruptedException {
        return runForLinesUnsafe(parseCommand(command), true, null);
    }

    public static List<String> runForLines(String[] command, boolean redirectError, String charset) {
        try {
            return runForLinesUnsafe(command, redirectError, charset);
        } catch (Exception e) {
            System.err.println("CLIUtil: Failed to run command, Reason:[" + e.getMessage() + "]");
            return new ArrayList<>();
        }
    }

    public static List<String> runForLines(String[] command) {
        return runForLines(command, true, null);
    }

    public static List<String> runForLines(String command) {
        return runForLines(parseCommand(command), true, null);
    }

    /**
     * 执行命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据为文本。
     * <p>
     * 本方法只能处理拆分后的命令数组，例如： {@code "ipconfig", "/all" } ，对于完整的命令语句（例如： {@code "ipconfig /all" } ），
     * 请使用 {@link #runForText(String, String)} 方法。
     * <p>
     * 解析输出文本时字符集的优先级为：参数指定 > JVM编码 > 默认值(UTF-8)。
     *
     * @param command 命令数组。
     * @param charset 字符集名称。空值表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空字符串。
     */
    public static String runForText(String[] command, String charset) {
        String text = "";
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            text = BaseIOUtil.readAsText(process.getInputStream(), selectCharset(charset));
            process.waitFor();
        } catch (Exception e) {
            System.err.println("CLIUtil: Failed to run command, Reason:[" + e.getMessage() + "]");
        }

        return text;
    }

    /**
     * 执行命令并获取文本消息。
     * <p>
     * 简化方法，自动选择字符集，其他行为详见 {@link #runForText(String[], String)} 。
     *
     * @param command 命令数组。
     * @return 命令输出文本。若出现异常则返回空字符串。
     */
    public static String runForText(String[] command) {
        return runForText(command, null);
    }

    /**
     * 执行命令并获取文本消息。
     * <p>
     * 执行命令并阻塞当前线程，将错误输出与标准输出合并，然后读取所有输出数据为文本。
     * <p>
     * 本方法将对完整的命令语句（例如： {@code "ipconfig /all" } ）进行拆分，再传递给 {@link ProcessBuilder } 执行。
     * <p>
     * 解析输出文本时字符集的优先级为：参数指定 > JVM编码 > 默认值(UTF-8)。
     *
     * @param command 命令语句。
     * @param charset 字符集名称。空值表示自动侦测。
     * @return 命令输出文本。若出现异常则返回空字符串。
     */
    public static String runForText(String command, String charset) {
        return runForText(parseCommand(command), charset);
    }

    /**
     * 执行命令并获取文本消息。
     * <p>
     * 简化方法，自动选择字符集，其他行为详见 {@link #runForText(String, String)} 。
     *
     * @param command 命令语句。
     * @return 命令输出文本。若出现异常则返回空字符串。
     */
    public static String runForText(String command) {
        return runForText(command, null);
    }

    /**
     * 将完整的命令语句拆分为数组。
     * <p>
     * {@link ProcessBuilder} 只能接受已按空格拆分的命令数组，如果用户输入的是整行命令，需要先通过本方法进行拆分。
     *
     * @param command 命令语句。
     * @return 拆分后的命令数组。
     */
    public static String[] parseCommand(String command) {
        // 前置校验：输入无效时返回空命令。
        if (command == null || command.isBlank()) {
            return new String[]{""};
        }

        List<String> result = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        boolean inQuotes = false;
        // 遍历字符，拆分命令语句。
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            // 如果当前字符是引号，将标志位取反，并跳过后续的拆分操作。
            if (c == '"') {
                inQuotes = !inQuotes;
                continue;
            }

            // 如果当前字符是空格且不在引号内，则将暂存区的所有字符视作单词并放入结果列表。
            if (Character.isWhitespace(c) && !inQuotes) {
                // 若单词之间存在多个空格，此处需要跳过，防止产生多个内容为空的数组元素。
                if (word.isEmpty()) {
                    continue;
                }

                result.add(word.toString());
                word.setLength(0);
                continue;
            }

            // 如果当前字符是普通字符，则放入暂存区。
            word.append(c);
        }

        if (inQuotes) {
            System.err.println("CLIUtil: Unclosed quote in command: [" + command + "]!");
            return new String[]{""};
        }

        // 上述循环只处理到最后一个空格之前的字符，此处需要将剩余字符放入结果列表。
        if (!word.isEmpty()) {
            result.add(word.toString());
        }

        return result.toArray(new String[0]);
    }

    /**
     * 终止进程。
     * <p>
     * 首先向进程发送终止信号，阻塞当前线程并等待最多 {@code timeout} 毫秒，如果该时长内目标进程仍未终止，则强制杀死进程。
     *
     * @param process 目标进程。
     * @param timeout 最大等待时长。
     * @return {@code true} 表示进程在最大等待时长内终止， {@code false} 表示进程在最大等待时长后仍未终止。
     */
    public static boolean stopProcess(Process process, long timeout) {
        if (process == null) {
            return true;
        }

        try {
            // 首先尝试正常终止进程
            process.destroy();

            // 等待至参数指定的时长
            boolean exited = process.waitFor(timeout, TimeUnit.MILLISECONDS);

            // 常规终止信号未响应时，发送强制终止信号。
            if (!exited) {
                System.err.println("CLIUtil: Failed to stop process [" + process.pid() + "], kill it!");
                process.destroyForcibly();
            }

            return exited;
        } catch (InterruptedException e) {
            System.err.println("CLIUtil: Interrupted while waiting for process destroyed!");
            Thread.currentThread().interrupt();
            // 中断处理：什么都不做，静默结束操作。
            return false;
        }
    }

    /**
     * 终止进程。
     * <p>
     * 简化方法，最大等待3秒，其他行为详见 {@link #stopProcess(Process, long)} 。
     *
     * @param process 目标进程。
     * @return {@code true} 表示进程在最大等待时长内终止， {@code false} 表示进程在最大等待时长后仍未终止。
     */
    public static boolean stopProcess(Process process) {
        return stopProcess(process, 3 * 1000L);
    }

    /**
     * 选择解析输入流时所用的字符集。
     * <p>
     * 优先使用用户指定的字符集，否则侦测操作系统声明的控制台字符集；若无法侦测默认使用 {@link StandardCharsets#UTF_8} 。
     *
     * @param userset 用户指定的字符集，空值表示未指定。
     * @return 最终使用的字符集。
     */
    private static Charset selectCharset(String userset) {
        Charset charset = StandardCharsets.UTF_8;

        try {
            if (userset != null && !userset.isBlank()) {
                /* 存在用户输入，则尝试解析该字符集并进行使用。 */
                charset = Charset.forName(userset);
            } else {
                /* 不存在用户输入，尝试解析系统声明的控制台字符集。 */
                // Java 17引入的平台编码属性
                String nativeEncoding = System.getProperty("native.encoding");
                if (nativeEncoding != null && !nativeEncoding.isBlank()) {
                    charset = Charset.forName(nativeEncoding);
                } else {
                    // 如果当前环境低于Java 17，则使用SUN提供的隐藏属性。
                    String internalEncoding = System.getProperty("sun.jnu.encoding");
                    charset = Charset.forName(internalEncoding);
                }
            }
        } catch (Exception e) {
            // 解析失败，保持默认值。
            System.err.println("CLIUtil: Invalid charset [" + userset + "], fallback to [UTF-8]!");
        }

        return charset;
    }
}
