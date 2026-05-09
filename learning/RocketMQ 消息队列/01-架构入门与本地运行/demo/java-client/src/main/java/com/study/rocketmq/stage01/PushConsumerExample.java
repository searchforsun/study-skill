package com.study.rocketmq.stage01;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Push 消费示例：订阅全部 Tag（{@code *}），收到一条成功后结束进程（便于课堂演示）。
 * 若需长期驻留，可把 {@link CountDownLatch} 等待改为 {@code Thread.sleep(Long.MAX_VALUE)}。
 */
public final class PushConsumerExample {

    private static final Logger log = LoggerFactory.getLogger(PushConsumerExample.class);

    private PushConsumerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        String endpoints = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");
        String consumerGroup = System.getProperty("rmq.consumerGroup", "Stage01ConsumerGroup");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration = ClientConfiguration.newBuilder().setEndpoints(endpoints).build();

        FilterExpression filterExpression = new FilterExpression("*", FilterExpressionType.TAG);
        CountDownLatch latch = new CountDownLatch(1);

        PushConsumer pushConsumer = provider.newPushConsumerBuilder()
            .setClientConfiguration(clientConfiguration)
            .setConsumerGroup(consumerGroup)
            .setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
            .setMessageListener(messageView -> {
                ByteBuffer buf = messageView.getBody();
                String text = StandardCharsets.UTF_8.decode(buf.duplicate()).toString();
                log.info("收到消息 messageId={} bodyUtf8={}", messageView.getMessageId(), text);
                latch.countDown();
                return ConsumeResult.SUCCESS;
            })
            .build();

        // 演示场景：最多等 60 秒；课堂常驻可改为无限 sleep
        boolean ok = latch.await(60, TimeUnit.SECONDS);
        if (!ok) {
            log.warn("超时未收到消息，请确认已先启动 Producer 或 Topic/订阅是否正确");
        }
        pushConsumer.close();
    }
}
