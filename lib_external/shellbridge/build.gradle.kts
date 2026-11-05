plugins {
    alias(libKotlin.plugins.core)

    alias(privateLibJava.plugins.java.version)
}

dependencies {
    /* 外部组件 */
    implementation("org.jline:jline-terminal:3.26.3")
    implementation("org.jline:jline-terminal-jni:3.26.3")
}

javaVersionConfig {
    jdkVersion = JavaVersion.VERSION_1_9
}

// 打包包含全部运行时依赖的可执行 JAR ，可通过 `java -jar` 独立运行。
val executableJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Assembles an executable fat jar."

    archiveFileName.set("shellbridge.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes("Main-Class" to "net.bi4vmr.tool.kotlin.util.shell.transfer.ShellBridge")
    }

    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get()
            .filter { it.name.endsWith(".jar") }
            .map { zipTree(it) }
    }) {
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/versions/9/module-info.class")
    }
}
