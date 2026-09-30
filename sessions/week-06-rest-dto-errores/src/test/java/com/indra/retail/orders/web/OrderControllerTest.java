package com.indra.retail.orders.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indra.retail.orders.web.dto.CreateOrderRequest;
import com.indra.retail.orders.web.dto.OrderItemRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@DisplayName("OrderController - POST /api/orders y GET /api/orders/{orderId}")
class OrderControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    OrderControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @Test
    @DisplayName("Dado un pedido válido, cuando se crea, entonces responde 201 con el OrderResponse")
    void createOrder_withValidRequest_returns201WithOrderResponse() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-001",
                List.of(new OrderItemRequest("SKU-001", 2, 19.99)),
                "Calle Falsa 123, Bogota"
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(39.98))
                .andExpect(jsonPath("$.estimatedDelivery").exists());
    }

    @Test
    @DisplayName("Dado un pedido inválido, cuando se crea, entonces responde 400 con los errores de cada campo")
    void createOrder_withInvalidRequest_returns400WithFieldErrors() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest("", List.of(),  "corta" );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors", hasSize(3)));
    }

    @Test
    @DisplayName("Dado un orderId inexistente, cuando se consulta, entonces responde 404 con mensaje descriptivo")
    void getById_withUnknownOrder_returns404WithDescriptiveMessage() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", "no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.errors[0]").value(containsString("Pedido no encontrado")));
    }
}