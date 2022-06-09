package com.bartex.adresslentawifi.main

import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.message.connect.connack.Mqtt3ConnAck
import java.util.*
import java.util.concurrent.CompletableFuture

class MqttHive {
    //Создание клиента
    val client: Mqtt3AsyncClient = MqttClient.builder()
        .useMqttVersion3()
        .identifier(UUID.randomUUID().toString())
        .serverHost("broker.hivemq.com")
        .serverPort(1883)
        .buildAsync()
    //Подключение к брокеру
    val connection:CompletableFuture<Mqtt3ConnAck> = client.connectWith()
        .simpleAuth()
        .username("bartex")
        .password("Kukamon@1".toByteArray())
        .applySimpleAuth()
        .send()
    //это перенесено в mainActivity
//           .whenComplete { connAck: Mqtt3ConnAck?, throwable: Throwable? ->
//                if (throwable != null) {
//                    // handle failure
//                    Log.d(Constants.TAG, "MainActivity onCreate: throwable.conn = ${throwable.message} ")
//                } else {
//                    // setup subscribes or start publishing
//                    Log.d(Constants.TAG, "MainActivity onCreate: connAck = $connAck ")
//                    // так как подключение успешно - подписываемся на топик testtt
//                    client.subscribeWith()
//                        .topicFilter("testtt")
//                        .callback { publish ->
//                            val addressIp = String(publish.payloadAsBytes, Charsets.UTF_8)
//                            // Process the received message
//                            Log.d( TAG, "***MainActivity onCreate: received publish topic = ${publish.topic}" +
//                                    "\nreceived publish payloadAsBytes = $addressIp\n")
//                        }
//                        .send()
//                        .whenComplete { subAck, e ->
//                            if (e != null) {
//                                // Handle failure to subscribe
//                                Log.d(TAG, "MainActivity onCreate: throwable = ${e.message} ")
//                            } else {
//                                // Handle successful subscription, e.g. logging or incrementing a metric
//                                Log.d(TAG, "MainActivity onCreate: subAck = $subAck ")
//                            }
//                        }
//                    //так как подключение успешно - публикуем сообщение в топике test333
//                    client.publish(publishMessage)
//                        .whenComplete { publish, e ->
//                            if (e != null) {
//                                // handle failure to publish
//                                Log.d(TAG, "MainActivity onCreate: publish.e = ${e.message} ")
//                            } else {
//                                // Handle successful subscription, e.g. logging or incrementing a metric
//                                Log.d(TAG, "###MainActivity onCreate: publishMessage = " +
//                                            "${String(publish.payloadAsBytes, Charsets.UTF_8)} ")
//                            }
//                        }
//                }
//            }
}
    //Подключение к брокеру может быть и таким
// - см https://console.hivemq.cloud/clients/java-hivemq?uuid=adb26aada24a40fdbfabb0bc5cf6f9b9
// или  https://hivemq.github.io/hivemq-mqtt-client/docs/quick-start/
//         val connAck:CompletableFuture<Mqtt3ConnAck> = client.connectWith()
//            // here you can specify multiple properties described below
//            .cleanSession(true)
//             .noKeepAlive()
//             .simpleAuth()
//             .username("bartex")
//             .password("Kukamon@1".toByteArray())
//             .applySimpleAuth()
//            .send()

//создание публикации
//        publishMessage = Mqtt3Publish.builder()
//        .topic("test333")
//            .payload("hello world".toByteArray())
//            .qos(MqttQos.AT_LEAST_ONCE)
//            .build()