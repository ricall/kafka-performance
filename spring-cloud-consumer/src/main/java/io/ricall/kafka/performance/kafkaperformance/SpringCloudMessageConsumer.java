/*
 * Copyright (c) 2021 Richard Allwood
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package io.ricall.kafka.performance.kafkaperformance;

import io.ricall.kafka.performance.kafkaperformance.entity.MessageEntity;
import io.ricall.kafka.performance.kafkaperformance.repository.MessageRespository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@Slf4j
@SpringBootApplication
@RequiredArgsConstructor
public class SpringCloudMessageConsumer {

    private final MessageRespository repository;

    public static void main(String[] args) {
        SpringApplication.run(SpringCloudMessageConsumer.class, args);
    }

    @Bean
    public Function<Flux<Message<String>>, Mono<Void>> onMessage() {
        return message -> message
                .subscribeOn(Schedulers.newBoundedElastic(10, 100, "db-threads"))
                .flatMap(this::handleMessage)
                .doOnError(this::handleError)
                .then();
    }

    private final AtomicInteger counter = new AtomicInteger();

    private Mono<Message<String>> handleMessage(Message<String> message) {
        String key = (String) message.getHeaders().get(KafkaHeaders.RECEIVED_MESSAGE_KEY);
        String value = message.getPayload();
        log.info("Received Message: [{}] {}", key, value);

        final MessageEntity messageEntity = MessageEntity.builder()
                .source("kafka-consumer")
                .key(key)
                .message(value)
                .build();
        repository.save(messageEntity);

        int index = Integer.parseInt(key);
        log.info("Processed {} messages", counter.incrementAndGet());

        return Mono.just(message)
                .delayElement(Duration.ofMillis((10 - (index % 10)) * 100));
    }

    private void handleError(Throwable error) {
        log.error("Failed to get message", error);
    }

}

