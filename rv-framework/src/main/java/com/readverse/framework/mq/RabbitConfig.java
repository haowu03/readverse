package com.readverse.framework.mq;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "rv.mq.enabled", havingValue = "true")
public class RabbitConfig {

    public static final String EXCHANGE = "rv.event.topic";
    public static final String DLX = "rv.event.dlx";
    public static final String NOTIFY_QUEUE = "rv.notify.audit";
    public static final String STATS_QUEUE = "rv.stats.read";
    public static final String BOOK_QUEUE = "rv.book.created";

    @Bean
    public TopicExchange eventExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange deadLetterExchange() {
        return new TopicExchange(DLX, true, false);
    }

    @Bean
    public Queue notifyQueue() {
        return QueueBuilder.durable(NOTIFY_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("dead.notify")
                .build();
    }

    @Bean
    public Queue statsQueue() {
        return QueueBuilder.durable(STATS_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("dead.stats")
                .build();
    }

    @Bean
    public Queue bookQueue() {
        return QueueBuilder.durable(BOOK_QUEUE)
                .deadLetterExchange(DLX)
                .deadLetterRoutingKey("dead.book")
                .build();
    }

    @Bean
    public Queue notifyDeadQueue() {
        return QueueBuilder.durable("rv.notify.audit.dlq").build();
    }

    @Bean
    public Binding notifyBinding() {
        return BindingBuilder.bind(notifyQueue()).to(eventExchange()).with("event.book_audit_passed");
    }

    @Bean
    public Binding notifyRejectedBinding() {
        return BindingBuilder.bind(notifyQueue()).to(eventExchange()).with("event.book_audit_rejected");
    }

    @Bean
    public Binding statsBinding() {
        return BindingBuilder.bind(statsQueue()).to(eventExchange()).with("event.chapter_read");
    }

    @Bean
    public Binding bookBinding() {
        return BindingBuilder.bind(bookQueue()).to(eventExchange()).with("event.book_created");
    }

    @Bean
    public Binding notifyDeadBinding() {
        return BindingBuilder.bind(notifyDeadQueue()).to(deadLetterExchange()).with("dead.notify");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter converter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }
}
