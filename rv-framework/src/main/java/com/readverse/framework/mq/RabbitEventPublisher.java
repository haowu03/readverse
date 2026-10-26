package com.readverse.framework.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rv.mq.enabled", havingValue = "true")
public class RabbitEventPublisher implements EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Override
    public void publish(DomainEvent event) {
        String routingKey = "event." + event.type().toLowerCase();
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, routingKey, event);
        log.info("Published MQ event {} routingKey={}", event.eventId(), routingKey);
    }
}
