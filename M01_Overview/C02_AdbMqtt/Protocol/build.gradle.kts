plugins {
    alias(libKotlin.plugins.core)
    alias(libKotlin.plugins.serial)
}

dependencies {
    implementation(libKotlin.standardlib)
    api(libKotlin.ktx.coroutines.core)
    api(libKotlin.ktx.serial.json)
    // MQTT客户端
    api(libJava.hivemq.mqtt)
}
