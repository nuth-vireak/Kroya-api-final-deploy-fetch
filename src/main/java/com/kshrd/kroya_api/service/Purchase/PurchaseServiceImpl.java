package com.kshrd.kroya_api.service.Purchase;

import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.enums.*;
import com.kshrd.kroya_api.exception.FieldEmptyExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodSell.BuyerOrderCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.FoodSell.SellerOrderCardResponse;
import com.kshrd.kroya_api.payload.Purchase.PurchaseRequest;
import com.kshrd.kroya_api.payload.Purchase.PurchaseResponse;
import com.kshrd.kroya_api.payload.Receipt.ReceiptResponse;
import com.kshrd.kroya_api.repository.Address.AddressRepository;
import com.kshrd.kroya_api.repository.DeviceToken.DeviceTokenRepository;
import com.kshrd.kroya_api.repository.Favorite.FavoriteRepository;
import com.kshrd.kroya_api.repository.FoodSell.FoodSellRepository;
import com.kshrd.kroya_api.repository.Notification.NotificationRepository;
import com.kshrd.kroya_api.repository.Purchase.PurchaseRepository;
import com.kshrd.kroya_api.repository.Receipt.RecipeRepository;
import com.kshrd.kroya_api.repository.User.UserRepository;
import com.kshrd.kroya_api.service.Notification.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseServiceImpl implements PurchaseService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FoodSellRepository foodSellRepository;
    private final PurchaseRepository purchaseRepository;
    private final RecipeRepository recipeRepository;
    private final ModelMapper modelMapper;
    private final FavoriteRepository favoriteRepository;
    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;
    private final DeviceTokenRepository deviceTokenRepository;

    @Override
    public BaseResponse<?> addPurchase(PurchaseRequest purchaseRequest, PaymentType paymentType) {
        UserEntity buyer = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer id = buyer.getId();

        // Fetch the FoodSellEntity and associated seller information
        FoodSellEntity product = foodSellRepository.getById(purchaseRequest.getFoodSellId());
        UserEntity seller = userRepository.getById(product.getFoodRecipe().getUser().getId());

        // Check if buyer is attempting to purchase their own product
        if (id.equals(product.getFoodRecipe().getUser().getId())) {
            throw new FieldEmptyExceptionHandler("Cannot order your own product!");
        }

        // Ensure quantity and totalPrice are not null
        Integer quantity = purchaseRequest.getQuantity();
        Double totalPrice = purchaseRequest.getTotalPrice();
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be provided and greater than zero.");
        }
        if (totalPrice == null || totalPrice <= 0) {
            throw new IllegalArgumentException("Total price must be provided and greater than zero.");
        }

        // Create and save PurchaseEntity
        PurchaseEntity purchaseEntity = PurchaseEntity.builder()
                .buyer(buyer)
                .foodSell(product)
                .location(purchaseRequest.getLocation())
                .paymentType(paymentType)
                .remark(purchaseRequest.getRemark())
                .purchaseStatusType(PurchaseStatusType.PENDING)
                .createdDate(LocalDateTime.now())
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();

        purchaseEntity = purchaseRepository.save(purchaseEntity);

        // Create and save RecipeEntity1 for the receipt
        RecipeEntity recipeEntity = new RecipeEntity();
        recipeEntity.setPaidBy(paymentType);
        recipeEntity.setPaidDate(LocalDateTime.now());
        recipeEntity.setReference(RandomStringUtils.randomAlphanumeric(8));
        recipeEntity.setPurchase(purchaseEntity);
        recipeEntity = recipeRepository.save(recipeEntity);

        // Prepare FoodSellCardResponse for the purchased item
        FoodSellCardResponse foodSellResponse = FoodSellCardResponse.builder()
                .foodSellId(product.getId())
                .photo(product.getFoodRecipe().getPhotos().stream()
                        .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                        .collect(Collectors.toList()))
                .name(product.getFoodRecipe().getName())
                .dateCooking(product.getDateCooking())
                .price(product.getPrice())
                .currencyType(CurrencyType.valueOf(product.getCurrencyType()))
                .averageRating(product.getFoodRecipe().getAverageRating())
                .totalRaters(product.getFoodRecipe().getTotalRaters())
                .isFavorite(favoriteRepository.existsByUserAndFoodSell(buyer, product))
                .isOrderable(product.getIsOrderable())
                .sellerInformation(UserProfileDTO.builder()
                        .userId(Long.valueOf(seller.getId()))
                        .fullName(seller.getFullName())
                        .phoneNumber(seller.getPhoneNumber())
                        .profileImage(seller.getProfileImage())
                        .location(seller.getLocation())
                        .build())
                .build();

        // Create ReceiptResponse with details for UI
        ReceiptResponse receiptResponse = ReceiptResponse.builder()
                .recipeId(purchaseEntity.getId())
                .purchaseId(purchaseEntity.getId())
                .foodSellCardResponse(foodSellResponse)
                .reference(recipeEntity.getReference())
                .orderDate(recipeEntity.getPaidDate())
                .paidBy(paymentType)
                .payer(buyer.getFullName())
                .seller(seller.getFullName())
                .sellerPhoneNumber(seller.getPhoneNumber())
                .quantity(quantity)
                .totalPrice(totalPrice)
                .build();

        // Create notification for the seller
        String descriptionForSeller = String.format("%s ordered %s. Please prepare the meal.", buyer.getFullName(), product.getFoodRecipe().getName());
        NotificationEntity sellerNotification = NotificationEntity.builder()
                .recipe(recipeEntity)
                .description(descriptionForSeller)
                .receiver(seller)
                .sender(buyer)
                .isRead(false)
                .createdDate(LocalDateTime.now())
                .build();
        notificationRepository.save(sellerNotification);

        // Create notification for the buyer
        String descriptionForBuyer = String.format("Your order for %s has been placed successfully.", product.getFoodRecipe().getName());
        NotificationEntity buyerNotification = NotificationEntity.builder()
                .recipe(recipeEntity)
                .description(descriptionForBuyer)
                .receiver(buyer)
                .sender(seller)
                .isRead(false)
                .createdDate(LocalDateTime.now())
                .build();
        notificationRepository.save(buyerNotification);

        // Retrieve FCM device tokens from DeviceTokenRepository
        DeviceTokenEntity sellerDeviceTokenEntity = deviceTokenRepository.findByUser(seller);
        DeviceTokenEntity buyerDeviceTokenEntity = deviceTokenRepository.findByUser(buyer);

        if (sellerDeviceTokenEntity != null) {
            String sellerToken = sellerDeviceTokenEntity.getDeviceToken();
            // Send FCM notification to seller
            String sellerTitle = "New Order Received";
            String sellerMessage = String.format("%s ordered %s. Please prepare the meal.", buyer.getFullName(), product.getFoodRecipe().getName());
            pushNotificationService.sendNotification(sellerToken, sellerTitle, sellerMessage);
        }

        if (buyerDeviceTokenEntity != null) {
            String buyerToken = buyerDeviceTokenEntity.getDeviceToken();
            // Send FCM notification to buyer
            String buyerTitle = "Order Placed Successfully";
            String buyerMessage = String.format("Your order for %s has been placed successfully.", product.getFoodRecipe().getName());
            pushNotificationService.sendNotification(buyerToken, buyerTitle, buyerMessage);
        }


