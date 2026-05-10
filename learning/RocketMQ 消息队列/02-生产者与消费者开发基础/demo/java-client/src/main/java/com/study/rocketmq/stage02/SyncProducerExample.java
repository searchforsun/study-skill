package com.study.rocketmq.stage02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 同步发送示例：调用线程阻塞等待 Broker 确认，拿到 messageId 后才返回。
 *
 * <p>适用场景：订单状态通知、支付回调等必须确认消息已落盘的核心链路。
 * 运行前请确保 docker-compose 已启动且 Topic 已创建（见 demo/README.md）。
 */
public final class SyncProducerExample {

    private static final Logger log = LoggerFactory.getLogger(SyncProducerExample.class);

    private SyncProducerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException {
        String endpoint = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
            .setEndpoints(endpoint)
            .build();

        // 生产者实例是重量级对象，应用启动时创建一次，复用至进程退出
        Producer producer = provider.newProducerBuilder()
            .setTopics(topic)
            .setClientConfiguration(configuration)
            .build();

        // 发送多条消息，观察每条都返回不同的 messageId
        for (int i = 1; i <= 5; i++) {
            Message message = provider.newMessageBuilder()
                .setTopic(topic)
                .setTag("SyncDemo")                         // Tag 用于消费者过滤
                .setKeys("order-" + i)                      // Key：业务唯一标识，追踪与幂等
                .setBody(("同步消息 #" + i).getBytes(StandardCharsets.UTF_8))
                .build();

            // 同步发送 —— 调用线程阻塞直到 Broker 返回确认
            SendReceipt receipt = producer.send(message);
            log.info("[同步发送] 第{}条 → messageId={}", i, receipt.getMessageId());
        }

        producer.close();
        log.info("同步发送示例完成，生产者已关闭");
    }
}
