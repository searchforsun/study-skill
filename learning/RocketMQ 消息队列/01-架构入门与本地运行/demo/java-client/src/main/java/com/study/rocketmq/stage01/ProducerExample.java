package com.study.rocketmq.stage01;

import java.io.IOException;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通过 5.x 推荐的 {@code rocketmq-client-java} 向 Proxy 发送普通消息。
 * 运行前请按 demo/README.md 创建 Topic，并确保 Proxy 端口映射到本机。
 */
public final class ProducerExample {

    private static final Logger log = LoggerFactory.getLogger(ProducerExample.class);

    private ProducerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException {
        // 官方示例使用 Proxy 端口；本机 compose 默认映射 8081
        String endpoint = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder().setEndpoints(endpoint).build();

        Producer producer = provider.newProducerBuilder()
            .setTopics(topic)
            .setClientConfiguration(configuration)
            .build();

        Message message = provider.newMessageBuilder()
            .setTopic(topic)
            .setKeys("stage01-key")
            .setTag("stage01-tag")
            .setBody("你好，RocketMQ（阶段01 示例）".getBytes(java.nio.charset.StandardCharsets.UTF_8))
            .build();

        try {
            SendReceipt receipt = producer.send(message);
            log.info("发送成功 messageId={}", receipt.getMessageId());
        } catch (ClientException e) {
            log.error("发送失败（请检查 Topic、Proxy、消息类型是否与 Topic 声明一致）", e);
            throw e;
        } finally {
            producer.close();
        }
    }
}
