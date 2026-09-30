package com.indra.retail.orders.web.dto;

import com.indra.retail.orders.model.OrderStatus;

import java.time.LocalDate;

public record OrderResponse(
        String orderId,
        OrderStatus status,
        double totalAmount,
        LocalDate estimatedDelivery
) {
}
