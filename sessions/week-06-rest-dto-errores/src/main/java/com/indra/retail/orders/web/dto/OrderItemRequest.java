package com.indra.retail.orders.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(

        @NotBlank(message = "{orderItem.sku.notBlank}")
        String sku,

        @Min(value = 1, message = "{orderItem.quantity.min}")
        int quantity,

        @DecimalMin(value = "0.0", inclusive = false, message = "{orderItem.unitPrice.positive}")
        double unitPrice
) {
}