//        // Retrieve FCM device tokens from DeviceTokenRepository
//        String sellerToken = deviceTokenRepository.findByUser(seller).getDeviceToken();
//        String buyerToken = deviceTokenRepository.findByUser(buyer).getDeviceToken();
//
//        // Send FCM notification to seller
//        String sellerTitle = "New Order Received";
//        String sellerMessage = String.format("%s ordered %s. Please prepare the meal.", buyer.getFullName(), product.getFoodRecipe().getName());
//        pushNotificationService.sendNotification(sellerToken, sellerTitle, sellerMessage);
//
//        // Send FCM notification to buyer
//        String buyerTitle = "Order Placed Successfully";
//        String buyerMessage = String.format("Your order for %s has been placed successfully.", product.getFoodRecipe().getName());
//        pushNotificationService.sendNotification(buyerToken, buyerTitle, buyerMessage);

//        if (sellerDeviceTokenEntity != null) {
//            String sellerToken = sellerDeviceTokenEntity.getDeviceToken();
//            // Send FCM notification to seller
//            String sellerTitle = "ការកម្មង់ថ្មីបានមកដល់!";
//            String sellerMessage = String.format("%s បានកម្មង់ %s។ សូមរៀបចំម្ហូបឱ្យបានរួសរាន់។", buyer.getFullName(), product.getFoodRecipe().getName());
//            pushNotificationService.sendNotification(sellerToken, sellerTitle, sellerMessage);
//        }
//
//        if (buyerDeviceTokenEntity != null) {
//            String buyerToken = buyerDeviceTokenEntity.getDeviceToken();
//            // Send FCM notification to buyer
//            String buyerTitle = "ការកម្មង់បានជោគជ័យ";
//            String buyerMessage = String.format("ការកម្មង់របស់អ្នកសម្រាប់ %s បានបញ្ចប់ដោយជោគជ័យ! សូមរង់ចាំម្ហូបរបស់អ្នកនៅក្នុងពេលឆាប់ៗនេះ។", product.getFoodRecipe().getName());
//            pushNotificationService.sendNotification(buyerToken, buyerTitle, buyerMessage);
//        }



        return BaseResponse.builder()
                .message("Purchase successfully!")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(receiptResponse)
                .build();
    }

    @Override
    public BaseResponse<?> getAllOrdersForSeller(Long foodSellId) {

        UserEntity seller = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

//        List<PurchaseEntity> orders = purchaseRepository.findByFoodSell_FoodRecipe_User_Id(seller.getId());

        // Fetch orders for the specific foodSellId sold by the authenticated seller
        List<PurchaseEntity> orders = purchaseRepository.findByFoodSell_IdAndFoodSell_FoodRecipe_User_Id(foodSellId, seller.getId());

        List<PurchaseResponse> orderResponses = orders.stream().map(order -> {
            FoodSellEntity product = order.getFoodSell();
            FoodSellCardResponse foodSellResponse = FoodSellCardResponse.builder()
                    .foodSellId(product.getId())
                    .photo(product.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList()))
                    .name(product.getFoodRecipe().getName())
                    .dateCooking(product.getDateCooking())
                    .price(product.getPrice())
                    .averageRating(product.getFoodRecipe().getAverageRating())
                    .totalRaters(product.getFoodRecipe().getTotalRaters())
                    .isFavorite(favoriteRepository.existsByUserAndFoodSell(order.getBuyer(), product))
                    .isOrderable(product.getIsOrderable())
                    .sellerInformation(UserProfileDTO.builder()
                            .userId(Long.valueOf(seller.getId()))
                            .fullName(seller.getFullName())
                            .phoneNumber(seller.getPhoneNumber())
                            .profileImage(seller.getProfileImage())
                            .location(seller.getLocation())
                            .build())
                    .build();

            return PurchaseResponse.builder()
                    .purchaseId(order.getId())
                    .foodSellCardResponse(foodSellResponse)
                    .remark(order.getRemark())
                    .location(order.getLocation())
                    .paymentType(order.getPaymentType())
                    .purchaseStatusType(order.getPurchaseStatusType())
                    .purchaseDate(order.getCreatedDate())
                    .quantity(order.getQuantity())
                    .totalPrice(order.getTotalPrice())
                    .buyerInformation(UserProfileDTO.builder()
                            .userId(Long.valueOf(order.getBuyer().getId()))
                            .fullName(order.getBuyer().getFullName())
                            .phoneNumber(order.getBuyer().getPhoneNumber())
                            .profileImage(order.getBuyer().getProfileImage())
                            .location(order.getBuyer().getLocation())
                            .build())
                    .build();
        }).toList();

        return BaseResponse.builder()
                .message("Order requests retrieved successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(orderResponses)
                .build();
    }

    @Override
    public BaseResponse<?> getOrdersForBuyer() {
        UserEntity buyer = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<PurchaseEntity> orders = purchaseRepository.findByBuyer_Id(buyer.getId());

        List<BuyerOrderCardResponse> orderResponses = orders.stream().map(order -> {
            FoodSellEntity product = order.getFoodSell();

            return BuyerOrderCardResponse.builder()
                    .purchaseId(order.getId())
                    .foodSellId(product.getId())
                    .name(product.getFoodRecipe().getName())
                    .photo(product.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList()))
                    .quantity(order.getQuantity())
                    .totalPrice(order.getTotalPrice())
                    .dateCooking(product.getDateCooking())
                    .isOrderable(product.getIsOrderable())
                    .itemType(ItemType.FOOD_SELL)
                    .foodCardType(FoodCardType.ORDER)
                    .purchaseStatusType(order.getPurchaseStatusType())
                    .purchaseDate(order.getCreatedDate())
                    .build();
        }).collect(Collectors.toList());

        return BaseResponse.builder()
                .message("Order history retrieved successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(orderResponses)
                .build();
    }

    @Override
    public BaseResponse<?> updateOrderStatus(Long purchaseId, PurchaseStatusType newStatus) {
        UserEntity seller = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        PurchaseEntity order = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new FieldEmptyExceptionHandler("Order not found!"));

        RecipeEntity recipeEntity = recipeRepository.findByPurchase_Id(purchaseId)
                .orElseThrow(() -> new FieldEmptyExceptionHandler("Receipt not found for the given purchase ID."));

        if (!order.getFoodSell().getFoodRecipe().getUser().getId().equals(seller.getId())) {
            throw new FieldEmptyExceptionHandler("You are not authorized to update this order's status.");
        }

        order.setPurchaseStatusType(newStatus);
        purchaseRepository.save(order);

        FoodSellEntity product = order.getFoodSell();
        FoodRecipeEntity foodRecipe = product.getFoodRecipe();
        UserEntity productSeller = foodRecipe.getUser();
        UserEntity buyer = order.getBuyer();

        // Build FoodSellCardResponse manually
        FoodSellCardResponse foodSellResponse = FoodSellCardResponse.builder()
                .foodSellId(product.getId())
                .photo(product.getFoodRecipe().getPhotos().stream()
                        .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                        .collect(Collectors.toList()))
                .name(product.getFoodRecipe().getName())
                .dateCooking(product.getDateCooking())
                .price(product.getPrice())
                .averageRating(product.getFoodRecipe().getAverageRating())
                .totalRaters(product.getFoodRecipe().getTotalRaters())
                .isFavorite(favoriteRepository.existsByUserAndFoodSell(order.getBuyer(), product))
                .isOrderable(product.getIsOrderable())
                .sellerInformation(UserProfileDTO.builder()
                        .userId(Long.valueOf(productSeller.getId()))
                        .fullName(productSeller.getFullName())
                        .phoneNumber(productSeller.getPhoneNumber())
                        .profileImage(productSeller.getProfileImage())
                        .location(productSeller.getLocation())
                        .build())
                .build();

        PurchaseResponse response = PurchaseResponse.builder()
                .purchaseId(order.getId())
                .foodSellCardResponse(foodSellResponse)
                .remark(order.getRemark())
                .location(order.getLocation())
                .paymentType(order.getPaymentType())
                .purchaseStatusType(order.getPurchaseStatusType())
                .quantity(order.getQuantity())
                .totalPrice(order.getTotalPrice())
                .buyerInformation(UserProfileDTO.builder()
                        .userId(Long.valueOf(order.getBuyer().getId()))
                        .fullName(order.getBuyer().getFullName())
                        .phoneNumber(order.getBuyer().getPhoneNumber())
                        .profileImage(order.getBuyer().getProfileImage())
                        .location(order.getBuyer().getLocation())
                        .build())
                .build();

        String descriptionForBuyer = "";
        String titleForBuyer = "";
        if (newStatus == PurchaseStatusType.ACCEPTED) {
            titleForBuyer = String.format("Order Accepted");
            descriptionForBuyer = String.format("%s accepted your order, please wait for the meal preparation.", seller.getFullName());

        }
        if (newStatus == PurchaseStatusType.REJECTED) {
            titleForBuyer = String.format("Order Rejected");
            descriptionForBuyer = String.format("%s rejected your order. You can order again later.", seller.getFullName());

        }
        if (newStatus == PurchaseStatusType.PENDING) {
            titleForBuyer = String.format("Your order is Pending");
            descriptionForBuyer = String.format("Your order is pending. %s will review it soon.", seller.getFullName());
        }
