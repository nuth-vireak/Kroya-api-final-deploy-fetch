package com.kshrd.kroya_api.payload.FoodRecipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IngredientRequest {
    private String name;
    private Double quantity;
    private Double price;
}
