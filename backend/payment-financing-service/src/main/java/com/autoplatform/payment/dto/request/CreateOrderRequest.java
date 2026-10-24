package com.autoplatform.payment.dto.request;

import com.autoplatform.payment.enums.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "A buyer's request to purchase a specific car, either in full or via financing")
public record CreateOrderRequest(

        @NotNull Long buyerId,

        @NotNull @Schema(example = "100") Long carId,

        @NotNull PaymentType paymentType,

        @Min(5) @Max(90)
        @Schema(description = "Down payment percentage of the car price, required when paymentType=FINANCING", example = "20")
        Integer downPaymentPercent,

        @Min(3) @Max(84)
        @Schema(description = "Loan tenure in months, required when paymentType=FINANCING", example = "36")
        Integer tenureMonths,

        @Schema(description = "Optional accepted offer to purchase at the negotiated price instead of the listed price")
        String offerId
) {
}
