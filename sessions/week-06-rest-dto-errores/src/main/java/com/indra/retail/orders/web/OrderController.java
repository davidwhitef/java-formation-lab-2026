package com.indra.retail.orders.web;

import com.indra.retail.orders.model.Order;
import com.indra.retail.orders.service.OrderService;
import com.indra.retail.orders.web.dto.CreateOrderRequest;
import com.indra.retail.orders.web.dto.OrderResponse;
import com.indra.retail.orders.web.mapper.OrderMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@Validated
public class OrderController {

    private final OrderService orderService;
    private final OrderMapper orderMapper;

    public OrderController(OrderService orderService, OrderMapper orderMapper) {
        this.orderService = orderService;
        this.orderMapper = orderMapper;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request) {
        Order created = orderService.create(orderMapper.toEntity(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(orderMapper.toResponse(created));
    }

    @GetMapping("/{orderId}")
    public OrderResponse getById(@PathVariable @NotBlank String orderId) {
        return orderMapper.toResponse(orderService.findById(orderId));
    }
}
