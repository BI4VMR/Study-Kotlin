package net.bi4vmr.study.adbmqtt

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import kotlinx.coroutines.future.await

/** 创建异步客户端，启用自动重连。 */
fun newMqttClient(
    clientId: String,
    host: String,
    port: Int,
    willTopic: String? = null,
    willPayload: String? = null
): Mqtt5AsyncClient {
    val builder = Mqtt5Client.builder()
        .identifier(clientId)
        .serverHost(host)
        .serverPort(port)
        .automaticReconnectWithDefaultConfig()
    if (willTopic != null && willPayload != null) {
        builder.willPublish()
            .topic(willTopic)
            .qos(MqttQos.AT_LEAST_ONCE)
            .retain(true)
            .payload(willPayload.toByteArray())
            .applyWillPublish()
    }
    return builder.buildAsync()
}

/** 连接Broker；cleanStart=false时由Broker保留会话与订阅，自动重连后无需重新订阅。 */
suspend fun Mqtt5AsyncClient.connectSuspend() {
    connectWith()
        .cleanStart(false)
        .sessionExpiryInterval(3600)
        .send()
        .await()
}

suspend fun Mqtt5AsyncClient.publishText(topic: String, text: String, retain: Boolean = false) {
    publishWith()
        .topic(topic)
        .qos(MqttQos.AT_LEAST_ONCE)
        .retain(retain)
        .payload(text.toByteArray())
        .send()
        .await()
}

suspend fun Mqtt5AsyncClient.subscribeText(
    filter: String,
    callback: (topic: String, payload: String) -> Unit
) {
    subscribeWith()
        .topicFilter(filter)
        .qos(MqttQos.AT_LEAST_ONCE)
        .callback { p: Mqtt5Publish -> callback(p.topic.toString(), String(p.payloadAsBytes)) }
        .send()
        .await()
}
