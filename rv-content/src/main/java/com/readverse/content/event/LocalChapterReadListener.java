package com.readverse.content.event;

import com.readverse.framework.mq.DomainEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "rv.mq.enabled", havingValue = "false", matchIfMissing = true)
public class LocalChapterReadListener {

    private final ChapterReadStatsHandler handler;

    @EventListener
    public void onEvent(DomainEvent event) {
        handler.handle(event);
    }
}
