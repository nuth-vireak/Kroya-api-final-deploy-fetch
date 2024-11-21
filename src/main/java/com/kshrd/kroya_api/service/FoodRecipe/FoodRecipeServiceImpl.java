package com.kshrd.kroya_api.service.FoodRecipe;

import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.*;
import com.kshrd.kroya_api.dto.PhotoDTO;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
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

        // Generate sequential IDs for ingredients if not provided
        AtomicLong ingredientIdCounter = new AtomicLong(1L);
        List<Ingredient> ingredients = foodRecipeRequest.getIngredients().stream()
                .map(req -> {
                    Ingredient ingredient = modelMapper.map(req, Ingredient.class);
                    if (ingredient.getId() == null || ingredient.getId() == 0) {
                        ingredient.setId(ingredientIdCounter.getAndIncrement());
                    }
                    return ingredient;
                })
                .collect(Collectors.toList());

        // Generate sequential IDs for cooking steps if not provided
        AtomicLong cookingStepIdCounter = new AtomicLong(1L);
        List<CookingStep> cookingSteps = foodRecipeRequest.getCookingSteps().stream()
                .map(req -> {
                    CookingStep step = modelMapper.map(req, CookingStep.class);
                    if (step.getId() == null || step.getId() == 0) {
                        step.setId(cookingStepIdCounter.getAndIncrement());
                    }
                    return step;
                })
                .collect(Collectors.toList());

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
        List<FoodRecipeEntity> foodRecipeEntities = foodRecipeRepository.findAll();

        if (foodRecipeEntities.isEmpty()) {
            log.error("No food recipes found");
            return BaseResponse.builder()
                    .message("No food recipes available at the moment. Please check back later.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

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
    public BaseResponse<?> editRecipe(Long recipeId, FoodRecipeUpdateRequest foodRecipeUpdateRequest) {
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
        Optional<CategoryEntity> categoryOptional = categoryRepository.findById(foodRecipeUpdateRequest.getCategoryId());
        if (categoryOptional.isEmpty()) {
            log.error("Category with ID {} not found", foodRecipeUpdateRequest.getCategoryId());
            return BaseResponse.builder()
                    .message("Category not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        Optional<CuisineEntity> cuisineOptional = cuisineRepository.findById(foodRecipeUpdateRequest.getCuisineId());
        if (cuisineOptional.isEmpty()) {
            log.error("Cuisine with ID {} not found", foodRecipeUpdateRequest.getCuisineId());
            return BaseResponse.builder()
                    .message("Cuisine not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        CategoryEntity categoryEntity = categoryOptional.get();
        CuisineEntity cuisineEntity = cuisineOptional.get();

        // Update recipe details
        existingRecipe.setName(foodRecipeUpdateRequest.getName());
        existingRecipe.setDescription(foodRecipeUpdateRequest.getDescription());
        existingRecipe.setDurationInMinutes(foodRecipeUpdateRequest.getDurationInMinutes());
        existingRecipe.setLevel(foodRecipeUpdateRequest.getLevel());
        existingRecipe.setCategory(categoryEntity);
        existingRecipe.setCuisine(cuisineEntity);
        existingRecipe.setUpdatedAt(LocalDateTime.now());


        // Initialize counters using an array for mutability within lambda
        long[] ingredientIdCounter = { existingRecipe.getIngredients().stream()
                .mapToLong(Ingredient::getId)
                .max()
                .orElse(0L) + 1 };
        long[] cookingStepIdCounter = { existingRecipe.getCookingSteps().stream()
                .mapToLong(CookingStep::getId)
                .max()
                .orElse(0L) + 1 };

        // Update or create new ingredients
        List<Ingredient> updatedIngredients = foodRecipeUpdateRequest.getIngredients().stream()
                .map(req -> {
                    Ingredient ingredient = modelMapper.map(req, Ingredient.class);
                    if (ingredient.getId() != null && ingredient.getId() > 0) {
                        // Update existing ingredient if found
                        return existingRecipe.getIngredients().stream()
                                .filter(existingIngredient -> existingIngredient.getId().equals(ingredient.getId()))
                                .findFirst()
                                .map(existingIngredient -> {
                                    if (ingredient.getName() != null) existingIngredient.setName(ingredient.getName());
                                    if (ingredient.getQuantity() != null) existingIngredient.setQuantity(ingredient.getQuantity());
                                    if (ingredient.getPrice() != null) existingIngredient.setPrice(ingredient.getPrice());
                                    return existingIngredient;
                                })
                                .orElseGet(() -> {
                                    // If not found, treat as a new entry with a new ID
                                    ingredient.setId(ingredientIdCounter[0]++);
                                    return ingredient;
                                });
                    } else {
                        // New entry with a new ID
                        ingredient.setId(ingredientIdCounter[0]++);
                        return ingredient;
                    }
                })
                .collect(Collectors.toList());
        existingRecipe.setIngredients(updatedIngredients);

        // Update or create new cooking steps
        List<CookingStep> updatedCookingSteps = foodRecipeUpdateRequest.getCookingSteps().stream()
                .map(req -> {
                    CookingStep step = modelMapper.map(req, CookingStep.class);
                    if (step.getId() != null && step.getId() > 0) {
                        // Update existing cooking step if found
                        return existingRecipe.getCookingSteps().stream()
                                .filter(existingStep -> existingStep.getId().equals(step.getId()))
                                .findFirst()
                                .map(existingStep -> {
                                    if (step.getDescription() != null) existingStep.setDescription(step.getDescription());
                                    return existingStep;
                                })
                                .orElseGet(() -> {
                                    // If not found, treat as a new entry with a new ID
                                    step.setId(cookingStepIdCounter[0]++);
                                    return step;
                                });
                    } else {
                        // New entry with a new ID
                        step.setId(cookingStepIdCounter[0]++);
                        return step;
                    }
                })
                .collect(Collectors.toList());
        existingRecipe.setCookingSteps(updatedCookingSteps);

        // Handle photo updates
        Map<Long, PhotoEntity> existingPhotosMap = existingRecipe.getPhotos().stream()
                .collect(Collectors.toMap(PhotoEntity::getId, photo -> photo));  // Store existing photos in a map

        List<PhotoEntity> updatedPhotos = foodRecipeUpdateRequest.getPhoto().stream()
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

        // Check if the cuisineId exists in the database
        boolean cuisineExists = cuisineRepository.existsById(cuisineId);
        if (!cuisineExists) {
            throw new NotFoundExceptionHandler("Cuisine ID " + cuisineId + " not found.");
        }

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch FoodRecipe entities by Cuisine ID
        List<FoodRecipeEntity> foodRecipeEntities = foodRecipeRepository.findByCuisineId(cuisineId);

        if (foodRecipeEntities.isEmpty()) {
            log.error("No food recipes found for cuisine ID {}", cuisineId);
            return BaseResponse.builder()
                    .message("No food recipes found for the specified cuisine ID")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

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
            return BaseResponse.builder()
                    .message("No food recipes found matching the provided name. Please try with a different search term.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
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