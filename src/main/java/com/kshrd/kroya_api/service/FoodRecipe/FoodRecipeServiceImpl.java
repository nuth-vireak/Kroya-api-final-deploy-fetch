package com.kshrd.kroya_api.service.FoodRecipe;

import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.*;
import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.repository.Category.CategoryRepository;
import com.kshrd.kroya_api.repository.Cuisine.CuisineRepository;
import com.kshrd.kroya_api.repository.Favorite.FavoriteRepository;
import com.kshrd.kroya_api.repository.FoodRecipe.FoodRecipeRepository;
import com.kshrd.kroya_api.repository.FoodSell.FoodSellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodRecipeServiceImpl implements FoodRecipeService {

    private final FoodRecipeRepository foodRecipeRepository;
    private final FoodSellRepository foodSellRepository;
    private final CategoryRepository categoryRepository;
    private final CuisineRepository cuisineRepository;
    private final FavoriteRepository favoriteRepository;
    private final ModelMapper modelMapper;

    @Override
    public BaseResponse<?> createRecipe(FoodRecipeRequest foodRecipeRequest) {

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch CategoryEntity by category ID
        Optional<CategoryEntity> categoryOptional = categoryRepository.findById(foodRecipeRequest.getCategoryId());
        if (categoryOptional.isEmpty()) {
            log.error("Category with ID {} not found", foodRecipeRequest.getCategoryId());
            return BaseResponse.builder()
                    .message("Category not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }
        CategoryEntity categoryEntity = categoryOptional.get();

        // Fetch CuisineEntity by cuisine ID
        Optional<CuisineEntity> cuisineOptional = cuisineRepository.findById(foodRecipeRequest.getCuisineId());
        if (cuisineOptional.isEmpty()) {
            log.error("Cuisine with ID {} not found", foodRecipeRequest.getCuisineId());
            return BaseResponse.builder()
                    .message("Cuisine not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        CuisineEntity cuisineEntity = cuisineOptional.get();

        // Assign unique IDs to ingredients if they are null or 0
        List<Ingredient> ingredients = foodRecipeRequest.getIngredients();
        long ingredientIdCounter = 1L;  // Start the ID counter for ingredients

        for (Ingredient ingredient : ingredients) {
            if (ingredient.getId() == null || ingredient.getId() == 0) {
                ingredient.setId(ingredientIdCounter++);  // Assign a new ID
            }
        }

        // Assign unique IDs to cooking steps if they are null or 0
        List<CookingStep> cookingSteps = foodRecipeRequest.getCookingSteps();
        long cookingStepIdCounter = 1L;  // Start the ID counter for cooking steps

        for (CookingStep cookingStep : cookingSteps) {
            if (cookingStep.getId() == null || cookingStep.getId() == 0) {
                cookingStep.setId(cookingStepIdCounter++);  // Assign a new ID
            }
        }

        // Map the RecipeRequest to RecipeEntity
        FoodRecipeEntity foodRecipeEntity = FoodRecipeEntity.builder()
                .name(foodRecipeRequest.getName())
                .description(foodRecipeRequest.getDescription())
                .durationInMinutes(foodRecipeRequest.getDurationInMinutes())
                .level(foodRecipeRequest.getLevel())
                .cuisine(cuisineEntity)  // Set CuisineEntity (using ID)
                .category(categoryEntity)  // Set CategoryEntity (using ID)
                .ingredients(ingredients)
                .cookingSteps(cookingSteps)
                .user(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        // Save the recipe to the database first
        FoodRecipeEntity savedRecipe = foodRecipeRepository.save(foodRecipeEntity);

        // Process photo entities from the request
        List<PhotoEntity> photoEntities = foodRecipeRequest.getPhoto().stream()
                .map(photoEntity -> {
                    photoEntity.setFoodRecipe(savedRecipe); // Set the association with the saved recipe
                    return photoEntity;
                })
                .collect(Collectors.toList());

        // Set the photos to the saved recipe
        savedRecipe.setPhotos(photoEntities);

        // Save the recipe again to persist the photos
        foodRecipeRepository.save(savedRecipe);

        // Log the newly created recipe's ID
        log.info("Recipe saved successfully with ID: {}", savedRecipe.getId());

        // Map the saved entity to RecipeResponse using ModelMapper
        FoodRecipeResponse foodRecipeResponse = modelMapper.map(savedRecipe, FoodRecipeResponse.class);

        // Map photo entities to PhotoResponse objects
        List<PhotoDTO> photoRespons = savedRecipe.getPhotos().stream()
                .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                .collect(Collectors.toList());
        foodRecipeResponse.setPhoto(photoRespons);

        // Set category name in the response
        foodRecipeResponse.setCategoryName(categoryEntity.getCategoryName());

        // Set cuisine name in the response
        foodRecipeResponse.setCuisineName(cuisineEntity.getCuisineName());

        // Check if this recipe is a favorite for the current user
        boolean isFavorite = favoriteRepository.existsByUserAndFoodRecipe(currentUser, savedRecipe);
        foodRecipeResponse.setIsFavorite(isFavorite);

        // Return a success response with the saved recipe as the payload
        return BaseResponse.builder()
                .payload(foodRecipeResponse)
                .message("Recipe created successfully")
                .statusCode("201")
                .build();
    }

    @Override
    public BaseResponse<?> getAllFoodRecipes() {

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch all FoodRecipeEntity records from the database
//        List<FoodRecipeEntity> foodRecipeEntities = foodRecipeRepository.findAll();

        List<FoodRecipeEntity> foodRecipeEntities = foodRecipeRepository.findAllByOrderByIdDesc();
        // Fetch the user's favorite recipes
        List<FavoriteEntity> userFavorites = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser);
        List<Long> userFavoriteRecipeIds = userFavorites.stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        // Filter out FoodRecipeEntities that have a related FoodSellEntity
        List<FoodRecipeCardResponse> foodRecipeResponses = foodRecipeEntities.stream()
                .filter(foodRecipeEntity -> !foodSellRepository.existsByFoodRecipe(foodRecipeEntity))
                .map(foodRecipeEntity -> {
                    // Map to FoodRecipeCardResponse using ModelMapper
                    FoodRecipeCardResponse response = modelMapper.map(foodRecipeEntity, FoodRecipeCardResponse.class);

                    // Set isFavorite if it's in the user's favorites
                    response.setIsFavorite(userFavoriteRecipeIds.contains(foodRecipeEntity.getId()));

                    // Map photos to PhotoResponse
                    List<PhotoDTO> photoResponses = foodRecipeEntity.getPhotos().stream()
                            .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                            .collect(Collectors.toList());

                    // Set the photo field in the response
                    response.setPhoto(photoResponses);

                    return response;
                })
                .toList();

        // Return the response with the list of FoodRecipeCardResponse objects
        return BaseResponse.builder()
                .message("All food recipes fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodRecipeResponses)
                .build();
    }

    @Override
    public BaseResponse<?> editRecipe(Long recipeId, FoodRecipeRequest foodRecipeRequest) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch the existing recipe by ID
        Optional<FoodRecipeEntity> recipeOptional = foodRecipeRepository.findById(Math.toIntExact(recipeId));
        if (recipeOptional.isEmpty()) {
            log.error("Recipe with ID {} not found", recipeId);
            return BaseResponse.builder()
                    .message("Recipe not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        FoodRecipeEntity existingRecipe = recipeOptional.get();

        // Check if the current user is the owner of the recipe
        if (!existingRecipe.getUser().getId().equals(currentUser.getId())) {
            log.error("User {} is not authorized to edit this recipe", currentUser.getEmail());
            return BaseResponse.builder()
                    .message("You are not authorized to edit this recipe")
                    .statusCode(String.valueOf(HttpStatus.FORBIDDEN.value()))
                    .build();
        }

        // Fetch and validate CategoryEntity and CuisineEntity
        Optional<CategoryEntity> categoryOptional = categoryRepository.findById(foodRecipeRequest.getCategoryId());
        if (categoryOptional.isEmpty()) {
            log.error("Category with ID {} not found", foodRecipeRequest.getCategoryId());
            return BaseResponse.builder()
                    .message("Category not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        Optional<CuisineEntity> cuisineOptional = cuisineRepository.findById(foodRecipeRequest.getCuisineId());
        if (cuisineOptional.isEmpty()) {
            log.error("Cuisine with ID {} not found", foodRecipeRequest.getCuisineId());
            return BaseResponse.builder()
                    .message("Cuisine not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        CategoryEntity categoryEntity = categoryOptional.get();
        CuisineEntity cuisineEntity = cuisineOptional.get();

        // Update recipe details
        existingRecipe.setName(foodRecipeRequest.getName());
        existingRecipe.setDescription(foodRecipeRequest.getDescription());
        existingRecipe.setDurationInMinutes(foodRecipeRequest.getDurationInMinutes());
        existingRecipe.setLevel(foodRecipeRequest.getLevel());
        existingRecipe.setCategory(categoryEntity);
        existingRecipe.setCuisine(cuisineEntity);
        existingRecipe.setUpdatedAt(LocalDateTime.now());

        // Update ingredients and cooking steps (same as before)
        List<Ingredient> ingredients = foodRecipeRequest.getIngredients();
        List<CookingStep> cookingSteps = foodRecipeRequest.getCookingSteps();
        long ingredientIdCounter = 1L;
        for (Ingredient ingredient : ingredients) {
            if (ingredient.getId() == null || ingredient.getId() == 0) {
                ingredient.setId(ingredientIdCounter++);
            }
        }

        long cookingStepIdCounter = 1L;
        for (CookingStep cookingStep : cookingSteps) {
            if (cookingStep.getId() == null || cookingStep.getId() == 0) {
                cookingStep.setId(cookingStepIdCounter++);
            }
        }
        existingRecipe.setIngredients(ingredients);
        existingRecipe.setCookingSteps(cookingSteps);

        // Handle photo updates
        Map<Long, PhotoEntity> existingPhotosMap = existingRecipe.getPhotos().stream()
                .collect(Collectors.toMap(PhotoEntity::getId, photo -> photo));  // Store existing photos in a map

        List<PhotoEntity> updatedPhotos = foodRecipeRequest.getPhoto().stream()
                .map(photoEntity -> {
                    if (photoEntity.getId() != null && existingPhotosMap.containsKey(photoEntity.getId())) {
                        // Update existing photo
                        PhotoEntity existingPhoto = existingPhotosMap.get(photoEntity.getId());
                        existingPhoto.setPhoto(photoEntity.getPhoto());  // Update the photo content if needed
                        return existingPhoto;
                    } else {
                        // Add new photo
                        photoEntity.setFoodRecipe(existingRecipe);
                        return photoEntity;
                    }
                })
                .toList();

        // Remove photos that are no longer present in the request
        existingRecipe.getPhotos().removeIf(photo -> !updatedPhotos.contains(photo));

        // Add or update the photos
        existingRecipe.getPhotos().clear();
        existingRecipe.getPhotos().addAll(updatedPhotos);

        // Save the updated recipe
        foodRecipeRepository.save(existingRecipe);

        // Check if this recipe is a favorite for the current user
        boolean isFavorite = favoriteRepository.existsByUserAndFoodRecipe(currentUser, existingRecipe);

        // Log the updated recipe's ID
        log.info("Recipe updated successfully with ID: {}", existingRecipe.getId());

        // Map the updated recipe to FoodRecipeResponse using ModelMapper
        FoodRecipeResponse foodRecipeResponse = modelMapper.map(existingRecipe, FoodRecipeResponse.class);

        // Map photo entities to PhotoDTO objects
        List<PhotoDTO> photoRespons = existingRecipe.getPhotos().stream()
                .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                .collect(Collectors.toList());
        foodRecipeResponse.setPhoto(photoRespons);

        // Set category and cuisine names in the response
        foodRecipeResponse.setCategoryName(categoryEntity.getCategoryName());
        foodRecipeResponse.setCuisineName(cuisineEntity.getCuisineName());

        // Set isFavorite in the response
        foodRecipeResponse.setIsFavorite(isFavorite);

        // Return a success response with the updated recipe as the payload
        return BaseResponse.builder()
                .payload(foodRecipeResponse)
                .message("Recipe updated successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    public BaseResponse<?> getFoodRecipeByCuisineID(Long cuisineId) {

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch FoodRecipe entities by Cuisine ID
        List<FoodRecipeEntity> foodRecipeEntities = foodRecipeRepository.findByCuisineId(cuisineId);

        // Fetch the user's favorite recipes
        List<FavoriteEntity> userFavorites = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser);
        List<Long> userFavoriteRecipeIds = userFavorites.stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        // Filter out FoodRecipeEntities that have a related FoodSellEntity
        List<FoodRecipeCardResponse> foodRecipeResponses = foodRecipeEntities.stream()
                .filter(foodRecipeEntity -> !foodSellRepository.existsByFoodRecipe(foodRecipeEntity))
                .map(foodRecipeEntity -> {
                    // Map to FoodRecipeCardResponse using ModelMapper
                    FoodRecipeCardResponse response = modelMapper.map(foodRecipeEntity, FoodRecipeCardResponse.class);

                    // Set isFavorite if it's in the user's favorites
                    response.setIsFavorite(userFavoriteRecipeIds.contains(foodRecipeEntity.getId()));

                    // Map photos to PhotoResponse
                    List<PhotoDTO> photoResponses = foodRecipeEntity.getPhotos().stream()
                            .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                            .collect(Collectors.toList());

                    // Set the photo field in the response
                    response.setPhoto(photoResponses);

                    return response;
                })
                .toList();

        // Return the response
        return BaseResponse.builder()
                .message("Food recipes by cuisine ID fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodRecipeResponses)
                .build();
    }

    @Override
    public BaseResponse<?> searchFoodsByName(String name) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Fetch all food recipes that match the name
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByNameContainingIgnoreCase(name);

        // Check if no records were found for the provided name
        if (foodRecipes.isEmpty()) {
            throw new NotFoundExceptionHandler("No foods found for the specified name.");
        }

        // Retrieve user's favorite recipe IDs
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        // Map food recipes to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> recipeResponses = foodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

                    // Set favorite status based on user's favorites
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId()));

                    // Map photos
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("Search results fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(recipeResponses)
                .build();
    }

}