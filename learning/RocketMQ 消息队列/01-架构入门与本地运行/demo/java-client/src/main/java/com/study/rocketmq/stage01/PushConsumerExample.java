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
 * PushConsumer：订阅 TestTopic 下全部 Tag（*），收到消息后打印 messageId。
 * 运行前请先创建 Topic，并确保 Proxy 已启动。
 */
public final class PushConsumerExample {

    private static final Logger log = LoggerFactory.getLogger(PushConsumerExample.class);

    private PushConsumerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        String endpoints = System.getProperty("rocketmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rocketmq.topic", "TestTopic");
        String consumerGroup = System.getProperty("rocketmq.consumerGroup", "DemoConsumerGroup");

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration clientConfiguration =
                ClientConfiguration.newBuilder().setEndpoints(endpoints).build();

        FilterExpression filterExpression =
                new FilterExpression("*", FilterExpressionType.TAG);

        CountDownLatch latch = new CountDownLatch(1);

        try (PushConsumer consumer = provider.newPushConsumerBuilder()
                .setClientConfiguration(clientConfiguration)
                .setConsumerGroup(consumerGroup)
                .setSubscriptionExpressions(Collections.singletonMap(topic, filterExpression))
                .setMessageListener(messageView -> {
                    ByteBuffer buf = messageView.getBody().duplicate();
                    byte[] bytes = new byte[buf.remaining()];
                    buf.get(bytes);
                    log.info("收到消息 messageId={} body={}",
                            messageView.getMessageId(),
                            new String(bytes, StandardCharsets.UTF_8));
                    latch.countDown();
                    return ConsumeResult.SUCCESS;
                })
                .build()) {

            boolean ok = latch.await(60, TimeUnit.SECONDS);
            if (!ok) {
                log.warn("等待超时：60 秒内未收到消息，请确认 Topic 是否创建、是否已先运行 ProducerExample");
            }
        }
    }
}
