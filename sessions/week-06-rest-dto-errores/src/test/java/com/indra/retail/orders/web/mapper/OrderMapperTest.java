package com.indra.retail.orders.web.mapper;

import com.indra.retail.orders.model.Order;
import com.indra.retail.orders.web.dto.CreateOrderRequest;
import com.indra.retail.orders.web.dto.OrderItemRequest;
import com.indra.retail.orders.web.dto.OrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OrderMapper")
class OrderMapperTest {

    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);

    @Test
    @DisplayName("Convierte CreateOrderRequest en Order calculando el total y los items")
    void toEntity_mapsRequestToOrder() {
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-001",
                List.of(new OrderItemRequest("SKU-001", 2, 19.99)),
                "Calle Falsa 123, Bogota"
        );

        Order order = orderMapper.toEntity(request);

        assertThat(order.getCustomerId()).isEqualTo("CUST-001");
        assertThat(order.getItems()).hasSize(1);
        assertThat(order.getTotalAmount()).isEqualTo(39.98);
        assertThat(order.getDeliveryAddress()).isEqualTo("Calle Falsa 123, Bogota");
    }

    @Test
    @DisplayName("Convierte Order en OrderResponse exponiendo solo los campos públicos")
    void toResponse_mapsOrderToPublicFields() {
        Order order = new Order("CUST-001",
                List.of(new com.indra.retail.orders.model.OrderItem("SKU-001", 1, 10.0)),
                "Calle Falsa 123, Bogota");

        OrderResponse response = orderMapper.toResponse(order);

        assertThat(response.orderId()).isEqualTo(order.getId());
        assertThat(response.status()).isEqualTo(order.getStatus());
        assertThat(response.totalAmount()).isEqualTo(order.getTotalAmount());
        assertThat(response.estimatedDelivery()).isEqualTo(order.getEstimatedDelivery());
    }
}
