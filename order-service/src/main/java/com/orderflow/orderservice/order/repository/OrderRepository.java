package com.orderflow.orderservice.order.repository;

import com.orderflow.orderservice.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository
        extends JpaRepository<Order, UUID> {
}