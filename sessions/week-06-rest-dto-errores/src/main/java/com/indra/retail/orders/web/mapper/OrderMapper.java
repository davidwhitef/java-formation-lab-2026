package com.indra.retail.orders.web.mapper;

import com.indra.retail.orders.model.Order;
import com.indra.retail.orders.model.OrderItem;
import com.indra.retail.orders.web.dto.CreateOrderRequest;
import com.indra.retail.orders.web.dto.OrderItemRequest;
import com.indra.retail.orders.web.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    default Order toEntity(CreateOrderRequest request) {
        return new Order(request.customerId(), toItemEntities(request.items()), request.deliveryAddress());
    }

    List<OrderItem> toItemEntities(List<OrderItemRequest> items);

    OrderItem toItemEntity(OrderItemRequest item);

    @Mapping(target = "orderId", source = "id")
    OrderResponse toResponse(Order order);
}