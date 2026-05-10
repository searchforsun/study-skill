package com.study.rocketmq.stage02;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.apache.rocketmq.client.apis.producer.SendReceipt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 异步发送示例：调用线程立即返回，结果通过回调通知。
 *
 * <p>关键约束：
 * <ol>
 *   <li>回调在 IO 线程执行，不可做重操作（DB 写入、RPC 调用需提交到独立线程池）</li>
 *   <li>必须等待所有回调完成再关闭 Producer —— 此处用 {@link CountDownLatch} 汇聚</li>
 * </ol>
 *
 * <p>适用场景：批量日志上报、埋点数据写入等高吞吐场景，对单条延迟不敏感。
 */
public final class AsyncProducerExample {

    private static final Logger log = LoggerFactory.getLogger(AsyncProducerExample.class);

    private AsyncProducerExample() {
    }

    public static void main(String[] args) throws ClientException, IOException, InterruptedException {
        String endpoint = System.getProperty("rmq.endpoints", "localhost:8081");
        String topic = System.getProperty("rmq.topic", "TestTopic");
        int totalMessages = 10;

        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder()
            .setEndpoints(endpoint)
            .build();

        Producer producer = provider.newProducerBuilder()
            .setTopics(topic)
            .setClientConfiguration(configuration)
            .build();

        // CountDownLatch 确保所有异步回调完成后再退出
        CountDownLatch latch = new CountDownLatch(totalMessages);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 1; i <= totalMessages; i++) {
            Message message = provider.newMessageBuilder()
                .setTopic(topic)
                .setTag("AsyncDemo")
                .setKeys("async-" + i)
                .setBody(("异步消息 #" + i).getBytes(StandardCharsets.UTF_8))
                .build();

            // 异步发送 —— 立即返回，回调在 IO 线程执行
            producer.sendAsync(message).thenAccept(receipt -> {
                successCount.incrementAndGet();
                log.info("[异步发送] 成功 messageId={}", receipt.getMessageId());
            }).exceptionally(throwable -> {
                failCount.incrementAndGet();
                log.error("[异步发送] 失败", throwable);
                return null;
            }).thenRun(latch::countDown);  // 无论成败都释放 latch
        }

        log.info("已提交 {} 条异步发送，等待回调完成...", totalMessages);

        // 等待所有回调完成（最多等 30 秒）
        boolean allDone = latch.await(30, TimeUnit.SECONDS);
        log.info("异步发送结束 — 成功:{}, 失败:{}, 全部完成:{}",
            successCount.get(), failCount.get(), allDone);

        producer.close();
    }
}
