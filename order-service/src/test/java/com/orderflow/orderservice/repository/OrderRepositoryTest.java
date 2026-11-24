package com.orderflow.orderservice.repository;

import com.orderflow.orderservice.order.domain.Order;
import com.orderflow.orderservice.order.domain.OrderStatus;
import com.orderflow.orderservice.order.repository.OrderRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@SpringBootTest
@Transactional
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldSaveAndLoadOrder() {
        // Given
        UUID customerId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");

        Order order = new Order(customerId, amount);

        // When
        Order savedOrder = orderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        Order loadedOrder = orderRepository
                .findById(savedOrder.getId())
                .orElseThrow();

        // Then
        assertThat(loadedOrder.getId()).isNotNull();
        assertThat(loadedOrder.getCustomerId())
                .isEqualTo(customerId);
        assertThat(loadedOrder.getStatus())
                .isEqualTo(OrderStatus.CREATED);
        assertThat(loadedOrder.getTotalAmount())
                .isEqualByComparingTo(amount);
    }

    @Test
    void shouldUpdateOrderUsingDirtyChecking() {
        // Given
        Order order = new Order(
                UUID.randomUUID(),
                new BigDecimal("250.00")
        );

        orderRepository.save(order);

        entityManager.flush();
        entityManager.clear();

        // When
        Order managedOrder = orderRepository
                .findById(order.getId())
                .orElseThrow();

        managedOrder.complete();

        entityManager.flush();
        entityManager.clear();

        // Then
        Order updatedOrder = orderRepository
                .findById(order.getId())
                .orElseThrow();

        assertThat(updatedOrder.getStatus())
                .isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void shouldRollbackOrderWhenExceptionOccurs() {
        // Given
        Order order = new Order(
                UUID.randomUUID(),
                new BigDecimal("350.00")
        );

        // When
        assertThatThrownBy(() ->
                                   transactionTemplate.executeWithoutResult(status -> {
                                       orderRepository.save(order);

                                       entityManager.flush();

                                       throw new RuntimeException("Simulated failure");
                                   })
                          ).isInstanceOf(RuntimeException.class)
                .hasMessage("Simulated failure");

        // Then
        assertThat(orderRepository.findById(order.getId()))
                .isEmpty();
    }
}