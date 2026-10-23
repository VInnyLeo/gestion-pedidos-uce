package com.pedidosuce.product_service.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record DecreaseStockRequest(
        @NotEmpty(message = "Debe incluir al menos un producto")
        List<@Valid StockItemRequest> items
) {
}