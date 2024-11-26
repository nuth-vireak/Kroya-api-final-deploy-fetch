package com.kshrd.kroya_api.service.Notification;

import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PushNotificationService {
    private final Logger logger = LoggerFactory.getLogger(PushNotificationService.class);
    private final FirebaseMessaging firebaseMessaging;

    public void sendNotification(String token, String title, String messageBody) {
        Notification notification = Notification.builder()
                .setTitle(title)
                .setBody(messageBody)
                .build();

        Message message = Message.builder()
                .setToken(token)
                .setNotification(notification)
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(Aps.builder()
                                .setSound("default") // Add this for sound
                                .build())
                        .build())
                .build();

        try {
            String response = firebaseMessaging.send(message);
            logger.info("Notification sent successfully. Response: " + response);
        } catch (Exception e) {
            logger.error("Failed to send FCM notification: ", e);
        }
    }
}
