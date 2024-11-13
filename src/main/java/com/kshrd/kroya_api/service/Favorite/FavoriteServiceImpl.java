package com.kshrd.kroya_api.service.Favorite;

import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.enums.ItemType;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final FoodRecipeRepository foodRecipeRepository;
    private final FoodSellRepository foodSellRepository;
    private final ModelMapper modelMapper;

    @Override
    public BaseResponse<?> saveFoodToFavorite(Long foodId, ItemType itemType) {

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        if (itemType == ItemType.FOOD_RECIPE) {
            // Find the FoodRecipe by ID
            Optional<FoodRecipeEntity> foodRecipeOptional = foodRecipeRepository.findById(Math.toIntExact(foodId));
            if (foodRecipeOptional.isEmpty()) {
                return BaseResponse.builder()
                        .message("Food Recipe not found")
                        .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                        .build();
            }

            FoodRecipeEntity foodRecipe = foodRecipeOptional.get();

            // Check if this food recipe is linked to any food sell
            Optional<FoodSellEntity> linkedSell = foodSellRepository.findByFoodRecipe(foodRecipe);
            if (linkedSell.isPresent()) {
                return BaseResponse.builder()
                        .message("This recipe is part of a Food Sell. Please select the Food Sell item instead.")
                        .statusCode("400")
                        .build();
            }

            // Check if the recipe is already in the user's favorites
            Optional<FavoriteEntity> existingFavorite = favoriteRepository.findByUserAndFoodRecipe(currentUser, foodRecipe);
            if (existingFavorite.isPresent()) {
                return BaseResponse.builder()
                        .message("This recipe is already in your favorites")
                        .statusCode("400")
                        .build();
            }

            // Save the recipe to the favorites
            FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                    .user(currentUser)
                    .foodRecipe(foodRecipe)
                    .favoriteDate(LocalDateTime.now())
                    .build();

            favoriteRepository.save(favoriteEntity);

        } else if (itemType == ItemType.FOOD_SELL) {
            // Find the FoodSell by ID
            Optional<FoodSellEntity> foodSellOptional = foodSellRepository.findById(foodId);
            if (foodSellOptional.isEmpty()) {
                return BaseResponse.builder()
                        .message("Food Sell not found")
                        .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                        .build();
            }

            FoodSellEntity foodSell = foodSellOptional.get();

            // Check if the sell item is already in the user's favorites
            Optional<FavoriteEntity> existingFavorite = favoriteRepository.findByUserAndFoodSell(currentUser, foodSell);
            if (existingFavorite.isPresent()) {
                return BaseResponse.builder()
                        .message("This item is already in your favorites")
                        .statusCode("400")
                        .build();
            }

            // Save the sell item to the favorites
            FavoriteEntity favoriteEntity = FavoriteEntity.builder()
                    .user(currentUser)
                    .foodSell(foodSell)
                    .favoriteDate(LocalDateTime.now())
                    .build();

            favoriteRepository.save(favoriteEntity);

        } else {
            return BaseResponse.builder()
                    .message("Invalid item type")
                    .statusCode("400")
                    .build();
        }

        return BaseResponse.builder()
                .message("Item added to favorites")
                .statusCode("201")
                .build();
    }


    @Override
    public BaseResponse<?> unsavedFoodFromFavorite(Long foodId, ItemType itemType) {

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        if (itemType == ItemType.FOOD_RECIPE) {
            // Find the FoodRecipe by ID
            Optional<FoodRecipeEntity> foodRecipeOptional = foodRecipeRepository.findById(Math.toIntExact(foodId));
            if (foodRecipeOptional.isEmpty()) {
                return BaseResponse.builder()
                        .message("Food Recipe not found")
                        .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                        .build();
            }

            FoodRecipeEntity foodRecipe = foodRecipeOptional.get();

            // Check if this food recipe is linked to any food sell
            Optional<FoodSellEntity> linkedSell = foodSellRepository.findByFoodRecipe(foodRecipe);
            if (linkedSell.isPresent()) {
                return BaseResponse.builder()
                        .message("This recipe is part of a Food Sell. Please select the Food Sell item instead.")
                        .statusCode("400")
                        .build();
            }

            // Check if the recipe is in the user's favorites
            Optional<FavoriteEntity> existingFavorite = favoriteRepository.findByUserAndFoodRecipe(currentUser, foodRecipe);
            if (existingFavorite.isEmpty()) {
                return BaseResponse.builder()
                        .message("This recipe is not in your favorites")
                        .statusCode("400")
                        .build();
            }

            // Remove the favorite entry
            favoriteRepository.delete(existingFavorite.get());
            log.info("Food Recipe removed from favorites for user: {}", currentUser.getEmail());

        } else if (itemType == ItemType.FOOD_SELL) {
            // Find the FoodSell by ID
            Optional<FoodSellEntity> foodSellOptional = foodSellRepository.findById(foodId);
            if (foodSellOptional.isEmpty()) {
                return BaseResponse.builder()
                        .message("Food Sell not found")
                        .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                        .build();
            }

            FoodSellEntity foodSell = foodSellOptional.get();

            // Check if the sell item is in the user's favorites
            Optional<FavoriteEntity> existingFavorite = favoriteRepository.findByUserAndFoodSell(currentUser, foodSell);
            if (existingFavorite.isEmpty()) {
                return BaseResponse.builder()
                        .message("This item is not in your favorites")
                        .statusCode("400")
                        .build();
            }

            // Remove the favorite entry
            favoriteRepository.delete(existingFavorite.get());
            log.info("Food Sell removed from favorites for user: {}", currentUser.getEmail());

        } else {
            return BaseResponse.builder()
                    .message("Invalid item type")
                    .statusCode("400")
                    .build();
        }

        return BaseResponse.builder()
                .message("Item removed from favorites")
                .statusCode("200")
                .build();
    }

    @Override
    public BaseResponse<?> getAllFavoriteFoodsByCurrentUser() {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Fetching favorite foods for user: {}", currentUser.getEmail());

        // Fetch all favorite entities for the user
        List<FavoriteEntity> favoriteEntities = favoriteRepository.findByUser(currentUser);

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Map favorite entities to FoodRecipeCardResponse (pure food recipes)
        List<FoodRecipeCardResponse> favoriteFoodRecipes = favoriteEntities.stream()
                .filter(favorite -> favorite.getFoodRecipe() != null && favorite.getFoodSell() == null)
                .map(favorite -> {
                    FoodRecipeCardResponse response = modelMapper.map(favorite.getFoodRecipe(), FoodRecipeCardResponse.class);
                    response.setIsFavorite(true); // Mark it as favorite

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = favorite.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .toList();

        // Map favorite entities to FoodSellCardResponse (food sells)
        List<FoodSellCardResponse> favoriteFoodSells = favoriteEntities.stream()
                .filter(favorite -> favorite.getFoodSell() != null)
                .map(favorite -> {
                    FoodSellCardResponse response = modelMapper.map(favorite.getFoodSell(), FoodSellCardResponse.class);

                    // Set foodSellId
                    response.setFoodSellId(favorite.getFoodSell().getId());

                    // Set additional fields from the related FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = favorite.getFoodSell().getFoodRecipe();

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());
                    response.setIsFavorite(true);

                    // Parse the dateCooking to LocalDateTime using the correct formatter
                    LocalDateTime dateCooking = LocalDateTime.parse(
                            favorite.getFoodSell().getDateCooking().toString(),
                            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
                    );

                    // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
                    ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));

                    // Update isOrderable based on whether dateCooking is expired or not
                    if (dateCookingZoned.isBefore(currentDateTimeInPhnomPenh)) {
                        favorite.getFoodSell().setIsOrderable(false);
                    } else {
                        favorite.getFoodSell().setIsOrderable(true);
                    }

                    // Save the updated isOrderable value to the database
                    foodSellRepository.save(favorite.getFoodSell());

                    // Set isOrderable explicitly in the response
                    response.setIsOrderable(favorite.getFoodSell().getIsOrderable());

                    // Set seller information from the linked FoodRecipeEntity user
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
                .toList();

        // Prepare the response map
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("favoriteFoodRecipes", favoriteFoodRecipes);
        responseMap.put("favoriteFoodSells", favoriteFoodSells);

        // Build the BaseResponse
        return BaseResponse.builder()
                .message("Favorite foods fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    @Override
    public BaseResponse<?> searchFoodsByName(String name) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Searching favorite foods by name '{}' for user: {}", name, currentUser.getEmail());

        // Get current time in Phnom Penh time zone (UTC+7) to determine orderable status for food sells
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Search for favorite FoodRecipe items by name
        List<FavoriteEntity> favoriteRecipeEntities = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser);
        List<FoodRecipeCardResponse> favoriteFoodRecipes = favoriteRecipeEntities.stream()
                .filter(favorite -> favorite.getFoodRecipe().getName().toLowerCase().contains(name.toLowerCase()))
                .map(favorite -> {
                    FoodRecipeCardResponse response = modelMapper.map(favorite.getFoodRecipe(), FoodRecipeCardResponse.class);
                    response.setIsFavorite(true);

                    // Map photos
                    List<PhotoDTO> photoDTOs = favorite.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .toList();

        // Search for favorite FoodSell items by name
        List<FavoriteEntity> favoriteSellEntities = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser);
        List<FoodSellCardResponse> favoriteFoodSells = favoriteSellEntities.stream()
                .filter(favorite -> favorite.getFoodSell().getFoodRecipe().getName().toLowerCase().contains(name.toLowerCase()))
                .map(favorite -> {
                    FoodSellCardResponse response = modelMapper.map(favorite.getFoodSell(), FoodSellCardResponse.class);
                    response.setIsFavorite(true);

                    // Map photos from the related FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = favorite.getFoodSell().getFoodRecipe();
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    // Check if the FoodSell item is orderable
                    LocalDateTime dateCooking = favorite.getFoodSell().getDateCooking();
                    ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));
                    response.setIsOrderable(!dateCookingZoned.isBefore(currentDateTimeInPhnomPenh));

                    return response;
                })
                .toList();

        // Prepare the response payload
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("favoriteFoodRecipes", favoriteFoodRecipes);
        responseMap.put("favoriteFoodSells", favoriteFoodSells);

        // Return the response
        return BaseResponse.builder()
                .message("Search results fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }


}