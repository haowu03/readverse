package com.readverse.framework.mq;

public interface EventPublisher {

    void publish(DomainEvent event);
}
