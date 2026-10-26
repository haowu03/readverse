package com.readverse.framework.mq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rv.mq.enabled", havingValue = "false", matchIfMissing = true)
public class LocalEventPublisher implements EventPublisher {

    private final ApplicationEventPublisher publisher;

    @Override
    public void publish(DomainEvent event) {
        log.debug("Publishing local event {} {}", event.type(), event.eventId());
        publisher.publishEvent(event);
    }
}
