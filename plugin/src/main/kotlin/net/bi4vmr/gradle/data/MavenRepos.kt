package net.bi4vmr.gradle.data

import net.bi4vmr.gradle.entity.MavenRepo
import java.io.File

/**
 * 预设Maven仓库。
 *
 * @author bi4vmr@outlook.com
 * @since 1.0.0
 */
object MavenRepos {

    /* ----- 常用仓库 ----- */

    val JITPACK = MavenRepo(
        name = "Jitpack",
        host = "www.jitpack.io",
        port = 80,
        url = "https://www.jitpack.io/",
        description = "Jitpack仓库。"
    )


    /* ----- 国内镜像 ----- */

    val PUBLIC_TENCENT = MavenRepo(
        name = "Public-Tencent",
        host = "mirrors.cloud.tencent.com",
        port = 80,
        url = "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/",
        description = "腾讯云镜像（Maven中心仓库+Spring+Google+JCenter）。"
    )

    val PUBLIC_ALIYUN = MavenRepo(
        name = "Public-Aliyun",
        host = "maven.aliyun.com",
        port = 80,
        url = "https://maven.aliyun.com/repository/public/",
        description = "阿里云镜像（Maven中心仓库+JCenter）。"
    )

    val GOOGLE_ALIYUN = MavenRepo(
        name = "Google-Aliyun",
        host = "maven.aliyun.com",
        port = 80,
        url = "https://maven.aliyun.com/repository/google/",
        description = "阿里云镜像（Google）。"
    )

    val SPRING_ALIYUN = MavenRepo(
        name = "Spring-Aliyun",
        host = "maven.aliyun.com",
        port = 80,
        url = "https://maven.aliyun.com/repository/spring/",
        description = "阿里云镜像（Spring）。"
    )


    /* ----- 私有仓库 ----- */

    val PRIVATE_LAN = MavenRepo(
        name = "Private-LAN",
        host = "172.16.5.1",
        port = 8081,
        url = "http://172.16.5.1:8081/repository/maven-union/",
        description = "私有仓库（通过局域网连接）。"
    )

    // 主机名，局域网和TailScale VPN可用。
    val PRIVATE_HOSTNAME = MavenRepo(
        name = "Private-HostName",
        host = "BI4VMR-S1",
        port = 8081,
        url = "http://BI4VMR-S1:8081/repository/maven-union/",
        description = "私有仓库（通过主机名连接）。"
    )

    // DynV6域名。
    val PRIVATE_DYNV6 = MavenRepo(
        name = "Private-DynV6",
        host = "bi4vmr.dynv6.net",
        port = 8081,
        url = "http://bi4vmr.dynv6.net:8081/repository/maven-union/",
        description = "私有仓库（通过域名连接）。"
    )

    val PRIVATE_LOCAL = MavenRepo(
        name = "Private-LOCAL",
        host = "127.0.0.1",
        port = 8081,
        url = "http://127.0.0.1:8081/repository/maven-union/",
        description = "私有仓库（本机内置）。"
    )

    /**
     * Maven本地仓库。
     *
     * 该常量仅用于占位，建议使用内置函数 `mavenLocal()` 。
     */
    val PRIVATE_MAVEN_LOCAL = MavenRepo(
        name = "Maven-Local",
        host = "-",
        port = -1,
        url = "${System.getProperty("user.home")}${File.separator}.m2${File.separator}repository",
        description = "Maven本地仓库。"
    )
}
