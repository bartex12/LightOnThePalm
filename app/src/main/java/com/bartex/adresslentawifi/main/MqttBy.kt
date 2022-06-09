package com.bartex.adresslentawifi.main

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import java.util.*
import java.util.concurrent.CompletableFuture

class MqttBy {
    //Создание клиента
    val client: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(UUID.randomUUID().toString())
        //.serverHost("mqtt.by")
        .serverHost("broker.hivemq.com")
        .serverPort(1883)
        .buildAsync()

    //Подключение к брокеру
    val connection: CompletableFuture<Mqtt3ConnAck> = client.connectWith()
        .simpleAuth()
        .username("bartex")
        //.password("eeoyuepr".toByteArray())  //mqtt.by
        .password("Kukamon@1".toByteArray()) //hivemq
        .applySimpleAuth()
        .send()

}

