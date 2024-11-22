package com.kshrd.kroya_api.controller;

import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.service.Notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(
            summary = "🔔 Retrieve Notifications",
            description = """
                    Fetches a paginated list of notifications for the currently authenticated user.
                    
                    **Request Parameters**:
                    - **pageNumber** (Integer): The page number to retrieve.
                    - **pageSize** (Integer): The number of notifications per page.
                    
                    **Response Summary**:
                    - **200**: ✅ Notifications retrieved successfully.
                    - **404**: 🚫 No notifications found.
                    """
    )
    @GetMapping("")
    public BaseResponse<?> getNotification() {
        return notificationService.getNotification();
    }

    @Operation(
            summary = "🗑️ Delete Notification",
            description = """
                    Deletes a specific notification based on its ID.
                    
                    **Path Variable**:
                    - **id** (Long): The ID of the notification to delete.
                    
                    **Response Summary**:
                    - **200**: ✅ Notification deleted successfully.
                    - **404**: 🚫 Notification not found.
                    - **401**: 🚫 Unauthorized access if the user does not own the notification.
                    """
    )
    @DeleteMapping("/{id}")
    public BaseResponse<?> DeletedNotification(@PathVariable("id") Long id) {
        return notificationService.deletedNotification(id);
    }

    @Operation(
            summary = "✔️ Update Notification Status",
            description = """
                    Updates the read status of a specific notification.
                    
                    **Path Variable**:
                    - **id** (Long): The ID of the notification to update.
                    
                    **Response Summary**:
                    - **200**: ✅ Notification status updated successfully.
                    - **404**: 🚫 Notification not found.
                    - **401**: 🚫 Unauthorized access if the user does not own the notification.
                    """
    )
    @PutMapping("/{id}")
    public BaseResponse<?> updateNotificationStatus(@PathVariable("id") Long id) {
        return notificationService.updateNotificationStatus(id);
    }
}

