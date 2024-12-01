package com.kshrd.kroya_api.payload.FoodSell;

import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.enums.FoodCardType;
import com.kshrd.kroya_api.enums.ItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SellerOrderCardResponse {
    private Long foodSellId;
    private String name;
    private Double price;
    private String currencyType;
    private Integer orderCount;
    private List<PhotoDTO> photo;
    private LocalDateTime dateCooking;
    private Boolean isOrderable;
    @Builder.Default
    private ItemType itemType = ItemType.FOOD_SELL;
    private FoodCardType foodCardType;
}