package com.kshrd.kroya_api.payload.Receipt;

import com.kshrd.kroya_api.enums.PaymentType;
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
public class ReceiptResponse {
   private Long recipeId;
   private Long purchaseId;
   private FoodSellCardResponse foodSellCardResponse;
   private String reference;
   private LocalDateTime orderDate;
   private PaymentType paidBy;
   private String payer;
   private String seller;
   private String sellerPhoneNumber;
   private Integer quantity;
   private Double totalPrice;

}
