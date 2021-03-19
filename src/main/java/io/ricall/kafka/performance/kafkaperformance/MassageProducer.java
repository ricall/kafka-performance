package io.ricall.kafka.performance.kafkaperformance;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@SpringBootApplication
public class MassageProducer {

    public static void main(String[] args) {
        SpringApplication.run(MassageProducer.class, args);
    }

    @Bean
    public CommandLineRunner onApplicationStartup(SendService sendService) {
        return args -> IntStream.rangeClosed(1, 10_000)
                .forEach(i -> sendService.sendMessage(i, String.format("Message number %d", i)));
    }

    private Sinks.Many<Message> messageSink = Sinks.many().multicast().onBackpressureBuffer();

    @Bean
    public Supplier<Flux<Message>> message() {
        return () -> messageSink.asFlux();
    }

    @Bean
    public SendService2 sendService2() {
        return new SendService2(messageSink);
    }

}

@Slf4j
@RequiredArgsConstructor
class SendService2 {
    private final Sinks.Many<Message> messageSink;

    public void sendMessage(int key, String value) {
        sendMessage(String.format("%06d", key), value);
    }

    public void sendMessage(String key, String value) {
        log.info("Sending [{}] {}", key, value);
        val message = MessageBuilder.withPayload(value)
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .build();

        messageSink.tryEmitNext(message);
    }
}

@Slf4j
@Component
@RequiredArgsConstructor
class SendService {

    private final StreamBridge streamBridge;

    public void sendMessage(int key, String value) {
        sendMessage(String.format("%06d", key), value);
    }

    public void sendMessage(String key, String value) {
        log.info("Sending [{}] {}", key, value);
        val message = MessageBuilder.withPayload(value)
                .setHeader(KafkaHeaders.MESSAGE_KEY, key)
                .build();

        streamBridge.send("message-out-0", message);
    }

}

@Slf4j
@RestController
@RequiredArgsConstructor
class SimpleRestController {

    private final SendService2 sender;

    @PostMapping("/bridge")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void handler(@RequestBody final String body) {
        log.info("Request body = {}", body);
        String[] split = body.split(";");

        sender.sendMessage(split[0], split[1]);
    }

}