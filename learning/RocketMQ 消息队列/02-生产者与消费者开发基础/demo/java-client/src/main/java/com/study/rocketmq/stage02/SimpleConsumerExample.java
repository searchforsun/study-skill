package com.study.rocketmq.stage02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.SimpleConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SimpleConsumer 示例：业务主动 {@code receive()} 拉取消息，处理成功后显式 {@code ack()}。
 *
 * <p>与 PushConsumer 的关键区别：
 * <ul>
 *   <li>拉取与确认由业务代码控制，不是回调</li>
 *   <li>{@code invisibleDuration} 内未 ACK 的消息会自动重新可见（等效重试）</li>
 *   <li>并发由业务自行管理，适合需要精细流量控制的场景</li>
 * </ul>
 *
 * <p>适用场景：消费速率受下游限流、需批量处理、或 ACK 前需做多步事务。
 */
public final class SimpleConsumerExample {

    private static final Logger log = LoggerFactory.getLogger(SimpleConsumerExample.class);

    private SimpleConsumerExample() {
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        String endpoint = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");
        String consumerGroup = System.getProperty("rmq.consumer.group", "Stage02SimpleConsumerGroup");
        // 注意：使用不同的 ConsumerGroup，避免与 PushConsumerExample 干扰

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
            .setEndpoints(endpoint)
            .build();

        SimpleConsumer consumer = provider.newSimpleConsumerBuilder()
            .setClientConfiguration(configuration)
            .setConsumerGroup(consumerGroup)
            .setSubscriptionExpressions(
                Collections.singletonMap(topic,
                    new FilterExpression("*", FilterExpressionType.TAG)))
            .setAwaitDuration(Duration.ofSeconds(3))     // 长轮询最大等待时间
            .build();

        log.info("SimpleConsumer 已启动，等待消息... (consumerGroup={}, topic={})",
            consumerGroup, topic);
        log.info("在另一个终端运行 SyncProducerExample 发送消息，观察本进程输出。");

        // 持续拉取循环
        while (true) {
            // receive(maxMessages, invisibleDuration)：
            //   - maxMessages：单次最多拉取条数
            //   - invisibleDuration：消息不可见时间（超时未 ACK 则重新投递）
            List<MessageView> messages = consumer.receive(10, Duration.ofSeconds(15));

            for (MessageView msg : messages) {
                try {
                    log.info("[SimpleConsumer] 收到消息 — msgId={}, key={}, tag={}, body={}",
                        msg.getMessageId(),
                        msg.getKeys(),
                        msg.getTag().orElse("无"),
                        StandardCharsets.UTF_8.decode(msg.getBody()));

                    // 模拟业务处理（实际项目中替换为真实逻辑）
                    Thread.sleep(100);

                    // 处理成功后显式 ACK —— 不调用则消息在 invisibleDuration 后重新投递
                    consumer.ack(msg);
                    log.info("[SimpleConsumer] ACK 成功 — msgId={}", msg.getMessageId());

                } catch (Exception e) {
                    log.error("[SimpleConsumer] 处理异常，不 ACK，消息将在 {} 后重新投递", "15s", e);
                    // 不调用 ack，消息在 invisibleDuration 后自动重新可见
                }
            }
        }
    }
}
