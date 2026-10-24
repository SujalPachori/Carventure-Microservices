package com.autoplatform.payment.controller;

import com.autoplatform.payment.dto.request.CreateOrderRequest;
import com.autoplatform.payment.dto.response.OrderResponse;
import com.autoplatform.payment.dto.response.OrderWithPaymentResponse;
import com.autoplatform.payment.entity.Order;
import com.autoplatform.payment.enums.PaymentType;
import com.autoplatform.payment.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Orders", description = "Car purchase orders: full payment or financing, backed by Stripe")
public class OrderController {

    private final OrderService orderService;

    @Value("${stripe.publishable-key}")
    private String stripePublishableKey;

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping
    @Operation(summary = "Create a purchase order for a car and a Stripe PaymentIntent to pay for it")
    @ApiResponse(responseCode = "201", description = "Created; use stripeClientSecret to confirm payment client-side")
    @ApiResponse(responseCode = "409", description = "The car already has an active or completed order")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderWithPaymentResponse create(@Valid @RequestBody CreateOrderRequest request) {
        if (request.paymentType() == PaymentType.FINANCING && request.tenureMonths() == null) {
            throw new com.autoplatform.payment.exception.InvalidRequestException("tenureMonths is required for FINANCING orders");
        }
        Order order = orderService.createOrder(
                request.buyerId(), request.carId(), request.paymentType(),
                request.downPaymentPercent(), request.tenureMonths(), request.offerId());
        String clientSecret = orderService.getClientSecret(order.getId());
        return new OrderWithPaymentResponse(toResponse(order), clientSecret, stripePublishableKey);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order by id")
    public OrderResponse getById(@PathVariable Long id) {
        return toResponse(orderService.getById(id));
    }

    @GetMapping
    @Operation(summary = "List orders for a buyer or a seller")
    public List<OrderResponse> list(@RequestParam(required = false) Long buyerId,
                                     @RequestParam(required = false) Long sellerId) {
        List<Order> orders;
        if (buyerId != null) {
            orders = orderService.getForBuyer(buyerId);
        } else if (sellerId != null) {
            orders = orderService.getForSeller(sellerId);
        } else {
            throw new com.autoplatform.payment.exception.InvalidRequestException("Either buyerId or sellerId is required");
        }
        return orders.stream().map(this::toResponse).toList();
    }

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Buyer cancels an order that hasn't been paid yet")
    public OrderResponse cancel(@PathVariable Long id, @RequestParam Long buyerId) {
        return toResponse(orderService.cancel(id, buyerId));
    }

    private OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getId(), o.getBuyerId(), o.getSellerId(), o.getCarId(),
                o.getTotalAmount(), o.getDownPaymentAmount(), o.getCurrency(),
                o.getPaymentType(), o.getStatus(), o.getCreatedAt(), o.getUpdatedAt());
    }
}
