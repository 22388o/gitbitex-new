package com.gitbitex.feed;

import javax.annotation.PostConstruct;

import com.alibaba.fastjson.JSON;

import com.gitbitex.account.entity.Account;
import com.gitbitex.feed.message.AccountMessage;
import com.gitbitex.feed.message.CandleMessage;
import com.gitbitex.feed.message.L2UpdateMessage;
import com.gitbitex.feed.message.OrderDoneMessage;
import com.gitbitex.feed.message.OrderMatchMessage;
import com.gitbitex.feed.message.OrderMessage;
import com.gitbitex.feed.message.OrderOpenMessage;
import com.gitbitex.feed.message.OrderReceivedMessage;
import com.gitbitex.feed.message.TickerMessage;
import com.gitbitex.marketdata.entity.Candle;
import com.gitbitex.marketdata.entity.Ticker;
import com.gitbitex.matchingengine.log.OrderBookLog;
import com.gitbitex.matchingengine.log.OrderDoneLog;
import com.gitbitex.matchingengine.log.OrderMatchLog;
import com.gitbitex.matchingengine.log.OrderOpenLog;
import com.gitbitex.matchingengine.log.OrderReceivedLog;
import com.gitbitex.matchingengine.snapshot.L2OrderBookUpdate;
import com.gitbitex.order.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class FeedMessageListener {
    private final RedissonClient redissonClient;
    private final SessionManager sessionManager;

    @PostConstruct
    public void run() {
        redissonClient.getTopic("order", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Order order = JSON.parseObject(msg, Order.class);
            String channel = order.getUserId() + "." + order.getProductId() + ".order";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(orderMessage(order)));
        });

        redissonClient.getTopic("account", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Account account = JSON.parseObject(msg, Account.class);
            String channel = account.getUserId() + "." + account.getCurrency() + ".funds";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(accountMessage(account)));
        });

        redissonClient.getTopic("ticker", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Ticker ticker = JSON.parseObject(msg, Ticker.class);
            String channel = ticker.getProductId() + ".ticker";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(new TickerMessage(ticker)));
        });

        redissonClient.getTopic("candle", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            Candle candle = JSON.parseObject(msg, Candle.class);
            String channel = candle.getProductId() + ".candle_" + candle.getGranularity() * 60;
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(candleMessage(candle)));
        });

        redissonClient.getTopic("l2change", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            L2OrderBookUpdate l2OrderBookUpdate = JSON.parseObject(msg, L2OrderBookUpdate.class);
            String channel = l2OrderBookUpdate.getProductId() + ".level2";
            sessionManager.sendMessageToChannel(channel, JSON.toJSONString(new L2UpdateMessage(l2OrderBookUpdate)));
        });

        redissonClient.getTopic("orderBookLog", StringCodec.INSTANCE).addListener(String.class, (c, msg) -> {
            OrderBookLog log = JSON.parseObject(msg, OrderBookLog.class);
            String fullChannel = log.getProductId() + ".full";
            switch (log.getType()) {
                case RECEIVED:
                    OrderReceivedLog orderReceivedLog = JSON.parseObject(msg, OrderReceivedLog.class);
                    sessionManager.sendMessageToChannel(fullChannel,
                        JSON.toJSONString(orderReceivedMessage(orderReceivedLog)));
                    break;
                case MATCH:
                    OrderMatchLog orderMatchLog = JSON.parseObject(msg, OrderMatchLog.class);
                    String matchChannel = log.getProductId() + ".match";
                    sessionManager.sendMessageToChannel(matchChannel, JSON.toJSONString(matchMessage(orderMatchLog)));
                    sessionManager.sendMessageToChannel(fullChannel, JSON.toJSONString(matchMessage(orderMatchLog)));
                    break;
                case OPEN:
                    OrderOpenLog orderOpenLog = JSON.parseObject(msg, OrderOpenLog.class);
                    sessionManager.sendMessageToChannel(fullChannel, JSON.toJSONString(orderOpenMessage(orderOpenLog)));
                    break;
                case DONE:
                    OrderDoneLog orderDoneLog = JSON.parseObject(msg, OrderDoneLog.class);
                    sessionManager.sendMessageToChannel(fullChannel, JSON.toJSONString(orderDoneMessage(orderDoneLog)));
                    break;
                default:
            }
        });
    }

    private OrderReceivedMessage orderReceivedMessage(OrderReceivedLog log) {
        OrderReceivedMessage message = new OrderReceivedMessage();
        message.setProductId(log.getProductId());
        message.setTime(log.getTime().toInstant().toString());
        message.setSequence(log.getSequence());
        message.setOrderId(log.getOrder().getOrderId());
        message.setSize(log.getOrder().getSize().toPlainString());
        message.setPrice(log.getOrder().getPrice() != null ? log.getOrder().getPrice().toPlainString() : null);
        message.setFunds(log.getOrder().getFunds() != null ? log.getOrder().getFunds().toPlainString() : null);
        message.setSide(log.getOrder().getSide().name().toUpperCase());
        message.setOrderType(log.getOrder().getType().name().toUpperCase());
        return message;
    }

    private OrderMatchMessage matchMessage(OrderMatchLog log) {
        OrderMatchMessage message = new OrderMatchMessage();
        message.setTradeId(log.getTradeId());
        message.setSequence(log.getSequence());
        message.setTakerOrderId(log.getTakerOrderId());
        message.setMakerOrderId(log.getMakerOrderId());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setSize(log.getSize().toPlainString());
        message.setPrice(log.getPrice().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        return message;
    }

    private OrderOpenMessage orderOpenMessage(OrderOpenLog log) {
        OrderOpenMessage message = new OrderOpenMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setPrice(log.getPrice().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        message.setRemainingSize(log.getRemainingSize().toPlainString());
        return message;
    }

    private OrderDoneMessage orderDoneMessage(OrderDoneLog log) {
        OrderDoneMessage message = new OrderDoneMessage();
        message.setSequence(log.getSequence());
        message.setTime(log.getTime().toInstant().toString());
        message.setProductId(log.getProductId());
        message.setPrice(log.getPrice().toPlainString());
        message.setSide(log.getSide().name().toLowerCase());
        message.setReason(log.getDoneReason().name().toUpperCase());
        message.setRemainingSize(log.getRemainingSize().toPlainString());
        return message;
    }

    private CandleMessage candleMessage(Candle candle) {
        CandleMessage message = new CandleMessage();
        message.setProductId(candle.getProductId());
        message.setGranularity(candle.getGranularity());
        message.setTime(candle.getTime());
        message.setOpen(candle.getOpen().stripTrailingZeros().toPlainString());
        message.setClose(candle.getClose().stripTrailingZeros().toPlainString());
        message.setHigh(candle.getHigh().stripTrailingZeros().toPlainString());
        message.setLow(candle.getLow().stripTrailingZeros().toPlainString());
        message.setVolume(candle.getVolume().stripTrailingZeros().toPlainString());
        return message;
    }

    private OrderMessage orderMessage(Order order) {
        OrderMessage message = new OrderMessage();
        message.setUserId(order.getUserId());
        message.setProductId(order.getProductId());
        message.setId(order.getOrderId());
        message.setPrice(order.getPrice().toPlainString());
        message.setSize(order.getSize().toPlainString());
        message.setFunds(order.getFunds().toPlainString());
        message.setSide(order.getSide().name().toLowerCase());
        message.setOrderType(order.getType().name().toLowerCase());
        message.setCreatedAt(order.getCreatedAt().toInstant().toString());
        message.setFillFees(order.getFillFees() != null ? order.getFillFees().toPlainString() : "0");
        message.setFilledSize(order.getFilledSize() != null ? order.getFilledSize().toPlainString() : "0");
        message.setExecutedValue(order.getExecutedValue() != null ? order.getExecutedValue().toPlainString() : "0");
        message.setStatus(order.getStatus().name().toLowerCase());
        return message;
    }

    private AccountMessage accountMessage(Account account) {
        AccountMessage message = new AccountMessage();
        message.setUserId(account.getUserId());
        message.setCurrencyCode(account.getCurrency());
        message.setAvailable(
            account.getAvailable() != null ? account.getAvailable().stripTrailingZeros().toPlainString() : "0");
        message.setHold(account.getHold() != null ? account.getHold().stripTrailingZeros().toPlainString() : "0");
        return message;
    }

}
