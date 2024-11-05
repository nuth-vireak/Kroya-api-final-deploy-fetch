package com.kshrd.kroya_api.service.Notification;

import com.kshrd.kroya_api.payload.BaseResponse;
import org.springframework.stereotype.Service;

@Service
public interface NotificationService {
    BaseResponse<?> getNotification(Integer pageNumber, Integer pageSize);
    BaseResponse<?> deletedNotification(Long id);
    BaseResponse<?> updateNotificationStatus(Long id);
}
