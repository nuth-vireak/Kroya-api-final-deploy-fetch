package com.kshrd.kroya_api.service.User;

import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.entity.token.TokenRepository;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.exception.constand.FieldBlankExceptionHandler;
import com.kshrd.kroya_api.exception.exceptionValidateInput.Validation;
import com.kshrd.kroya_api.payload.Auth.UserProfileUpdateRequest;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodRecipe.FoodRecipeCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.User.UserResponse;
import com.kshrd.kroya_api.repository.Address.AddressRepository;
import com.kshrd.kroya_api.repository.Credencials.CredentialRepository;
import com.kshrd.kroya_api.repository.DeviceToken.DeviceTokenRepository;
import com.kshrd.kroya_api.repository.Favorite.FavoriteRepository;
import com.kshrd.kroya_api.repository.FoodRecipe.FoodRecipeRepository;
import com.kshrd.kroya_api.repository.FoodSell.FoodSellRepository;
import com.kshrd.kroya_api.repository.User.UserRepository;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final FoodRecipeRepository foodRecipeRepository;
    private final FoodSellRepository foodSellRepository;
    private final TokenRepository tokenRepository;
    private final FavoriteRepository favoriteRepository;
    private final AddressRepository addressRepository;
    private final ModelMapper modelMapper;
    private final PasswordEncoder passwordEncoder;
    private final CredentialRepository credentialRepository;
    private final DeviceTokenRepository deviceTokenRepository;
    private final Validation validation;

    @Override
    public BaseResponse<?> getFoodsByCurrentUser() {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Fetching profile for user: {}", currentUser.getEmail());

        // Fetch all food recipes and food sells posted by the current user
        List<FoodRecipeEntity> userRecipes = foodRecipeRepository.findByUserId(currentUser.getId());
        List<FoodSellEntity> userFoodSells = foodSellRepository.findByFoodRecipeUserId(currentUser.getId());

        // Get a list of foodRecipe IDs that are already linked to food sells
        Set<Long> foodSellRecipeIds = userFoodSells.stream()
                .map(sell -> sell.getFoodRecipe().getId())
                .collect(Collectors.toSet());

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = userRecipes.stream()
                .filter(recipe -> !foodSellRecipeIds.contains(recipe.getId()))
                .collect(Collectors.toList());

        // Fetch the favorite food recipe and food sell IDs for the current user
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Count total posts
        int totalFoodRecipes = pureFoodRecipes.size();
        int totalFoodSells = userFoodSells.size();
        int totalPosts = totalFoodRecipes + totalFoodSells;

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define a flexible DateTimeFormatter to handle variable fractional seconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true) // For milliseconds
                .optionalEnd()
                .toFormatter();

        // Map pure food recipes (not linked to food sells) to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> recipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

                    // Set isFavorite based on user's favorite food recipes
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId()));

                    // Map photos
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map food sells to FoodSellCardResponse
        List<FoodSellCardResponse> sellResponses = userFoodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);
                    response.setFoodSellId(sell.getId());

                    try {
                        // Parse the dateCooking using the flexible DateTimeFormatter
                        LocalDateTime dateCooking = LocalDateTime.parse(sell.getDateCooking().toString(), formatter);

                        // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
                        ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));

                        // Update isOrderable based on whether dateCooking is expired or not
                        if (dateCookingZoned.isBefore(currentDateTimeInPhnomPenh)) {
                            sell.setIsOrderable(false);
                        } else {
                            sell.setIsOrderable(true);
                        }

                        // Save the updated isOrderable value to the database
                        foodSellRepository.save(sell);

                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse dateCooking: {}", sell.getDateCooking(), e);
                    }

                    // Set isFavorite based on user's favorite food sells
                    response.setIsFavorite(userFavoriteFoodSellIds.contains(sell.getId()));

                    // Map photos from the associated FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = sell.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set additional fields from FoodRecipeEntity
                    response.setName(sell.getFoodRecipe().getName());
                    response.setAverageRating(sell.getFoodRecipe().getAverageRating());
                    response.setTotalRaters(sell.getFoodRecipe().getTotalRaters());
                    response.setIsOrderable(sell.getIsOrderable());

                    // Map seller information from FoodRecipeEntity's user
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


        // Prepare response payload
        Map<String, Object> profileData = new LinkedHashMap<>();
        profileData.put("totalFoodRecipes", totalFoodRecipes);
        profileData.put("totalFoodSells", totalFoodSells);
        profileData.put("totalPosts", totalPosts);
        profileData.put("foodSells", sellResponses);
        profileData.put("foodRecipes", recipeResponses);

        // Return the response
        return BaseResponse.builder()
                .message("Profile fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(profileData)
                .build();
    }

    @Override
    public BaseResponse<?> getFoodsByUserId(Integer userId) {
        // Fetch user by userId
        Optional<UserEntity> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            log.error("User with ID {} not found", userId);
            return BaseResponse.builder()
                    .message("User not found")
                    .statusCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                    .build();
        }
        UserEntity user = userOptional.get();

        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Fetch all food recipes and food sells posted by this user
        List<FoodRecipeEntity> userRecipes = foodRecipeRepository.findByUserId(user.getId());
        List<FoodSellEntity> userFoodSells = foodSellRepository.findByFoodRecipeUserId(user.getId());

        // Get a list of foodRecipe IDs that are already linked to food sells
        Set<Long> foodSellRecipeIds = userFoodSells.stream()
                .map(sell -> sell.getFoodRecipe().getId())
                .collect(Collectors.toSet());

        // Filter out food recipes that are linked to food sells
        List<FoodRecipeEntity> pureFoodRecipes = userRecipes.stream()
                .filter(recipe -> !foodSellRecipeIds.contains(recipe.getId()))
                .collect(Collectors.toList());

        // Fetch the favorite food recipe and food sell IDs for the current user
        List<Long> userFavoriteRecipeIds = favoriteRepository.findByUserAndFoodRecipeIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodRecipe().getId())
                .toList();

        List<Long> userFavoriteFoodSellIds = favoriteRepository.findByUserAndFoodSellIsNotNull(currentUser)
                .stream()
                .map(favorite -> favorite.getFoodSell().getId())
                .toList();

        // Count total posts
        int totalFoodRecipes = pureFoodRecipes.size();
        int totalFoodSells = userFoodSells.size();
        int totalPosts = totalFoodRecipes + totalFoodSells;

        // Get the current time in Phnom Penh time zone (UTC+7)
        ZonedDateTime currentDateTimeInPhnomPenh = ZonedDateTime.now(ZoneId.of("Asia/Phnom_Penh"));

        // Define a flexible DateTimeFormatter to handle variable fractional seconds
        DateTimeFormatter formatter = new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd'T'HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.MILLI_OF_SECOND, 0, 3, true) // For milliseconds
                .optionalEnd()
                .toFormatter();

        // Map pure food recipes (not linked to food sells) to FoodRecipeCardResponse
        List<FoodRecipeCardResponse> recipeResponses = pureFoodRecipes.stream()
                .map(recipe -> {
                    FoodRecipeCardResponse response = modelMapper.map(recipe, FoodRecipeCardResponse.class);

                    // Set isFavorite based on user's favorite food recipes
                    response.setIsFavorite(userFavoriteRecipeIds.contains(recipe.getId()));

                    // Map photos
                    List<PhotoDTO> photoDTOs = recipe.getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    return response;
                })
                .collect(Collectors.toList());

        // Map food sells to FoodSellCardResponse
        List<FoodSellCardResponse> sellResponses = userFoodSells.stream()
                .map(sell -> {
                    FoodSellCardResponse response = modelMapper.map(sell, FoodSellCardResponse.class);

                    try {
                        // Parse the dateCooking using the flexible DateTimeFormatter
                        LocalDateTime dateCooking = LocalDateTime.parse(sell.getDateCooking().toString(), formatter);

                        // Convert LocalDateTime to ZonedDateTime in Phnom Penh time zone
                        ZonedDateTime dateCookingZoned = dateCooking.atZone(ZoneId.of("Asia/Phnom_Penh"));
                        log.info("dateCookingZoned: {}", dateCookingZoned);

                        // Update isOrderable based on whether dateCooking is expired or not
                        if (dateCookingZoned.isBefore(currentDateTimeInPhnomPenh)) {
                            sell.setIsOrderable(false);
                        } else {
                            sell.setIsOrderable(true);
                        }

                        // Save the updated isOrderable value to the database
                        foodSellRepository.save(sell);

                    } catch (DateTimeParseException e) {
                        log.error("Failed to parse dateCooking: {}", sell.getDateCooking(), e);
                    }

                    // Set isFavorite based on user's favorite food sells
                    response.setIsFavorite(userFavoriteFoodSellIds.contains(sell.getId()));

                    // Map photos from the associated FoodRecipeEntity
                    List<PhotoDTO> photoDTOs = sell.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList());
                    response.setPhoto(photoDTOs);

                    // Set additional fields from FoodRecipeEntity
                    response.setName(sell.getFoodRecipe().getName());
                    response.setAverageRating(sell.getFoodRecipe().getAverageRating());
                    response.setTotalRaters(sell.getFoodRecipe().getTotalRaters());
                    response.setIsOrderable(sell.getIsOrderable());

                    return response;
                })
                .collect(Collectors.toList());

        // Prepare response payload
        Map<String, Object> profileData = new LinkedHashMap<>();
        profileData.put("totalFoodRecipes", totalFoodRecipes);
        profileData.put("totalFoodSells", totalFoodSells);
        profileData.put("totalPosts", totalPosts);  // Total posts (recipes + sells)
        profileData.put("foodRecipes", recipeResponses);
        profileData.put("foodSells", sellResponses);

        // Return the response
        return BaseResponse.builder()
                .message("User profile fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(profileData)
                .build();
    }

    @Override
    public BaseResponse<?> updateProfile(UserProfileUpdateRequest profileUpdateRequest) {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Updating profile for user: {}", currentUser.getEmail());

        // Validate and update profile image if present
        if (profileUpdateRequest.getProfileImage() != null && !profileUpdateRequest.getProfileImage().trim().isEmpty()) {
            validation.validateNotBlank(profileUpdateRequest.getProfileImage(), "Profile Image");
            currentUser.setProfileImage(profileUpdateRequest.getProfileImage());
        }

        // Validate and update password if present and valid
        if (profileUpdateRequest.getPassword() != null && !profileUpdateRequest.getPassword().trim().isEmpty()) {
            if (profileUpdateRequest.getPassword().length() < 8) {
                throw new FieldBlankExceptionHandler("Password must be more than 8 characters.");
            }
            currentUser.setPassword(passwordEncoder.encode(profileUpdateRequest.getPassword()));
        }

        // Validate and update full name if present
        if (profileUpdateRequest.getFullName() != null && !profileUpdateRequest.getFullName().trim().isEmpty()) {
            validation.validateNotBlank(profileUpdateRequest.getFullName(), "Full Name");
            validation.validateUserName(profileUpdateRequest.getFullName());
            currentUser.setFullName(profileUpdateRequest.getFullName());
        }

        // Validate and update phone number if present
        if (profileUpdateRequest.getPhoneNumber() != null && !profileUpdateRequest.getPhoneNumber().trim().isEmpty()) {
            validation.validateNotBlank(profileUpdateRequest.getPhoneNumber(), "Phone Number");
            validation.validatePhoneNumber(profileUpdateRequest.getPhoneNumber());
            currentUser.setPhoneNumber(profileUpdateRequest.getPhoneNumber());
        }

        // Validate and update location if present
        if (profileUpdateRequest.getLocation() != null && !profileUpdateRequest.getLocation().trim().isEmpty()) {
            validation.validateNotBlank(profileUpdateRequest.getLocation(), "Location");
            currentUser.setLocation(profileUpdateRequest.getLocation());
        }

        // Save the updated user details
        userRepository.save(currentUser);
        log.info("Profile updated successfully for user: {}", currentUser.getEmail());

        // Map profile update request to response DTO
        UserProfileDTO userProfileDTO = new UserProfileDTO();
        userProfileDTO.setUserId(Long.valueOf(currentUser.getId()));
        modelMapper.map(profileUpdateRequest, userProfileDTO);

        return BaseResponse.builder()
                .payload(userProfileDTO)
                .message("Profile updated successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    @Transactional
    public BaseResponse<?> deleteAccount() {
        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        log.info("Deleting user account: {}", currentUser.getEmail());

        // Verify that the user exists in the database
        Optional<UserEntity> userOptional = userRepository.findById(currentUser.getId());
        if (userOptional.isEmpty()) {
            log.error("User with ID {} not found. Cannot proceed with deletion.", currentUser.getId());
            throw new NotFoundExceptionHandler("User account not found.");
        }

        // Proceed to delete the user from the database
        userRepository.deleteById(currentUser.getId());
        log.info("User account deleted successfully with ID: {}", currentUser.getId());

        // Return the response
        return BaseResponse.builder()
                .message("User account deleted successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    public BaseResponse<?> connectWebill(CredentialEntity credentialEntity) {

        // Ensure required fields are present
        validation.validateNotBlank(credentialEntity.getClientId(), "Client ID");
        validation.validateNotBlank(credentialEntity.getClientSecret(), "Client Secret");


        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = auth.getId();

        // Validate the existence of the user
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundExceptionHandler("User not found!"));

        // Check if the user already has connected credentials
        CredentialEntity credential = credentialRepository.findByUserId(userId);
        if (credential == null) {
            // Create new credentials if none exist
            credential = new CredentialEntity();
            credential.setUser(user);
        }

        // Set and save the updated credentials
        credential.setClientId(credentialEntity.getClientId());
        credential.setClientSecret(credentialEntity.getClientSecret());
        credentialRepository.save(credential);

        log.info("Webill account connected successfully for user ID: {}", userId);

        return BaseResponse.builder()
                .message("Connected Webill successfully!")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(modelMapper.map(user, UserDTO.class))
                .build();
    }

    @Override
    public BaseResponse<?> disconnectWebill() {
        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = auth.getId();

        // Verify that the user's Webill credentials exist in the repository
        Optional<CredentialEntity> credentialOptional = Optional.ofNullable(credentialRepository.findByUserId(userId));
        if (credentialOptional.isEmpty()) {
            throw new NotFoundExceptionHandler("Cannot disconnect - no linked Webill account found.");
        }

        // Delete the credentials if they exist
        CredentialEntity credential = credentialOptional.get();
        credentialRepository.deleteById(credential.getId());

        return BaseResponse.builder()
                .message("Disconnected successfully!")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    public BaseResponse<?> getCredentialByUserId(Integer userId) {
        // Validate that userId is not null
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null.");
        }

        // Retrieve credentials for the user
        Optional<CredentialEntity> credentialsOptional = Optional.ofNullable(credentialRepository.findByUserId(userId));

        // Check if credentials are present, else throw an exception
        if (credentialsOptional.isEmpty()) {
            throw new NotFoundExceptionHandler("Credentials not found for the provided user ID.");
        }

        return BaseResponse.builder()
                .message("Credentials fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(credentialsOptional.get())
                .build();
    }

    @Override
    public BaseResponse<?> getDeviceTokenByUserId(Integer userId) {

        if (deviceTokenRepository.findByUserId(userId) == null) {
            throw new NotFoundExceptionHandler("Device Token is not found!");
        }
        return BaseResponse.builder()
                .message("Device Token fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(deviceTokenRepository.findByUserId(userId))
                .build();
    }

    @Override
    public BaseResponse<?> insertDeviceToken(String deviceToken) {

        // Validate the deviceToken input
        validation.validateNotBlank(deviceToken, "Device Token");

        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = auth.getId();

        // Validate the user exists in the database
        Optional<UserEntity> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new NotFoundExceptionHandler("User not found");
        }

        UserEntity user = userOptional.get();

        // Check if a device token exists for this user and update or insert accordingly
        DeviceTokenEntity deviceTokenEntity = deviceTokenRepository.findByUserId(userId);
        if (deviceTokenEntity == null) {
            deviceTokenEntity = new DeviceTokenEntity();
            deviceTokenEntity.setUser(user);
        }

        // Update or set the device token
        deviceTokenEntity.setDeviceToken(deviceToken);
        deviceTokenRepository.save(deviceTokenEntity);

        return BaseResponse.builder()
                .message("Device token inserted successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(modelMapper.map(user, UserDTO.class))
                .build();
    }

    @Override
    public BaseResponse<?> getUserInfo() {

        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = auth.getId();

        if (userRepository.findById(userId).isEmpty()) {
            throw new NotFoundExceptionHandler("User not found!");
        }

        UserEntity user = userRepository.getById(userId);

        UserResponse userResponse = modelMapper.map(user, UserResponse.class);

        return BaseResponse.builder()
                .message("User info fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(userResponse)
                .build();
    }
}
