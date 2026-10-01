package com.indra.retail.orders.web;

import com.indra.retail.orders.service.OrderNotFoundException;
import com.indra.retail.orders.web.dto.ApiError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("OrderNotFoundException se traduce a 404 con mensaje descriptivo")
    void handleOrderNotFound_returns404() {
        ResponseEntity<ApiError> response = handler.handleOrderNotFound(new OrderNotFoundException("abc-123"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().errors()).containsExactly("Pedido no encontrado: abc-123");
    }

    @Test
    @DisplayName("Cualquier excepción no controlada se traduce a 500 sin exponer detalles internos")
    void handleUnexpected_returns500WithGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleUnexpected(new RuntimeException("detalle sensible"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().errors()).containsExactly(
                "Ha ocurrido un error inesperado. Intente nuevamente más tarde.");
    }
}