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

package io.ricall.kafka.performace.kafkaperformance.listener;

import io.ricall.kafka.performace.kafkaperformance.repository.MessageRespository;
import io.ricall.kafka.performace.kafkaperformance.entity.MessageEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageListener {

    private final MessageRespository repository;

    private final AtomicInteger counter = new AtomicInteger();

    @KafkaListener(topics = "dev.message", groupId = "message-listener")
    public void onMessage(
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
            @Payload String message,
            Acknowledgment ack) throws Exception {
        log.info("Received message: {} - {}", key, message);

        final MessageEntity messageEntity = MessageEntity.builder()
                .source("kafka-consumer")
                .key(key)
                .message(message)
                .build();
        repository.save(messageEntity);

        int index = Integer.parseInt(key);
        Thread.sleep((10 - (index % 10)) * 100);

        log.info("Processed {} messages", counter.incrementAndGet());
        ack.acknowledge();
    }

}
