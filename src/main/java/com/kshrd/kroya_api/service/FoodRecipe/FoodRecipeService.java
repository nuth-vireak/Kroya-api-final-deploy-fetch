package com.kshrd.kroya_api.service.FoodRecipe;

import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeRequest;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeUpdateRequest;

public interface FoodRecipeService {
    BaseResponse<?> createRecipe(FoodRecipeRequest foodRecipeRequest);

    BaseResponse<?> getAllFoodRecipes();

    BaseResponse<?> editRecipe(Long recipeId, FoodRecipeUpdateRequest foodRecipeUpdateRequest);

    BaseResponse<?> getFoodRecipeByCuisineID(Long cuisineId);

    BaseResponse<?> searchFoodsByName(String name);
}
