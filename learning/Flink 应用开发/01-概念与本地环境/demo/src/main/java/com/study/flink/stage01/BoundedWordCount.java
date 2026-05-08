package com.study.flink.stage01;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

/**
 * 有界词频统计：不依赖 nc/socket，适合第一步跑通环境与惰性执行链路。
 *
 * <p>数据来自内存中的若干句子，作业结束后进程退出。</p>
 */
public final class BoundedWordCount {

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        // 本地调试时可观察并行度；与集群默认值行为不同，详见官方 ExecutionEnvironment 说明
        env.setParallelism(2);

        DataStream<String> lines =
                env.fromElements(
                        "apache flink streams",
                        "flink unified batch and stream",
                        "learn flink step by step");

        DataStream<Tuple2<String, Integer>> counts =
                lines.flatMap(new Tokenizer()).keyBy(t -> t.f0).sum(1);

        counts.print();

        env.execute("bounded-word-count");
    }

    /** 将一行文本切分为 (word, 1)。 */
    public static final class Tokenizer implements FlatMapFunction<String, Tuple2<String, Integer>> {
        @Override
        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
            for (String token : value.toLowerCase().split("\\W+")) {
                if (!token.isEmpty()) {
                    out.collect(Tuple2.of(token, 1));
                }
            }
        }
    }
}
