package com.gitbitex.module.matchingengine;

import com.alibaba.fastjson.JSON;

import com.gitbitex.module.matchingengine.marketmessage.MarketMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MarketMessagePublisher {
    private final RedissonClient redissonClient;

    public void publish(MarketMessage message) {
        logger.info("publish {}", JSON.toJSONString(message));
        redissonClient.getTopic(message.getType(), StringCodec.INSTANCE)
            .publish(JSON.toJSONString(message));
    }
}