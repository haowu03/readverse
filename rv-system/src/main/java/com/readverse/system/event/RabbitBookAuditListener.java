package com.readverse.system.event;

import com.readverse.framework.mq.DomainEvent;
import com.readverse.framework.mq.RabbitConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rv.mq.enabled", havingValue = "true")
public class RabbitBookAuditListener {

    private final BookAuditNotificationHandler handler;

    @RabbitListener(queues = RabbitConfig.NOTIFY_QUEUE)
    public void onEvent(DomainEvent event) {
        handler.handle(event);
    }
}
