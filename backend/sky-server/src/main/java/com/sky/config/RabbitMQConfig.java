package com.sky.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // 1. 定义队列名 (邮箱名字)
    public static final String SECKILL_QUEUE = "seckill.queue";

    // 2. 定义交换机名 (邮局分拣台)
    public static final String SECKILL_EXCHANGE = "seckill.exchange";

    // 3. 定义路由键 (信封上的地址)
    public static final String SECKILL_ROUTING_KEY = "seckill.key";

    /**
     * 定义一个队列 (Queue)
     * durable: true (持久化，RabbitMQ重启后队列还在)
     */
    @Bean
    public Queue seckillQueue() {
        return new Queue(SECKILL_QUEUE, true);
    }

    /**
     * 定义一个交换机 (DirectExchange)
     */
    @Bean
    public DirectExchange seckillExchange() {
        return new DirectExchange(SECKILL_EXCHANGE);
    }

    /**
     * 绑定关系：把队列绑定到交换机上，指定路由键
     * 意思是：只要发给 SECKILL_EXCHANGE，且路由键是 SECKILL_ROUTING_KEY 的消息，都投递到 SECKILL_QUEUE
     */
    @Bean
    public Binding binding() {
        return BindingBuilder.bind(seckillQueue()).to(seckillExchange()).with(SECKILL_ROUTING_KEY);
    }
}