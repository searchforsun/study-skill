package com.study.rocketmq.stage02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * PushConsumer 示例：SDK 管理拉取循环，业务只需实现 {@code MessageListener}。
 *
 * <p>注册监听器后，SDK 持续拉取并回调；返回 {@link ConsumeResult#SUCCESS} 即 ACK。
 * 通过系统属性 {@code rmq.consumer.group} 设置消费者组，同组多实例会自动负载均衡。
 *
 * <p>运行：保持本进程运行，然后打开另一个终端运行
 * {@link SyncProducerExample} 或 {@link AsyncProducerExample} 发送消息，观察本进程收到消息。
 */
public final class PushConsumerExample {

    private static final Logger log = LoggerFactory.getLogger(PushConsumerExample.class);

    private PushConsumerExample() {
    }

    @SuppressWarnings("InfiniteLoopStatement")
    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        String endpoint = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");
        String consumerGroup = System.getProperty("rmq.consumer.group", "Stage02ConsumerGroup");
        // Tag 过滤表达式："*" 匹配所有 Tag；也可改为 "SyncDemo" 只收同步发送的消息
        String tagExpression = System.getProperty("rmq.tag.expression", "*");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
            .setEndpoints(endpoint)
            .build();

        // PushConsumer：SDK 管理拉取循环，自动 ACK
        PushConsumer consumer = provider.newPushConsumerBuilder()
            .setClientConfiguration(configuration)
            .setConsumerGroup(consumerGroup)
            .setSubscriptionExpressions(
                Collections.singletonMap(topic,
                    new FilterExpression(tagExpression, FilterExpressionType.TAG)))
            .setMessageListener(messageView -> {
                // 此回调在 SDK 管理的线程中执行，不可长时间阻塞
                log.info("[PushConsumer] 收到消息 — msgId={}, key={}, tag={}, body={}",
                    messageView.getMessageId(),
                    messageView.getKeys(),
                    messageView.getTag().orElse("无"),
                    StandardCharsets.UTF_8.decode(messageView.getBody()));

                // 返回 SUCCESS → SDK 自动 ACK，Broker 推进 Offset
                // 返回 FAILURE → 触发重试，消息稍后重新投递
                return ConsumeResult.SUCCESS;
            })
            .build();

        log.info("PushConsumer 已启动，等待消息... (consumerGroup={}, topic={}, tag={})",
            consumerGroup, topic, tagExpression);
        log.info("在另一个终端运行 SyncProducerExample 发送消息，观察本进程输出。");

        // 保持进程运行
        Thread.sleep(Long.MAX_VALUE);
    }
}
