package com.gitbitex.matchingengine;

import com.gitbitex.entity.Order;
import com.gitbitex.entity.Order.OrderSide;
import com.gitbitex.entity.Order.OrderType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.BeanUtils;

import java.math.BigDecimal;

@Getter
@Setter
public class BookOrder {
    private String userId;
    private String orderId;
    private OrderType type;
    private OrderSide side;
    private BigDecimal size;
    private BigDecimal price;
    private BigDecimal funds;
    private boolean postOnly;
    private Order.TimeInForcePolicy timeInForcePolicy;

    public BookOrder() {
    }

    public BookOrder(Order order) {
        BeanUtils.copyProperties(order, this);
    }

    public BookOrder copy() {
        BookOrder order = new BookOrder();
        order.setUserId(this.userId);
        order.setOrderId(this.orderId);
        order.setType(this.type);
        order.setSide(this.side);
        order.setPostOnly(this.postOnly);
        order.setTimeInForcePolicy(this.timeInForcePolicy);
        order.setSize(size != null ? new BigDecimal(size.toPlainString()) : null);
        order.setPrice(price != null ? new BigDecimal(price.toPlainString()) : null);
        order.setFunds(funds != null ? new BigDecimal(funds.toPlainString()) : null);
        return order;
    }
}
