package com.kshrd.kroya_api.service.GuestUser;

import com.kshrd.kroya_api.dto.*;
import com.kshrd.kroya_api.entity.FeedbackEntity;
import com.kshrd.kroya_api.entity.FoodRecipeEntity;
import com.kshrd.kroya_api.entity.FoodSellEntity;
import com.kshrd.kroya_api.entity.UserEntity;
import com.kshrd.kroya_api.enums.ItemType;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.exception.constand.FieldBlankExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeCardResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellResponse;
import com.kshrd.kroya_api.repository.Category.CategoryRepository;
import com.kshrd.kroya_api.repository.Cuisine.CuisineRepository;
import com.kshrd.kroya_api.repository.Feedback.FeedbackRepository;
import com.kshrd.kroya_api.repository.FoodRecipe.FoodRecipeRepository;
import com.kshrd.kroya_api.repository.FoodSell.FoodSellRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
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
public class GuestUserServiceImpl implements GuestUserService {

    private final FoodSellRepository foodSellRepository;
    private final FoodRecipeRepository foodRecipeRepository;
    private final ModelMapper modelMapper;
    private final FeedbackRepository feedbackRepository;
    private final CategoryRepository categoryRepository;
    private final CuisineRepository cuisineRepository;

    //Get all food sells
    @Override
    public BaseResponse<?> getAllFoodSells() {
        log.info("Fetching all FoodSell records for guest user");

        // Fetch all FoodSellEntity records from the database
        List<FoodSellEntity> foodSellEntities = foodSellRepository.findAll();

        // Check if no records were found
        if (foodSellEntities.isEmpty()) {
            log.warn("No FoodSell records found in the database");
            return BaseResponse.builder()
                    .message("No FoodSell records found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Map each FoodSellEntity to FoodSellCardResponse
        List<FoodSellCardResponse> foodSellCardResponses = foodSellEntities.stream()
                .map(foodSellEntity -> {
                    // Determine if the food is orderable based on the dateCooking
                    boolean isOrderable = !foodSellEntity.getDateCooking()
                            .atZone(ZoneId.of("Asia/Phnom_Penh"))
                            .isBefore(currentDateTimeInPhnomPenh);
                    foodSellEntity.setIsOrderable(isOrderable);

                    // Map using ModelMapper
                    FoodSellCardResponse response = modelMapper.map(foodSellEntity, FoodSellCardResponse.class);

                    // Set foodSellId explicitly
                    response.setFoodSellId(foodSellEntity.getId());

                    // Set isOrderable explicitly
                    response.setIsOrderable(isOrderable);

                    // Set additional fields from the related FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = foodSellEntity.getFoodRecipe();

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = linkedRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Map additional details from linked recipe
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

                    return response;
                })
                .collect(Collectors.toList());

        // Return the response with the list of FoodSellCardResponse objects
        return BaseResponse.builder()
                .message("All FoodSell records fetched successfully for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodSellCardResponses)
                .build();
    }

    //Get all food recipes
    @Override
    public BaseResponse<?> getAllFoodRecipes() {
        log.info("Fetching all FoodRecipe records for guest user");

        // Fetch all FoodRecipeEntity records from the database
        List<FoodRecipeEntity> foodRecipeEntities = foodRecipeRepository.findAll();

        // Check if no records were found
        if (foodRecipeEntities.isEmpty()) {
            log.warn("No FoodRecipe records found in the database");
            return BaseResponse.builder()
                    .message("No FoodRecipe records found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Map each FoodRecipeEntity to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> foodRecipeCardResponses = foodRecipeEntities.stream()
                .map(foodRecipeEntity -> {
                    // Use ModelMapper to map entity to response
                    FoodRecipeCardResponse response = modelMapper.map(foodRecipeEntity, FoodRecipeCardResponse.class);

                    // Set additional fields from FoodRecipeEntity
                    response.setName(foodRecipeEntity.getName());
                    response.setAverageRating(foodRecipeEntity.getAverageRating());
                    response.setTotalRaters(foodRecipeEntity.getTotalRaters());

                    // Map photos to a list of PhotoDTOs
                    List<PhotoDTO> photoDTOs = foodRecipeEntity.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Map user information to UserDTO
                    UserEntity creator = foodRecipeEntity.getUser();
                    UserDTO userDTO = UserDTO.builder()
                            .id(creator.getId())
                            .fullName(creator.getFullName())
                            .profileImage(creator.getProfileImage())
                            .build();
                    response.setUser(userDTO);

                    return response;
                })
                .collect(Collectors.toList());

        // Return the response with the list of FoodRecipeCardResponse objects
        return BaseResponse.builder()
                .message("All FoodRecipe records fetched successfully for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodRecipeCardResponses)
                .build();
    }

    //Get all food by category id
    @Override
    public BaseResponse<?> getAllFoodsByCategory(Long categoryId) {
        log.info("Fetching all foods by category ID: {} for guest user", categoryId);

        // Validate the categoryId to ensure it's not null and is positive
        if (categoryId == null || categoryId <= 0) {
            throw new FieldBlankExceptionHandler("Category ID must be a positive number and cannot be null.");
        }

        // check if category exists in database
        if (!categoryRepository.existsById(categoryId)) {
            throw new NotFoundExceptionHandler("Category with ID " + categoryId + " not found.");
        }

        // Fetch all food recipes and food sells by category
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByCategoryId(categoryId);
        List<FoodSellEntity> foodSells = foodSellRepository.findByCategoryId(categoryId);

        // Check if no records were found for the provided categoryId
        if (foodRecipes.isEmpty() && foodSells.isEmpty()) {
            return BaseResponse.builder()
                    .message("No foods found for the specified category ID.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = foodRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define a flexible DateTimeFormatter to handle variable fractional seconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd HH:mm:ss")  // Base pattern
                .optionalStart()                       // Optional fractional seconds
                .appendFraction(ChronoField.MICRO_OF_SECOND, 0, 6, true)
                .optionalEnd()
                .toFormatter();

        // Map pure FoodRecipeEntities to FoodRecipeCardResponse using ModelMapper
        List<FoodRecipeCardResponse> foodRecipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);


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
                    // Map basic properties from FoodSellEntity to FoodSellCardResponse
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);

                    // Set foodSellId directly from the sell entity's ID
                    response.setFoodSellId(sell.getId());

                    // Set sellerInformation by retrieving the seller details from FoodRecipeEntity
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
                .message("All foods fetched successfully by category for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    //Get all popular foods
    @Override
    public BaseResponse<?> getPopularFoods() {
        log.info("Fetching popular foods for guest user");

        // Fetch all food recipes ordered by average rating
        List<FoodRecipeEntity> popularRecipes = foodRecipeRepository.findAllByOrderByAverageRatingDesc();

        // Filter out recipes linked to food sells to get standalone popular recipes
        List<FoodRecipeEntity> purePopularRecipes = popularRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Fetch all food sells ordered by average rating
        List<FoodSellEntity> popularSells = foodSellRepository.findAllByOrderByAverageRatingDesc();

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define a flexible DateTimeFormatter to handle variable fractional seconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true) // For milliseconds
                .optionalEnd()
                .toFormatter();


        // Map pure popular FoodRecipeEntities to FoodRecipeCardResponse using ModelMapper
        List<FoodRecipeCardResponse> popularRecipeResponses = purePopularRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map popular FoodSellEntities to FoodSellCardResponse using ModelMapper
        List<FoodSellCardResponse> popularSellResponses = popularSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);

                    // Set foodSellId explicitly
                    response.setFoodSellId(sell.getId());

                    try {
                        // Parse dateCooking using the updated DateTimeFormatter
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

                    // Set other details from the recipe
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    return response;
                })
                .collect(Collectors.toList());

        if (popularRecipeResponses.isEmpty() && popularSellResponses.isEmpty()) {
            return BaseResponse.builder()
                    .message("No popular foods found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Prepare the response map
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("popularRecipes", popularRecipeResponses);
        responseMap.put("popularSells", popularSellResponses);

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("Popular foods fetched successfully for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    //Get food detail by ID and item type
    @Override
    public BaseResponse<?> getFoodDetail(Long id, ItemType itemType) {
        log.info("Fetching food detail for ID: {} and item type: {}", id, itemType);

        // Validate the id to ensure it's not null and is positive
        if (id == null || id <= 0) {
            throw new FieldBlankExceptionHandler("Food ID must be a positive number and cannot be null.");
        }

        // **Calculate rating percentages**
        LinkedHashMap<Integer, Double> ratingPercentages = calculateRatingPercentages(id, itemType);

        if (itemType == ItemType.FOOD_RECIPE) {
            // Fetch FoodRecipe details
            Optional<FoodRecipeEntity> recipeOptional = foodRecipeRepository.findById(Math.toIntExact(id));
            if (recipeOptional.isEmpty()) {
                log.error("FoodRecipe with ID {} not found", id);
                throw new NotFoundExceptionHandler("FoodRecipe with ID " + id + " not found");
            }

            FoodRecipeEntity foodRecipe = recipeOptional.get();
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
                throw new NotFoundExceptionHandler("FoodSell with ID " + id + " not found");
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

            // Set rating percentages
            foodSellResponse.setRatingPercentages(ratingPercentages);

            // Map additional foodRecipe details to FoodSellResponse
            foodSellResponse.setFoodRecipeDTO(foodRecipeDTO);

            return BaseResponse.builder()
                    .payload(foodSellResponse)
                    .message("FoodSell details fetched successfully")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();

        } else {
            log.error("Invalid item type: {}", itemType);
            return BaseResponse.builder()
                    .message("Invalid item type")
                    .statusCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                    .build();
        }
    }

    //Search foods by name
    @Override
    public BaseResponse<?> searchFoodsByName(String name) {
        log.info("Searching foods by name: {}", name);

        // Fetch all food recipes and food sells that match the name
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByNameContainingIgnoreCase(name);
        List<FoodSellEntity> foodSells = foodSellRepository.findByFoodRecipeNameContainingIgnoreCase(name);

        // Check if no records were found for the provided name
        if (foodRecipes.isEmpty() && foodSells.isEmpty()) {
            return BaseResponse.builder()
                    .message("No foods found for the specified name.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = foodRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Map pure food recipes to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> recipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

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

                    // Set the foodSellId
                    response.setFoodSellId(sell.getId());
                    // Set name food sell
                    response.setName(sell.getFoodRecipe().getName());

                    // Map photos
                    List<PhotoDTO> photoDTOs = sell.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set seller information
                    UserProfileDTO sellerInfo = UserProfileDTO.builder()
                            .userId(Long.valueOf(sell.getFoodRecipe().getUser().getId()))
                            .fullName(sell.getFoodRecipe().getUser().getFullName())
                            .phoneNumber(sell.getFoodRecipe().getUser().getPhoneNumber())
                            .profileImage(sell.getFoodRecipe().getUser().getProfileImage())
                            .location(sell.getFoodRecipe().getUser().getLocation())
                            .build();
                    response.setSellerInformation(sellerInfo);

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare the response
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodRecipes", recipeResponses);
        responseMap.put("foodSells", sellResponses);

        return BaseResponse.builder()
                .message("Search results fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    //Get food recipe by cuisine id
    @Override
    public BaseResponse<?> getFoodRecipeByCuisineID(Long cuisineId) {
        log.info("Fetching food recipes by cuisine ID: {} for guest user", cuisineId);

        // Validate the cuisineId to ensure it's not null and is positive
        if (cuisineId == null || cuisineId <= 0) {
            throw new FieldBlankExceptionHandler("Cuisine ID must be a positive number and cannot be null.");
        }

        // Check if the cuisineId exists in the database
        boolean cuisineExists = cuisineRepository.existsById(cuisineId);
        if (!cuisineExists) {
            throw new NotFoundExceptionHandler("Cuisine ID " + cuisineId + " not found.");
        }

        // Fetch all FoodRecipe entities by cuisine ID
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByCuisineId(cuisineId);

        // Check if no records were found for the provided cuisineId
        if (foodRecipes.isEmpty()) {
            log.warn("No FoodRecipe records found for cuisine ID: {}", cuisineId);
            return BaseResponse.builder()
                    .message("No food recipes found for the specified cuisine ID.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Map each FoodRecipeEntity to FoodRecipeCardResponse using ModelMapper
        List<FoodRecipeCardResponse> foodRecipeResponses = foodRecipes.stream()
                .map(foodRecipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(foodRecipe, FoodRecipeCardResponse.class);

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = foodRecipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set additional details such as name, average rating, and total raters
                    response.setName(foodRecipe.getName());
                    response.setAverageRating(foodRecipe.getAverageRating());
                    response.setTotalRaters(foodRecipe.getTotalRaters());

                    // Map user information to UserDTO if present
                    if (foodRecipe.getUser() != null) {
                        UserEntity user = foodRecipe.getUser();
                        UserDTO userDTO = new UserDTO(user.getId(), user.getFullName(), user.getProfileImage());
                        response.setUser(userDTO);
                    }

                    return response;
                })
                .collect(Collectors.toList());

        // Return the response with the list of FoodRecipeCardResponse objects
        return BaseResponse.builder()
                .message("Food recipes by cuisine ID fetched successfully for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodRecipeResponses)
                .build();
    }


    //Get food sell by cuisine id
    @Override
    public BaseResponse<?> getFoodSellByCuisineID(Long cuisineId) {
        log.info("Fetching food sells by cuisine ID: {} for guest user", cuisineId);

        // Validate the cuisineId to ensure it's not null and is positive
        if (cuisineId == null || cuisineId <= 0) {
            throw new FieldBlankExceptionHandler("Cuisine ID must be a positive number and cannot be null.");
        }

        // Check if the cuisineId exists in the database
        boolean cuisineExists = cuisineRepository.existsById(cuisineId);
        if (!cuisineExists) {
            throw new NotFoundExceptionHandler("Cuisine ID " + cuisineId + " not found.");
        }

        // Fetch all FoodSell entities by cuisine ID
        List<FoodSellEntity> foodSells = foodSellRepository.findByCuisineId(cuisineId);

        // Check if no records were found for the provided cuisineId
        if (foodSells.isEmpty()) {
            log.warn("No FoodSell records found for cuisine ID: {}", cuisineId);
            return BaseResponse.builder()
                    .message("No food sells found for the specified cuisine ID.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Map each FoodSellEntity to FoodSellCardResponse using ModelMapper
        List<FoodSellCardResponse> foodSellResponses = foodSells.stream()
                .map(foodSell -> {
                    FoodSellCardResponse response = modelMapper.map(foodSell, FoodSellCardResponse.class);

                    // Set foodSellId explicitly
                    response.setFoodSellId(foodSell.getId());

                    // Determine if the food is orderable based on the dateCooking
                    boolean isOrderable = !foodSell.getDateCooking().atZone(ZoneId.of("Asia/Phnom_Penh"))
                            .isBefore(currentDateTimeInPhnomPenh);
                    response.setIsOrderable(isOrderable);

                    // Map photos from the related FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = foodSell.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set additional details from the related FoodRecipeEntity
                    FoodRecipeEntity linkedRecipe = foodSell.getFoodRecipe();
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    // Map the seller's details
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

        // Return the response with the list of FoodSellCardResponse objects
        return BaseResponse.builder()
                .message("Food sells by cuisine ID fetched successfully for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodSellResponses)
                .build();
    }


    @Override
    public BaseResponse<?> getAllFoodName() {
        log.info("Fetching all unique food names for guest user");

        // Fetch all food names from FoodRecipe and FoodSell entities
        List<String> foodRecipeNames = foodRecipeRepository.findAll().stream()
                .map(FoodRecipeEntity::getName)
                .toList();

        List<String> foodSellNames = foodSellRepository.findAll().stream()
                .map(foodSell -> foodSell.getFoodRecipe().getName())
                .toList();

        // Combine both lists and filter for unique names
        Set<String> uniqueFoodNames = new HashSet<>(foodRecipeNames);
        uniqueFoodNames.addAll(foodSellNames);

        // Check if there are no unique food names
        if (uniqueFoodNames.isEmpty()) {
            log.info("No food names found in both FoodRecipe and FoodSell repositories.");
            return BaseResponse.builder()
                    .message("No food names found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Map unique names to FoodNameDTO with a list of food names
        FoodNameDTO foodNameDTO = new FoodNameDTO(new ArrayList<>(uniqueFoodNames));

        if (foodNameDTO.getFoodNames().isEmpty()) {
            return BaseResponse.builder()
                    .message("No unique food names found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Build the response with the unique food names
        return BaseResponse.builder()
                .message("Unique food names fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(foodNameDTO)
                .build();
    }


    @Override
    public BaseResponse<?> getAllFoods() {
        log.info("Fetching all foods for guest user");

        // Fetch all food recipes
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findAll();
        if (foodRecipes.isEmpty()) {
            return BaseResponse.builder()
                    .message("No food recipes found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = foodRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Fetch all food sells
        List<FoodSellEntity> foodSells = foodSellRepository.findAll();

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define a flexible DateTimeFormatter to handle variable fractional seconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true) // For milliseconds
                .optionalEnd()
                .toFormatter();

        // Map pure FoodRecipeEntities to FoodRecipeCardResponse with UserDTO
        List<FoodRecipeCardResponse> foodRecipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

                    // Map user info to UserDTO
                    UserDTO userDTO = new UserDTO(recipe.getUser().getId(), recipe.getUser().getFullName(), recipe.getUser().getProfileImage());
                    response.setUser(userDTO);

                    // Map photos from FoodRecipeEntity to structured list
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map FoodSellEntities to FoodSellCardResponse with UserDTO
        List<FoodSellCardResponse> foodSellResponses = foodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);
                    response.setFoodSellId(sell.getId());

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

                    // Set other details from the recipe
                    response.setName(linkedRecipe.getName());
                    response.setAverageRating(linkedRecipe.getAverageRating());
                    response.setTotalRaters(linkedRecipe.getTotalRaters());

                    return response;
                })
                .collect(Collectors.toList());

        if (foodRecipeResponses.isEmpty() && foodSellResponses.isEmpty()) {
            return BaseResponse.builder()
                    .message("No foods found.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Prepare the response map
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodRecipes", foodRecipeResponses);
        responseMap.put("foodSells", foodSellResponses);

        // Build and return the BaseResponse
        return BaseResponse.builder()
                .message("All foods fetched successfully for guest user")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    @Override
    public BaseResponse<?> searchFoodsRecipeByName(String name) {
        log.info("Searching food recipes by name: {}", name);

        // Fetch all food recipes that match the name
        List<FoodRecipeEntity> foodRecipes = foodRecipeRepository.findByNameContainingIgnoreCase(name);

        // Check if no records were found for the provided name
        if (foodRecipes.isEmpty()) {
            return BaseResponse.builder()
                    .message("No food recipes found for the specified name.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = foodRecipes.stream()
                .filter(recipe -> foodSellRepository.findByFoodRecipe(recipe).isEmpty())
                .toList();

        // Map pure food recipes to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> recipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

                    // Map photos
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare the response
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodRecipes", recipeResponses);

        return BaseResponse.builder()
                .message("Food recipe search results fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    @Override
    public BaseResponse<?> searchFoodsSellByName(String name) {
        log.info("Searching food sells by name: {}", name);

        // Fetch all food sells that match the name
        List<FoodSellEntity> foodSells = foodSellRepository.findByFoodRecipeNameContainingIgnoreCase(name);

        // Check if no records were found for the provided name
        if (foodSells.isEmpty()) {
            return BaseResponse.builder()
                    .message("No food sells found for the specified name.")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        // Map food sells to FoodSellCardResponse
        List<FoodSellCardResponse> sellResponses = foodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);

                    // Set the foodSellId
                    response.setFoodSellId(sell.getId());
                    // Set name from linked FoodRecipeEntity
                    response.setName(sell.getFoodRecipe().getName());

                    // Map photos from FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = sell.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set seller information
                    UserProfileDTO sellerInfo = UserProfileDTO.builder()
                            .userId(Long.valueOf(sell.getFoodRecipe().getUser().getId()))
                            .fullName(sell.getFoodRecipe().getUser().getFullName())
                            .phoneNumber(sell.getFoodRecipe().getUser().getPhoneNumber())
                            .profileImage(sell.getFoodRecipe().getUser().getProfileImage())
                            .location(sell.getFoodRecipe().getUser().getLocation())
                            .build();
                    response.setSellerInformation(sellerInfo);

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare the response
        Map<String, List<?>> responseMap = new HashMap<>();
        responseMap.put("foodSells", sellResponses);

        return BaseResponse.builder()
                .message("Food sell search results fetched successfully")
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