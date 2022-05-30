package com.bartex.adresslentawifi.main

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish
import java.util.*
import java.util.concurrent.CompletableFuture

class MqttBy {
    val client: Mqtt3AsyncClient
    val connection: CompletableFuture<Mqtt3ConnAck>
    init {
        //Создание клиента
        client = MqttClient.builder()
            .useMqttVersion3()
            .identifier(UUID.randomUUID().toString())
            .serverHost("mqtt.by")
            .serverPort(1883)
            .buildAsync()

        //Подключение к брокеру
        connection =client.connectWith()
            .simpleAuth()
            .username("bartex")
            .password("eeoyuepr".toByteArray())
            .applySimpleAuth()
            .send()
    }
}