//        String descriptionForBuyer = "";
//        String titleForBuyer = "";
//        if (newStatus == PurchaseStatusType.ACCEPTED) {
//            titleForBuyer = String.format("%s បានទទួលការកម្មង់របស់អ្នក!", seller.getFullName());
//            descriptionForBuyer = "ម្ហូបរបស់អ្នកកំពុងត្រូវរៀបចំ! សូមរង់ចាំឱ្យមួយភ្លែត";
//        }
//        if (newStatus == PurchaseStatusType.REJECTED) {
//            titleForBuyer = String.format("%s បានបដិសេធការកម្មង់របស់អ្នក", seller.getFullName());
//            descriptionForBuyer = "កុំបារម្ភ! អ្នកអាចសាកល្បងម្ដងទៀតនៅពេលក្រោយ";
//        }
//        if (newStatus == PurchaseStatusType.PENDING) {
//            titleForBuyer = "ការកម្មង់របស់អ្នកកំពុងរង់ចាំការពិនិត្យ!";
//            descriptionForBuyer = String.format("%s កំពុងពិនិត្យមើលការកម្មង់របស់អ្នក, សូមរង់ចាំបន្តិចទៀត", seller.getFullName());
//        }

        NotificationEntity buyerNotification = NotificationEntity.builder()
                .recipe(recipeEntity)
                .description(descriptionForBuyer)
                .receiver(order.getBuyer())
                .sender(seller)
                .isRead(false)
                .createdDate(LocalDateTime.now())
                .build();
        notificationRepository.save(buyerNotification);

        // Push Notification
        DeviceTokenEntity buyerDeviceTokenEntity = deviceTokenRepository.findByUser(buyer);
        if (buyerDeviceTokenEntity != null) {
            String buyerToken = buyerDeviceTokenEntity.getDeviceToken();
            // seller name accepted your order.
            pushNotificationService.sendNotification(buyerToken, titleForBuyer, descriptionForBuyer);
        }

