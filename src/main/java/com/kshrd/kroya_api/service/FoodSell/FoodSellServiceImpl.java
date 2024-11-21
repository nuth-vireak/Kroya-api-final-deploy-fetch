package com.kshrd.kroya_api.service.FoodSell;

import com.kshrd.kroya_api.dto.FoodRecipeDTO;
import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.enums.CurrencyType;
import com.kshrd.kroya_api.exception.DuplicateFieldExceptionHandler;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.exception.constand.FieldBlankExceptionHandler;
import com.kshrd.kroya_api.exception.exceptionValidateInput.Validation;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellRequest;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellResponse;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodSellServiceImpl implements FoodSellService {

    private final FoodRecipeRepository foodRecipeRepository;
    private final FoodSellRepository foodSellRepository;
    private final FavoriteRepository favoriteRepository;
    private final ModelMapper modelMapper;
    private final Validation validation;
    private final CuisineRepository cuisineRepository;

    @Override
    public BaseResponse<?> createFoodSell(FoodSellRequest foodSellRequest, Long foodRecipeId, CurrencyType currencyType) {

        // Check if the saved food sell is a favorite for the current user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Validate input fields
        validation.validateNotBlank(foodSellRequest.getLocation(), "Location");

        // Validate dateCooking (should be a valid LocalDateTime and in the future)
        validation.validationDateWithLocalDateTime(foodSellRequest.getDateCooking());

        // Validate amount (should be positive and non-zero)
        if (foodSellRequest.getAmount() <= 0) {
            throw new FieldBlankExceptionHandler("Amount must be greater than 0.");
        }

        // Validate price (should be positive and non-zero)
        if (foodSellRequest.getPrice() <= 0) {
            throw new FieldBlankExceptionHandler("Price must be greater than 0.");
        }

        // Fetch the FoodRecipeEntity by ID and check if it exists
        FoodRecipeEntity foodRecipeEntity = foodRecipeRepository.findById(foodRecipeId.intValue())
                .orElseThrow(() -> new NotFoundExceptionHandler("Food Recipe with ID " + foodRecipeId + " not found"));

        // Check if a FoodSellEntity already exists for this FoodRecipeEntity
        if (foodSellRepository.findByFoodRecipe(foodRecipeEntity).isPresent()) {
            throw new DuplicateFieldExceptionHandler("FoodSell already exists for this Food Recipe");
        }

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
        ZonedDateTime dateCookingZoned = foodSellRequest.getDateCooking()
                .atZone(ZoneId.of("Asia/Phnom_Penh"));

        // Determine if the food is orderable based on the dateCooking
        boolean isOrderable = !dateCookingZoned.isBefore(currentDateTimeInPhnomPenh);

        // Create a new FoodSellEntity with the correct isOrderable value
        FoodSellEntity foodSellEntity = FoodSellEntity.builder()
                .foodRecipe(foodRecipeEntity)
                .dateCooking(dateCookingZoned.toLocalDateTime()) // Save as LocalDateTime
                .amount(foodSellRequest.getAmount())
                .price(foodSellRequest.getPrice())
                .currencyType(currencyType.name())
                .location(foodSellRequest.getLocation())
                .isOrderable(isOrderable) // Use the calculated isOrderable value
                .build();

        // Save the FoodSellEntity to the database
        FoodSellEntity savedFoodSell = foodSellRepository.save(foodSellEntity);
        log.info("FoodSell entity saved successfully with ID: {}", savedFoodSell.getId());

        // Map the photos from FoodRecipeEntity to PhotoDTO
        List<PhotoDTO> photoDTOs = foodRecipeEntity.getPhotos().stream()
                .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                .toList();

        // Map FoodRecipeEntity to FoodRecipeDTO including photos
        FoodRecipeDTO foodRecipeDTO = modelMapper.map(foodRecipeEntity, FoodRecipeDTO.class);
        foodRecipeDTO.setPhoto(photoDTOs);

        // Set cuisine and category names
        if (foodRecipeEntity.getCuisine() != null) {
            foodRecipeDTO.setCuisineName(foodRecipeEntity.getCuisine().getCuisineName());
        }
        if (foodRecipeEntity.getCategory() != null) {
            foodRecipeDTO.setCategoryName(foodRecipeEntity.getCategory().getCategoryName());
        }

        boolean isFavorite = favoriteRepository.existsByUserAndFoodSell(currentUser, savedFoodSell);

        // Map FoodSellEntity to FoodSellResponse and set the mapped FoodRecipeDTO
        FoodSellResponse foodSellResponse = modelMapper.map(savedFoodSell, FoodSellResponse.class);
        foodSellResponse.setFoodRecipeDTO(foodRecipeDTO);
        foodSellResponse.setIsFavorite(isFavorite);
        foodSellResponse.setIsOrderable(savedFoodSell.getIsOrderable()); // Ensure isOrderable is transferred to the response

        return BaseResponse.builder()
                .message("FoodSell created successfully")
                .statusCode(String.valueOf(HttpStatus.CREATED.value()))
                .payload(foodSellResponse)
                .build();
    }

    @Override
    public BaseResponse<?> getAllFoodSells() {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch all FoodSellEntity records from the database
        List<FoodSellEntity> foodSellEntities = foodSellRepository.findAll();

        if (foodSellEntities.isEmpty()) {
            log.error("No FoodSell records found");
            return BaseResponse.builder()
                    .message("No food listings found at this time. Please check back later.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Fetch the user's favorite sell items
        List<FavoriteEntity> userFavorites = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser);
        List<Long> userFavoriteSellIds = userFavorites.stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Map each FoodSellEntity to FoodSellCardResponse
        List<FoodSellCardResponse> foodSellCardResponses = foodSellEntities.stream()
                .map(foodSellEntity -> {
                    // Check if dateCooking is in the future to set isOrderable
                    ZonedDateTime dateCookingZoned = foodSellEntity.getDateCooking().atZone(ZoneId.of("Asia/Phnom_Penh"));
                    log.info("DateCookingZoned: {}", dateCookingZoned);

                    boolean isOrderable = !dateCookingZoned.isBefore(currentDateTimeInPhnomPenh);
                    foodSellEntity.setIsOrderable(isOrderable);
                    foodSellRepository.save(foodSellEntity);  // Update the entity

                    // Map to FoodSellCardResponse
                    FoodSellCardResponse response = modelMapper.map(foodSellEntity, FoodSellCardResponse.class);

                    // Set specific fields in response
                    response.setFoodSellId(foodSellEntity.getId());
                    response.setIsOrderable(isOrderable);

                    // Map additional fields from FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = foodSellEntity.getFoodRecipe();
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    // Map photos
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

                    // Set isFavorite based on user preferences
                    response.setIsFavorite(userFavoriteSellIds.contains(foodSellEntity.getId()));

                    return response;
                })
                .collect(Collectors.toList());

        // Return the response with the list of FoodSellCardResponse objects
        return BaseResponse.builder()
                .message("All FoodSell records fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodSellCardResponses)
                .build();
    }

    @Override
    public BaseResponse<?> editFoodSell(Long foodSellId, FoodSellRequest foodSellRequest) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch the existing FoodSell by ID
        Optional<FoodSellEntity> foodSellOptional = foodSellRepository.findById(foodSellId);
        if (foodSellOptional.isEmpty()) {
            log.error("FoodSell with ID {} not found", foodSellId);
            return BaseResponse.builder()
                    .message("FoodSell not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }

        FoodSellEntity existingFoodSell = foodSellOptional.get();

        // Verify that the current user is the owner of the FoodSell
        if (!existingFoodSell.getFoodRecipe().getUser().getId().equals(currentUser.getId())) {
            log.error("User {} is not authorized to edit this FoodSell", currentUser.getEmail());
            return BaseResponse.builder()
                    .message("You are not authorized to edit this FoodSell")
                    .statusCode(String.valueOf(HttpStatus.FORBIDDEN.value()))
                    .build();
        }

        // Validate and update fields
        validation.validationDateWithLocalDateTime(foodSellRequest.getDateCooking()); // Validate LocalDateTime

        existingFoodSell.setDateCooking(foodSellRequest.getDateCooking());  // Set LocalDateTime
        existingFoodSell.setAmount(foodSellRequest.getAmount());
        existingFoodSell.setPrice(foodSellRequest.getPrice());
        existingFoodSell.setLocation(foodSellRequest.getLocation());

        // Update isOrderable based on dateCooking
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));
        ZonedDateTime dateCookingZoned = foodSellRequest.getDateCooking()
                .atZone(ZoneId.of("Asia/Phnom_Penh")); // Convert LocalDateTime to ZonedDateTime

        boolean isOrderable = !dateCookingZoned.isBefore(currentDateTimeInPhnomPenh);
        existingFoodSell.setIsOrderable(isOrderable);

        // Save the updated FoodSell
        FoodSellEntity updatedFoodSell = foodSellRepository.save(existingFoodSell);
        log.info("FoodSell updated successfully with ID: {}", updatedFoodSell.getId());

        // Map to FoodSellResponse
        FoodSellResponse foodSellResponse = modelMapper.map(updatedFoodSell, FoodSellResponse.class);

        // Map the foodRecipe and its photos
        FoodRecipeEntity linkedRecipe = updatedFoodSell.getFoodRecipe();
        FoodRecipeDTO foodRecipeDTO = modelMapper.map(linkedRecipe, FoodRecipeDTO.class);

        // Map photos from FoodRecipeEntity to PhotoDTO
        List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                .map(photoEntity -> new PhotoDTO(photoEntity.getId(), photoEntity.getPhoto()))
                .collect(Collectors.toList());
        foodRecipeDTO.setPhoto(photoDTOs);

        // Set cuisine and category names
        if (linkedRecipe.getCuisine() != null) {
            foodRecipeDTO.setCuisineName(linkedRecipe.getCuisine().getCuisineName());
        }
        if (linkedRecipe.getCategory() != null) {
            foodRecipeDTO.setCategoryName(linkedRecipe.getCategory().getCategoryName());
        }

        foodSellResponse.setFoodRecipeDTO(foodRecipeDTO);

        // Check if the food sell is a favorite for the current user
        boolean isFavorite = favoriteRepository.existsByUserAndFoodSell(currentUser, updatedFoodSell);
        foodSellResponse.setIsFavorite(isFavorite);
        foodSellResponse.setIsOrderable(updatedFoodSell.getIsOrderable());

        return BaseResponse.builder()
                .payload(foodSellResponse)
                .message("FoodSell updated successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    public BaseResponse<?> getFoodSellByCuisineID(Long cuisineId) {

        // Check if the cuisineId exists in the database
        boolean cuisineExists = cuisineRepository.existsById(cuisineId);
        if (!cuisineExists) {
            throw new NotFoundExceptionHandler("Cuisine ID " + cuisineId + " not found.");
        }

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("User authenticated: {}", currentUser.getEmail());

        // Fetch FoodSell entities by Cuisine ID
        List<FoodSellEntity> foodSellEntities = foodSellRepository.findByCuisineId(cuisineId);

        // Check if there are no FoodSell entities for this cuisineId
        if (foodSellEntities.isEmpty()) {
            return BaseResponse.builder()
                    .message("No FoodSell records found for the specified cuisine ID")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Fetch the user's favorite sell items
        List<FavoriteEntity> userFavorites = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser);
        List<Long> userFavoriteSellIds = userFavorites.stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define the corrected DateTimeFormatter
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

        // Map each FoodSellEntity to FoodSellCardResponse
        List<FoodSellCardResponse> foodSellCardResponses = foodSellEntities.stream()
                .map(foodSellEntity -> {
                    try {
                        // Parse the dateCooking using ISO format
                        LocalDateTime dateCooking = LocalDateTime.parse(foodSellEntity.getDateCooking().toString(), formatter);

                        // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
                        ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));

                        // Update isOrderable based on whether dateCooking is expired or not
                        foodSellEntity.setIsOrderable(!dateCookingZoned.isBefore(currentDateTimeInPhnomPenh));

                        // Save the updated isOrderable value to the database
                        foodSellRepository.save(foodSellEntity);

                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse dateCooking: {}", foodSellEntity.getDateCooking(), e);
                    }

                    // Map using ModelMapper
                    FoodSellCardResponse response = modelMapper.map(foodSellEntity, FoodSellCardResponse.class);

                    // Set isOrderable and foodSellId explicitly
                    response.setIsOrderable(foodSellEntity.getIsOrderable());
                    response.setFoodSellId(foodSellEntity.getId());

                    // Set additional fields from the related FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = foodSellEntity.getFoodRecipe();

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    // Set isFavorite based on user preferences
                    response.setIsFavorite(userFavoriteSellIds.contains(foodSellEntity.getId()));

                    // Map seller information from the linked FoodRecipeEntity's user
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

        // Return the response
        return BaseResponse.builder()
                .message("Food sells by cuisine ID fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodSellCardResponses)
                .build();
    }

    @Override
    public BaseResponse<?> searchFoodsByName(String name) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Fetch all food sells that match the name
        List<FoodSellEntity> foodSells = foodSellRepository.findByFoodRecipeNameContainingIgnoreCase(name);

        // Check if no records were found for the provided name
        if (foodSells.isEmpty()) {
            return BaseResponse.builder()
                    .message("No foods found matching the specified name. Please try again with a different search term.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Retrieve user's favorite recipe and food sell IDs

        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();


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
                .toList();

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("Search results fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(sellResponses)
                .build();
    }



}