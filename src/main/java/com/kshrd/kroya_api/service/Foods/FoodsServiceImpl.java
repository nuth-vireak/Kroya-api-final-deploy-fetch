package com.kshrd.kroya_api.service.Foods;

import com.kshrd.kroya_api.dto.FoodRecipeDTO;
import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.FeedbackEntity;
import com.kshrd.kroya_api.entity.FoodRecipeEntity;
import com.kshrd.kroya_api.entity.FoodSellEntity;
import com.kshrd.kroya_api.entity.UserEntity;
import com.kshrd.kroya_api.enums.ItemType;
import com.kshrd.kroya_api.exception.ForbiddenException;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.exception.constand.FieldBlankExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeCardResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellResponse;
import com.kshrd.kroya_api.repository.Favorite.FavoriteRepository;
import com.kshrd.kroya_api.repository.Feedback.FeedbackRepository;
import com.kshrd.kroya_api.repository.FoodRecipe.FoodRecipeRepository;
import com.kshrd.kroya_api.repository.FoodSell.FoodSellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodsServiceImpl implements FoodsService {

    private final FoodRecipeRepository foodRecipeRepository;
    private final FoodSellRepository foodSellRepository;
    private final FavoriteRepository favoriteRepository;
    private final ModelMapper modelMapper;
    private final FeedbackRepository feedbackRepository;

    // Get all food by category ID
    @Override
    public BaseResponse<?> getAllFoodsByCategory(Long categoryId) {
        // Validate the categoryId to ensure it's not null and is positive
        if (categoryId == null || categoryId <= 0) {
            throw new FieldBlankExceptionHandler("Category ID must be a positive number and cannot be null.");
        }

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch all food recipes by category
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByCategoryId(categoryId);
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = foodRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Fetch all food sells by category
        List<FoodSellEntity> foodSells = foodSellRepository.findByCategoryId(categoryId);
        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Check if no records were found for the provided categoryId
        if (foodRecipes.isEmpty() && foodSells.isEmpty()) {
            throw new NotFoundExceptionHandler("No foods found for the specified category ID.");
        }

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define a flexible DateTimeFormatter to handle ISO-like format with 'T' and optional milliseconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")   // Base pattern with 'T' separator
                .optionalStart()                          // Optional milliseconds
                .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true)
                .optionalEnd()
                .toFormatter();

        // Map pure FoodRecipeEntities to FoodRecipeCardResponse using ModelMapper
        List<FoodRecipeCardResponse> foodRecipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId()));

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map FoodSellEntities to FoodSellCardResponse using ModelMapper
        List<FoodSellCardResponse> foodSellResponses = foodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);
                    response.setIsFavorite(userFavoriteFoodSellIds.contains(sell.getId()));

                    // Set foodSellId
                    response.setFoodSellId(sell.getId());

                    // Map seller information
                    UserEntity seller = sell.getFoodRecipe().getUser();
                    UserProfileDTO sellerInfo = UserProfileDTO.builder()
                            .userId(Long.valueOf(seller.getId()))
                            .fullName(seller.getFullName())
                            .phoneNumber(seller.getPhoneNumber())
                            .profileImage(seller.getProfileImage())
                            .location(seller.getLocation())
                            .build();
                    response.setSellerInformation(sellerInfo);

                    try {
                        // Parse the dateCooking using the flexible DateTimeFormatter
                        LocalDateTime dateCooking = LocalDateTime.parse(sell.getDateCooking().toString(), formatter);

                        // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
                        ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));

                        // Update isOrderable based on whether dateCooking is expired or not
                        sell.setIsOrderable(!dateCookingZoned.isBefore(currentDateTimeInPhnomPenh));
                        // Save the updated isOrderable value to the database
                        foodSellRepository.save(sell);
                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse dateCooking: {}", sell.getDateCooking(), e);
                    }


                    // Set additional fields from the related FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = sell.getFoodRecipe();

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare the response map
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodRecipes", foodRecipeResponses);
        responseMap.put("foodSells", foodSellResponses);

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("All foods fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    @Override
    public BaseResponse<?> getPopularFoods() {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch all food recipes ordered by average rating
        List<FoodRecipeEntity> allRecipes = foodRecipeRepository.findAllByOrderByAverageRatingDesc();
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        // Filter out food recipes linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = allRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Fetch all food sells ordered by average rating and user favorites
        List<FoodSellEntity> popularSells = foodSellRepository.findAllByOrderByAverageRatingDesc();
        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Current time in Phnom Penh time zone
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm") // Base pattern for hours and minutes
                .optionalStart()
                .appendPattern(":ss") // Optional seconds pattern
                .optionalEnd()
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true) // Optional fractional seconds
                .optionalEnd()
                .toFormatter();

        // Map pure FoodRecipeEntities to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> popularRecipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId())); // Set favorite status

                    // Map photos from FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map FoodSellEntities to FoodSellCardResponse
        List<FoodSellCardResponse> popularSellResponses = popularSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);

                    // Set foodSellId
                    response.setFoodSellId(sell.getId());
                    response.setIsFavorite(userFavoriteFoodSellIds.contains(sell.getId())); // Set favorite status

                    LocalDateTime dateCooking;
                    try {
                        dateCooking = LocalDateTime.parse(sell.getDateCooking().toString(), formatter);
                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse dateCooking: {}", sell.getDateCooking(), e);
                        dateCooking = LocalDateTime.now(); // Fallback to current date-time
                    }

                    // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
                    ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));

                    // Update isOrderable based on whether dateCooking is expired or not
                    if (dateCookingZoned.isBefore(currentDateTimeInPhnomPenh)) {
                        sell.setIsOrderable(false);
                    } else {
                        sell.setIsOrderable(true);
                    }

                    // Save updated isOrderable value
                    foodSellRepository.save(sell);

                    // Map details from linked FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = sell.getFoodRecipe();

                    // Map photos from linked FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set additional fields from linked recipe
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    // Set seller information
                    UserEntity seller = linkedRecipe.getUser();
                    UserProfileDTO sellerInfo = UserProfileDTO.builder()
                            .userId(Long.valueOf(seller.getId()))
                            .fullName(seller.getFullName())
                            .phoneNumber(seller.getPhoneNumber())
                            .profileImage(seller.getProfileImage())
                            .location(seller.getLocation())
                            .build();
                    response.setSellerInformation(sellerInfo);

                    // Ensure isOrderable is transferred correctly
                    response.setIsOrderable(sell.getIsOrderable());

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare response map
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("popularRecipes", popularRecipeResponses);
        responseMap.put("popularSells", popularSellResponses);

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("Popular foods fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    @Override
    public BaseResponse<?> getFoodDetail(Long id, ItemType itemType) {
        log.info("Fetching food detail for ID: {} and item type: {}", id, itemType);

        // Validate the id to ensure it's not null and is positive
        if (id == null || id <= 0) {
            throw new FieldBlankExceptionHandler("Food ID must be a positive number and cannot be null.");
        }

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Calculate rating percentages
        LinkedHashMap<Integer, Double> ratingPercentages = calculateRatingPercentages(id, itemType);

        if (itemType == ItemType.FOOD_RECIPE) {
            // Fetch FoodRecipe details
            Optional<FoodRecipeEntity> recipeOptional = foodRecipeRepository.findById(Math.toIntExact(id));
            if (recipeOptional.isEmpty()) {
                log.error("FoodRecipe with ID {} not found", id);
                throw new NotFoundExceptionHandler("FoodRecipe with ID " + id + " not found.");
            }

            FoodRecipeEntity foodRecipe = recipeOptional.get();

            // Check if this food recipe is linked to any food sell
            Optional<FoodSellEntity> linkedSell = foodSellRepository.findByFoodRecipe(foodRecipe);
            if (linkedSell.isPresent()) {
                throw new FieldBlankExceptionHandler("This recipe is part of a Food Sell. Please select the Food Sell item instead.");
            }

            FoodRecipeResponse foodRecipeResponse = modelMapper.map(foodRecipe, FoodRecipeResponse.class);

            // Map the photo entities
            List<PhotoDTO> photoResponses = foodRecipe.getPhotos().stream()
                    .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                    .collect(Collectors.toList());
            foodRecipeResponse.setPhoto(photoResponses);

            // Set the cuisine and category names
            if (foodRecipe.getCuisine() != null) {
                foodRecipeResponse.setCuisineName(foodRecipe.getCuisine().getCuisineName());
            }
            if (foodRecipe.getCategory() != null) {
                foodRecipeResponse.setCategoryName(foodRecipe.getCategory().getCategoryName());
            }

            // Set rating percentages
            foodRecipeResponse.setRatingPercentages(ratingPercentages);

            // Check if the food recipe is a favorite for the current user
            boolean isFavorite = favoriteRepository.existsByUserAndFoodRecipe(currentUser, foodRecipe);
            foodRecipeResponse.setIsFavorite(isFavorite);

            return BaseResponse.builder()
                    .payload(foodRecipeResponse)
                    .message("FoodRecipe details fetched successfully")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();

        } else if (itemType == ItemType.FOOD_SELL) {
            // Fetch FoodSell details
            Optional<FoodSellEntity> sellOptional = foodSellRepository.findById(id);
            if (sellOptional.isEmpty()) {
                log.error("FoodSell with ID {} not found", id);
                throw new NotFoundExceptionHandler("FoodSell with ID " + id + " not found.");
            }

            FoodSellEntity foodSell = sellOptional.get();
            FoodSellResponse foodSellResponse = modelMapper.map(foodSell, FoodSellResponse.class);

            // Map the linked FoodRecipe details
            FoodRecipeEntity linkedRecipe = foodSell.getFoodRecipe();
            FoodRecipeDTO foodRecipeDTO = modelMapper.map(linkedRecipe, FoodRecipeDTO.class);
            List<PhotoDTO> photoResponses = linkedRecipe.getPhotos().stream()
                    .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                    .collect(Collectors.toList());
            foodRecipeDTO.setPhoto(photoResponses);

            // Set cuisine and category names in the FoodSellResponse
            if (linkedRecipe.getCuisine() != null) {
                foodRecipeDTO.setCuisineName(linkedRecipe.getCuisine().getCuisineName());
            }
            if (linkedRecipe.getCategory() != null) {
                foodRecipeDTO.setCategoryName(linkedRecipe.getCategory().getCategoryName());
            }

            foodSellResponse.setFoodRecipeDTO(foodRecipeDTO);

            // Set rating percentages
            foodSellResponse.setRatingPercentages(ratingPercentages);

            // Check if the food sell is a favorite for the current user
            boolean isFavorite = favoriteRepository.existsByUserAndFoodSell(currentUser, foodSell);
            foodSellResponse.setIsFavorite(isFavorite);

            return BaseResponse.builder()
                    .payload(foodSellResponse)
                    .message("FoodSell details fetched successfully")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();

        } else {
            log.error("Invalid item type: {}", itemType);
            throw new FieldBlankExceptionHandler("Invalid item type.");
        }
    }

    @Override
    public BaseResponse<?> deleteFood(Long id, ItemType itemType) {
        // Validate the id to ensure it's not null and is positive
        if (id == null || id <= 0) {
            throw new FieldBlankExceptionHandler("Food ID must be a positive number and cannot be null.");
        }

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        if (itemType == ItemType.FOOD_RECIPE) {
            // Delete FoodRecipe
            Optional<FoodRecipeEntity> recipeOptional = foodRecipeRepository.findById(Math.toIntExact(id));
            if (recipeOptional.isEmpty()) {
                log.error("FoodRecipe with ID {} not found", id);
                throw new NotFoundExceptionHandler("FoodRecipe with ID " + id + " not found.");
            }

            FoodRecipeEntity foodRecipe = recipeOptional.get();

            // Check if the current user is the owner of the recipe
            if (!foodRecipe.getUser().getId().equals(currentUser.getId())) {
                log.error("User {} is not authorized to delete this recipe", currentUser.getEmail());
                throw new ForbiddenException("You are not authorized to delete this recipe.");
            }

            foodRecipeRepository.delete(foodRecipe);

            return BaseResponse.builder()
                    .message("FoodRecipe deleted successfully")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();

        } else if (itemType == ItemType.FOOD_SELL) {
            // Delete FoodSell
            Optional<FoodSellEntity> sellOptional = foodSellRepository.findById(id);
            if (sellOptional.isEmpty()) {
                log.error("FoodSell with ID {} not found", id);
                throw new NotFoundExceptionHandler("FoodSell with ID " + id + " not found.");
            }

            FoodSellEntity foodSell = sellOptional.get();

            // Check if the current user is the owner of the food sell item
            if (!foodSell.getFoodRecipe().getUser().getId().equals(currentUser.getId())) {
                log.error("User {} is not authorized to delete this food item", currentUser.getEmail());
                throw new ForbiddenException("You are not authorized to delete this food item.");
            }

            // Since cascade is set to ALL, deleting the FoodSell will automatically delete the associated FoodRecipe
            foodSellRepository.delete(foodSell);

            return BaseResponse.builder()
                    .message("FoodSell and associated FoodRecipe deleted successfully")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();

        } else {
            throw new FieldBlankExceptionHandler("Invalid item type provided.");
        }
    }

    @Override
    public BaseResponse<?> searchFoodsByName(String name) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch all food recipes and food sells that match the name
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByNameContainingIgnoreCase(name);
        List<FoodSellEntity> foodSells = foodSellRepository.findByFoodRecipeNameContainingIgnoreCase(name);

        // Check if no records were found for the provided name
        if (foodRecipes.isEmpty() && foodSells.isEmpty()) {
            throw new NotFoundExceptionHandler("No foods found for the specified name.");
        }

        // Retrieve user's favorite recipe and food sell IDs
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Map food recipes to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> recipeResponses = foodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId())); // Set favorite status

                    // Map photos
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map food sells to FoodSellCardResponse
        List<FoodSellCardResponse> sellResponses = foodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);

                    // Set foodSellId directly from FoodSellEntity ID
                    response.setFoodSellId(sell.getId());

                    // Set name from related FoodRecipeEntity
                    response.setName(sell.getFoodRecipe().getName());

                    // Set favorite status
                    response.setIsFavorite(userFavoriteFoodSellIds.contains(sell.getId()));

                    // Map photos from FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = sell.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Map seller information from FoodRecipeEntity user
                    UserEntity seller = sell.getFoodRecipe().getUser();
                    UserProfileDTO sellerInfo = UserProfileDTO.builder()
                            .userId(Long.valueOf(seller.getId()))
                            .fullName(seller.getFullName())
                            .phoneNumber(seller.getPhoneNumber())
                            .profileImage(seller.getProfileImage())
                            .location(seller.getLocation())
                            .build();
                    response.setSellerInformation(sellerInfo);

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare the response map
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodRecipes", recipeResponses);
        responseMap.put("foodSells", sellResponses);

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("Search results fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    @Override
    public BaseResponse<?> getAllFoods() {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch and filter all food recipes not linked to food sells
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findAll().stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Validate if there are no food recipes
        if (foodRecipes.isEmpty()) {
            throw new NotFoundExceptionHandler("No food recipes found.");
        }

        // Fetch all food sells
        List<FoodSellEntity> foodSells = foodSellRepository.findAll();
        if (foodSells.isEmpty()) {
            throw new NotFoundExceptionHandler("No food sells found.");
        }

        // Fetch favorite IDs for recipes and sells
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Current time for orderable status calculation
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // DateTimeFormatter to handle dateCooking parsing with optional fractional seconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .optionalEnd()
                .toFormatter();

        // Map FoodRecipeEntity to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> foodRecipeResponses = foodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId()));

                    // Map photos
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Map user info to UserDTO
                    UserDTO userDTO = new UserDTO(recipe.getUser().getId(), recipe.getUser().getFullName(), recipe.getUser().getProfileImage());
                    response.setUser(userDTO);

                    return response;
                })
                .collect(Collectors.toList());

        // Map FoodSellEntity to FoodSellCardResponse
        List<FoodSellCardResponse> foodSellResponses = foodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);
                    response.setIsFavorite(userFavoriteFoodSellIds.contains(sell.getId()));
                    response.setFoodSellId(sell.getId());

                    try {
                        // Parse and set orderable status based on dateCooking
                        LocalDateTime dateCooking = LocalDateTime.parse(sell.getDateCooking().toString(), formatter);
                        ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));
                        sell.setIsOrderable(!dateCookingZoned.isBefore(currentDateTimeInPhnomPenh));
                        foodSellRepository.save(sell); // Update database

                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse dateCooking: {}", sell.getDateCooking(), e);
                    }

                    // Map additional fields from FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = sell.getFoodRecipe();
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    // Map photos from FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Map seller information
                    UserEntity seller = linkedRecipe.getUser();
                    UserProfileDTO sellerInfo = UserProfileDTO.builder()
                            .userId(Long.valueOf(seller.getId()))
                            .fullName(seller.getFullName())
                            .phoneNumber(seller.getPhoneNumber())
                            .profileImage(seller.getProfileImage())
                            .location(seller.getLocation())
                            .build();
                    response.setSellerInformation(sellerInfo);

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare response payload
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodRecipes", foodRecipeResponses);
        responseMap.put("foodSells", foodSellResponses);

        // Build and return BaseResponse
        return BaseResponse.builder()
                .message("All foods fetched successfully with authentication")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    /**
     * Helper method to calculate the rating percentages for each star level (1 to 5).
     */
    private LinkedHashMap<Integer, Double> calculateRatingPercentages(Long foodId, ItemType itemType) {
        List<FeedbackEntity> feedbacks;
        if (itemType == ItemType.FOOD_RECIPE) {
            feedbacks = feedbackRepository.findByFoodRecipeId(foodId);
        } else {
            feedbacks = feedbackRepository.findByFoodSellId(foodId);
        }

        // Count ratings
        Map<Integer, Long> ratingCount = feedbacks.stream()
                .filter(feedback -> feedback.getRatingValue() != null)
                .collect(Collectors.groupingBy(FeedbackEntity::getRatingValue, Collectors.counting()));

        long totalRatings = ratingCount.values().stream().mapToLong(Long::longValue).sum();

        // Use a LinkedHashMap to ensure the order of keys from 5 to 1 in the JSON response
        LinkedHashMap<Integer, Double> ratingPercentages = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) {
            double percentage = totalRatings > 0 ? (ratingCount.getOrDefault(i, 0L) * 100.0 / totalRatings) : 0.0;
            ratingPercentages.put(i, Math.round(percentage * 100.0) / 100.0); // Round to two decimal places
        }

        // Log the rating distribution for verification
        log.info("Total ratings for item {}: {}", foodId, totalRatings);
        ratingPercentages.forEach((rating, percentage) ->
                log.info("Rating {}: {} users ({}%)", rating, ratingCount.getOrDefault(rating, 0L), percentage));

        return ratingPercentages;
    }


}