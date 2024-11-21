package com.kshrd.kroya_api.payload.FoodRecipe;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CookingStepRequest {
    private String description;
}
