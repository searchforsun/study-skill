package com.study.flink.stage01;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.util.Collector;

import java.time.Duration;

/**
 * 官方风格的 Socket + 滚动处理时间窗口词频（见 DataStream 文档示例思路）。
 *
 * <p>需先在另一终端监听端口并向 socket 发送文本（Windows 可用 WSL 或 ncat）。</p>
 */
public final class SocketWindowWordCount {

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("用法: SocketWindowWordCount <主机> <端口>");
            System.err.println("例: SocketWindowWordCount localhost 9999");
            System.exit(1);
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStream<String> text = env.socketTextStream(host, port);

        DataStream<Tuple2<String, Integer>> counts =
                text.flatMap(new Splitter())
                        .keyBy(t -> t.f0)
                        .window(TumblingProcessingTimeWindows.of(Duration.ofSeconds(5)))
                        .sum(1);

        counts.print();

        env.execute("socket-window-word-count");
    }

    public static final class Splitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
        @Override
        public void flatMap(String sentence, Collector<Tuple2<String, Integer>> out) {
            for (String word : sentence.split(" ")) {
                out.collect(Tuple2.of(word, 1));
            }
        }
    }
}
