package com.kshrd.kroya_api.payload.Purchase;

import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.enums.PaymentType;
import com.kshrd.kroya_api.enums.PurchaseStatusType;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PurchaseResponse {
    private Long purchaseId;
    private FoodSellCardResponse foodSellCardResponse;
    private String remark;
    private String location;
    private PaymentType paymentType;
    private PurchaseStatusType purchaseStatusType;
    private Integer quantity;
    private Double totalPrice;
    private LocalDateTime purchaseDate;
    private UserProfileDTO buyerInformation;
}
