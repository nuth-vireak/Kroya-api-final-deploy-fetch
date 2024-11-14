package com.kshrd.kroya_api.payload.FoodRecipe;

import com.kshrd.kroya_api.entity.PhotoEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FoodRecipeUpdateRequest {
    private List<PhotoEntity> photo; // Changed to List<String> to hold photo URLs
    private String name;
    private String description;
    private Integer durationInMinutes;
    private String level;
    private Long cuisineId;
    private Long categoryId;
    private List<Ingredient> ingredients;
    private List<CookingStep> cookingSteps;
}
