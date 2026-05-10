package com.study.rocketmq.stage02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 单向发送示例：消息写入 socket 缓冲区后立即返回，不等待任何确认。
 *
 * <p>特点：
 * <ul>
 *   <li>吞吐最高，无任何等待</li>
 *   <li>消息可能静默丢失（网络断开、Broker 拒绝等均不可知）</li>
 *   <li>绝不用于涉及业务状态的场景</li>
 * </ul>
 *
 * <p>适用场景：非关键监控心跳、低价值埋点。
 */
public final class OnewayProducerExample {

    private static final Logger log = LoggerFactory.getLogger(OnewayProducerExample.class);

    private OnewayProducerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        String endpoint = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
            .setEndpoints(endpoint)
            .build();

        Producer producer = provider.newProducerBuilder()
            .setTopics(topic)
            .setClientConfiguration(configuration)
            .build();

        for (int i = 1; i <= 5; i++) {
            Message message = provider.newMessageBuilder()
                .setTopic(topic)
                .setTag("OnewayDemo")
                .setKeys("oneway-" + i)
                .setBody(("单向消息 #" + i).getBytes(StandardCharsets.UTF_8))
                .build();

            // 5.x gRPC 客户端：通过 sendAsync 忽略回调来模拟单向语义
            producer.sendAsync(message);
            log.info("[单向发送] 已提交第{}条（不等待确认）", i);
        }

        // 短暂等待让异步消息有机会发出
        Thread.sleep(2000);

        producer.close();
        log.info("单向发送示例完成");
    }
}
