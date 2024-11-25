package com.kshrd.kroya_api.service.Notification;

import com.kshrd.kroya_api.entity.NotificationEntity;
import com.kshrd.kroya_api.entity.UserEntity;
import com.kshrd.kroya_api.enums.FoodCardType;
import com.kshrd.kroya_api.enums.ItemType;
import com.kshrd.kroya_api.exception.FieldEmptyExceptionHandler;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.Notification.NotificationResponse;
import com.kshrd.kroya_api.repository.Notification.NotificationRepository;
import com.kshrd.kroya_api.repository.User.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImp implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Override
    public BaseResponse<?> getNotification() {

        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer receiverId = auth.getId();

        // Fetch user by ID and handle the case where user is not found
        UserEntity userReceiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new FieldEmptyExceptionHandler("Receiver is not found!"));

        List<NotificationEntity> notifications = notificationRepository
                .findByReceiverOrderByCreatedDateDesc(userReceiver);

        if (notifications.isEmpty()) {
            throw new NotFoundExceptionHandler("No record");
        }

        // Convert NotificationEntity list to NotificationResponse list
        List<NotificationResponse> notificationResponses = notifications.stream().map(notification -> {
                    // Get FoodSellEntity and PurchaseEntity details for the response
                    Long purchaseId = notification.getRecipe() != null ? notification.getRecipe().getPurchase().getId() : null;
                    Long foodSellId = notification.getRecipe() != null ? notification.getRecipe().getPurchase().getFoodSell().getId() : null;
                    String foodPhoto = notification.getRecipe() != null && !notification.getRecipe().getPurchase().getFoodSell().getFoodRecipe().getPhotos().isEmpty()
                            ? notification.getRecipe().getPurchase().getFoodSell().getFoodRecipe().getPhotos().get(0).getPhoto()
                            : null;

                    // Determine FoodCardType based on the notification relationship between sender and receiver
                    assert notification.getRecipe() != null;
                    FoodCardType foodCardType = (notification.getReceiver().getId().equals(notification.getRecipe().getPurchase().getBuyer().getId()))
                            ? FoodCardType.ORDER   // For the buyer receiving an ORDER notification
                            : FoodCardType.SALE;   // For the seller receiving a SALE notification

                    return NotificationResponse.builder()
                            .notificationId(Long.valueOf(notification.getId()))
                            .purchaseId(purchaseId)
                            .foodSellId(foodSellId)
                            .description(notification.getDescription())
                            .isRead(notification.getIsRead())
                            .foodPhoto(foodPhoto)
                            .itemType(ItemType.FOOD_SELL)  // Defaulted as per the new response structure
                            .foodCardType(foodCardType)  // Assuming this is an order notification, adjust if needed
                            .purchaseStatusType(notification.getRecipe().getPurchase().getPurchaseStatusType())
                            .createdDate(notification.getCreatedDate())
                            .build();
                }
        ).collect(Collectors.toList());

        return BaseResponse.builder()
                .payload(notificationResponses)
                .message("Notification fetched successfully!")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    public BaseResponse<?> deletedNotification(Long id) {

        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = auth.getId();

        // Check if the notification exists and belongs to the user
        NotificationEntity notification = notificationRepository.findById(id)
                .filter(n -> n.getReceiver().getId().equals(userId))
                .orElseThrow(() -> new FieldEmptyExceptionHandler("Allow deleted only own user!"));

        // Delete the notification
        notificationRepository.delete(notification);

        // Return a success response
        return BaseResponse.builder()
                .message("Notification deleted successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }

    @Override
    public BaseResponse<?> updateNotificationStatus(Long id) {

        UserEntity auth = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Integer userId = auth.getId();

        // Check if the notification exists and belongs to the user
        NotificationEntity notification = notificationRepository.findById(id)
                .filter(n -> n.getReceiver().getId().equals(userId))
                .orElseThrow(() -> new FieldEmptyExceptionHandler("Allow updated only own user!"));

        // Update the notification status
        notification.setIsRead(true);
        notificationRepository.save(notification);

        // Return a success response
        return BaseResponse.builder()
                .message("Notification status updated successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }
}
