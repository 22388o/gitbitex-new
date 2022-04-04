package com.gitbitex.repository;

import com.gitbitex.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long>, CrudRepository<Trade, Long>,
        JpaSpecificationExecutor<Trade> {

    List<Trade> findFirst50ByProductIdOrderByTimeDesc(String productId);

    Trade findByProductIdAndTradeId(String productId, long tradeId);

}
