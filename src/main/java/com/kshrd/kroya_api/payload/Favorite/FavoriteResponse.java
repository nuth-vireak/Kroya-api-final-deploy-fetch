package com.kshrd.kroya_api.payload.Favorite;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FavoriteResponse {
    private Long id;
    private String userEmail;
    private Long foodRecipeId;
    private Long foodSellId;
    private LocalDateTime favoriteDate;
}