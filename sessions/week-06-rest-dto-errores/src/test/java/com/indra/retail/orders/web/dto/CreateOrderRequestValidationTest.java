package com.indra.retail.orders.web.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CreateOrderRequest - validaciones Bean Validation")
class CreateOrderRequestValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("Request válido no produce violaciones")
    void validRequest_hasNoViolations() {
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-001",
                List.of(new OrderItemRequest("SKU-001", 1, 10.0)),
                "Calle Falsa 123, Bogota"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("customerId vacío, items vacíos y deliveryAddress corto producen 3 violaciones")
    void invalidRequest_hasThreeViolations() {
        CreateOrderRequest request = new CreateOrderRequest("", List.of(), "corta");

        Set<ConstraintViolation<CreateOrderRequest>> violations = validator.validate(request);

        assertThat(violations).hasSize(3);
    }
}