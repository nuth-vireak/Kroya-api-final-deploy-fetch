package com.kshrd.kroya_api.controller;

import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeRequest;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeUpdateRequest;
import com.kshrd.kroya_api.service.FoodRecipe.FoodRecipeService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/food-recipe")
@RequiredArgsConstructor
@Slf4j
public class FoodRecipeController {

    private final FoodRecipeService foodRecipeService;

    @Operation(
            summary = "🍲 Create a New Food Recipe",
            description = """
                    Creates a new food recipe in the system.
                    **📩 Request Body**:
                    - **foodRecipeRequest**: JSON object with the details of the food recipe to be created.
                    
                    **📩 Response Summary**:
                    - **201**: ✅ Food recipe created successfully.
                    - **400**: 🚫 Invalid input data provided.
                    """
    )
    @PostMapping("/post-food-recipe")
    public BaseResponse<?> createRecipe(@RequestBody FoodRecipeRequest foodRecipeRequest) {
        return foodRecipeService.createRecipe(foodRecipeRequest);
    }

    @Operation(
            summary = "📃 Get All Food Recipes",
            description = """
                    Fetches a list of all available food recipes in the system.
                    
                    **📩 Response Summary**:
                    - **200**: ✅ List of food recipes retrieved successfully.
                    - **404**: 🚫 No food recipes found.
                    """
    )
    @GetMapping("/list")
    public BaseResponse<?> getAllFoodRecipe() {
        return foodRecipeService.getAllFoodRecipes();
    }

    @Operation(
            summary = "✏️ Edit an Existing Food Recipe",
            description = """
                    Updates an existing food recipe in the system.
                    - **Path Variable**: **recipeId**: ID of the food recipe to edit.
                    - **Request Body**: **foodRecipeRequest**: JSON object with the updated details of the recipe.
                    
                    **📩 Response Summary**:
                    - **200**: ✅ Food recipe updated successfully.
                    - **404**: 🚫 Food recipe not found.
                    """
    )
    @PutMapping("/edit-food-recipe/{recipeId}")
    public BaseResponse<?> editRecipe(@PathVariable Long recipeId, @RequestBody FoodRecipeUpdateRequest foodRecipeUpdateRequest) {
        return foodRecipeService.editRecipe(recipeId, foodRecipeUpdateRequest);
    }

    @Operation(
            summary = "🍜 Get Food Recipes by Cuisine ID",
            description = """
                    Retrieves food recipes based on the specified cuisine ID.
                    - **Path Variable**: **cuisineId**: ID of the cuisine.
                    
                    **📩 Response Summary**:
                    - **200**: ✅ List of food recipes by cuisine ID retrieved successfully.
                    - **404**: 🚫 No food recipes found for the specified cuisine ID.
                    """
    )
    @GetMapping("/cuisine/{cuisineId}")
    public BaseResponse<?> getFoodRecipeByCuisineID(@PathVariable Long cuisineId) {
        return foodRecipeService.getFoodRecipeByCuisineID(cuisineId);
    }


    @Operation(
            summary = "🔍 Search Food Recipes by Name",
            description = """
                    Searches for food recipes that match the specified name.
                    
                    **📩 Request Parameter**:
                    - **name**: Part or full name of the food recipe to search for.
                    
                    **📩 Response Summary**:
                    - **200**: ✅ Matching food recipes retrieved successfully.
                    - **404**: 🚫 No matching food recipes found for the provided name.
                    """
    )
    @GetMapping("/search")
    public BaseResponse<?> searchFoodsByName(@RequestParam String name) {
        return foodRecipeService.searchFoodsByName(name);
    }

}

