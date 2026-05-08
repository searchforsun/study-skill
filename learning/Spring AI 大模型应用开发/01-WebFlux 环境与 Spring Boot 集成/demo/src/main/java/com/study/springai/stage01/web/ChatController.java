package com.study.springai.stage01.web;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 阶段 1：演示在 WebFlux 中调用「阻塞型」{@link ChatModel}。
 * <p>
 * ChatModel#call 往往会阻塞等待远端 HTTP；必须在 boundedElastic 等调度器上执行，
 * 避免占用 Netty 事件循环线程。
 */
@RestController
@RequestMapping(path = "/api", produces = MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8")
public class ChatController {

    private final ChatModel chatModel;

    public ChatController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public Mono<String> chat(
            @RequestParam(value = "message", defaultValue = "用一句话介绍 Spring AI。") String message) {
        return Mono.fromCallable(() -> chatModel.call(new Prompt(new UserMessage(message)))
                        .getResult()
                        .getOutput()
                        .getText())
                .subscribeOn(Schedulers.boundedElastic());
    }
}
