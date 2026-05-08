package com.study.rocketmq.stage01;

import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RocketMQ 5.x 统一客户端示例：通过 Proxy Endpoint 发送普通消息。
 * 逻辑与官网 Docker 快速入门一致：https://rocketmq.apache.org/docs/quickStart/02quickstartWithDocker/
 */
public final class ProducerExample {

    private static final Logger log = LoggerFactory.getLogger(ProducerExample.class);

    private ProducerExample() {
    }

    public static void main(String[] args) throws Exception {
        // 默认连接本机 docker-compose 映射的 Proxy（参见 demo/docker-compose.yml）
        String endpoint = System.getProperty("rocketmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rocketmq.topic", "TestTopic");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration =
                ClientConfiguration.newBuilder().setEndpoints(endpoint).build();

        try (Producer producer = provider.newProducerBuilder()
                .setTopics(topic)
                .setClientConfiguration(configuration)
                .build()) {

            Message message = provider.newMessageBuilder()
                    .setTopic(topic)
                    .setKeys("demo-key")
                    .setTag("demoTag")
                    .setBody("hello RocketMQ stage01".getBytes())
                    .build();

            SendReceipt receipt = producer.send(message);
            log.info("发送成功 messageId={}", receipt.getMessageId());
        }
    }
}