//        // Retrieve FCM device tokens from DeviceTokenRepository
//        String buyerToken = deviceTokenRepository.findByUser(buyer).getDeviceToken();
//
//        // Send FCM notification to seller
//        String sellerTitle = "New Order Received";
//        String sellerMessage = String.format("%s ordered %s. Please prepare the meal.", buyer.getFullName(), product.getFoodRecipe().getName());
//        pushNotificationService.sendNotification(sellerToken, sellerTitle, sellerMessage);
//
//        // Send FCM notification to buyer
//        String buyerTitle = "Order Placed Successfully";
//        String buyerMessage = String.format("Your order for %s has been placed successfully.", product.getFoodRecipe().getName());
//        pushNotificationService.sendNotification(buyerToken, buyerTitle, buyerMessage);

        return BaseResponse.builder()
                .message("Order status updated successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(response)
                .build();
    }

    @Override
    public BaseResponse<?> getSellerItemsWithOrderCounts() {
        UserEntity seller = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Fetch all food items sold by the seller
        List<FoodSellEntity> sellerItems = foodSellRepository.findByFoodRecipe_User_Id(seller.getId());

        // Map each food item to SellerOrderCardResponse including order count
        List<SellerOrderCardResponse> responses = sellerItems.stream().map(item -> {
            int orderCount = purchaseRepository.countByFoodSell_Id(item.getId());

            return SellerOrderCardResponse.builder()
                    .foodSellId(item.getId())
                    .name(item.getFoodRecipe().getName())
                    .price(item.getPrice())
                    .orderCount(orderCount)
                    .photo(item.getFoodRecipe().getPhotos().stream()
                            .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                            .collect(Collectors.toList()))
                    .dateCooking(item.getDateCooking())
                    .isOrderable(item.getIsOrderable())
                    .itemType(ItemType.FOOD_SELL)
                    .foodCardType(FoodCardType.SALE)
                    .build();
        }).collect(Collectors.toList());

        return BaseResponse.builder()
                .message("Seller items with order counts retrieved successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responses)
                .build();
    }

    @Override
    public BaseResponse<?> getAllOrdersAndSales() {
        UserEntity user = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = user.getId();

        List<Object> responses = new ArrayList<>();

        // Fetch items sold by the user (SALE items)
        List<SellerOrderCardResponse> saleItems = foodSellRepository.findByFoodRecipe_User_Id(userId)
                .stream()
                .map(item -> {
                    int orderCount = purchaseRepository.countByFoodSell_Id(item.getId());
                    return SellerOrderCardResponse.builder()
                            .foodSellId(item.getId())
                            .name(item.getFoodRecipe().getName())
                            .price(item.getPrice())
                            .orderCount(orderCount)
                            .photo(item.getFoodRecipe().getPhotos().stream()
                                    .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                                    .collect(Collectors.toList()))
                            .dateCooking(item.getDateCooking())
                            .isOrderable(item.getIsOrderable())
                            .itemType(ItemType.FOOD_SELL)
                            .foodCardType(FoodCardType.SALE)
                            .build();
                })
                .collect(Collectors.toList());

        // Fetch items ordered by the user (ORDER items)
        List<BuyerOrderCardResponse> orderItems = purchaseRepository.findByBuyer_Id(userId)
                .stream()
                .map(order -> {
                    FoodSellEntity product = order.getFoodSell();
                    return BuyerOrderCardResponse.builder()
                            .purchaseId(order.getId())
                            .foodSellId(product.getId())
                            .name(product.getFoodRecipe().getName())
                            .photo(product.getFoodRecipe().getPhotos().stream()
                                    .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                                    .collect(Collectors.toList()))
                            .quantity(order.getQuantity())
                            .totalPrice(order.getTotalPrice())
                            .dateCooking(product.getDateCooking())
                            .isOrderable(product.getIsOrderable())
                            .itemType(ItemType.FOOD_SELL)
                            .foodCardType(FoodCardType.ORDER)
                            .purchaseStatusType(order.getPurchaseStatusType())
                            .purchaseDate(order.getCreatedDate())
                            .build();
                })
                .collect(Collectors.toList());

        // Combine both SALE and ORDER items into a single list
        responses.addAll(saleItems);
        responses.addAll(orderItems);

        return BaseResponse.builder()
                .message("All items retrieved successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responses)
                .build();
    }

    @Override
    public BaseResponse<?> searchFoodSellInPurchaseByName(String name) {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = currentUser.getId();

        List<Object> responses = new ArrayList<>();

        // Sale items mapping (SellerOrderCardResponse)
        List<SellerOrderCardResponse> saleItems = foodSellRepository.findByFoodRecipe_User_IdAndFoodRecipe_NameContainingIgnoreCase(userId, name)
                .stream()
                .map(foodSellEntity -> {
                    // Cast and map to SellerOrderCardResponse
                    FoodSellEntity foodSell = (FoodSellEntity) foodSellEntity;  // Ensure it's cast to FoodSellEntity

                    int orderCount = purchaseRepository.countByFoodSell_Id(foodSell.getId());

                    return SellerOrderCardResponse.builder()
                            .foodSellId(foodSell.getId())
                            .name(foodSell.getFoodRecipe().getName())
                            .price(foodSell.getPrice())
                            .orderCount(orderCount)
                            .photo(foodSell.getFoodRecipe().getPhotos().stream()
                                    .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                                    .collect(Collectors.toList()))
                            .dateCooking(foodSell.getDateCooking())
                            .isOrderable(foodSell.getIsOrderable())
                            .itemType(ItemType.FOOD_SELL)
                            .foodCardType(FoodCardType.SALE)
                            .build();
                })
                .toList();
        responses.addAll(saleItems);

        // Order items mapping (BuyerOrderCardResponse)
        List<BuyerOrderCardResponse> orderItems = purchaseRepository.findByBuyer_IdAndFoodSell_FoodRecipe_NameContainingIgnoreCase(userId, name)
                .stream()
                .map(purchaseEntity -> {
                    // Cast and map to BuyerOrderCardResponse
                    PurchaseEntity purchase = (PurchaseEntity) purchaseEntity;  // Ensure it's cast to PurchaseEntity
                    FoodSellEntity foodSell = purchase.getFoodSell();

                    return BuyerOrderCardResponse.builder()
                            .purchaseId(purchase.getId())
                            .foodSellId(foodSell.getId())
                            .name(foodSell.getFoodRecipe().getName())
                            .photo(foodSell.getFoodRecipe().getPhotos().stream()
                                    .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                                    .collect(Collectors.toList()))
                            .quantity(purchase.getQuantity())
                            .totalPrice(purchase.getTotalPrice())
                            .dateCooking(foodSell.getDateCooking())
                            .isOrderable(foodSell.getIsOrderable())
                            .itemType(ItemType.FOOD_SELL)
                            .foodCardType(FoodCardType.ORDER)
                            .purchaseStatusType(purchase.getPurchaseStatusType())
                            .purchaseDate(purchase.getCreatedDate())
                            .build();
                })
                .toList();
        responses.addAll(orderItems);

        // Return the search results as a response
        return BaseResponse.builder()
                .message("Search results retrieved successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responses)
                .build();
    }
}
