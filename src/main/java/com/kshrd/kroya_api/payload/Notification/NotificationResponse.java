package com.kshrd.kroya_api.payload.Notification;

import com.kshrd.kroya_api.enums.FoodCardType;
import com.kshrd.kroya_api.enums.ItemType;
import com.kshrd.kroya_api.enums.PurchaseStatusType;
import com.kshrd.kroya_api.payload.Receipt.ReceiptResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationResponse {
    private Long notificationId;
    private Long purchaseId;
    private Long foodSellId;
    private String description;
    private Boolean isRead;
    private String foodPhoto;
    @Builder.Default
    private ItemType itemType = ItemType.FOOD_SELL;
    private FoodCardType foodCardType;
    private PurchaseStatusType purchaseStatusType;
    private LocalDateTime createdDate;
}