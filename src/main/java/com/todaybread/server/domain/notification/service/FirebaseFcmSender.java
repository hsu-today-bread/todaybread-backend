package com.todaybread.server.domain.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.todaybread.server.domain.notification.dto.FcmSendRequest;
import com.todaybread.server.domain.notification.dto.FcmSendResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Firebase Admin SDK를 통해 실제 FCM 메시지를 발송하는 구현체.
 */
@Slf4j
@RequiredArgsConstructor
public class FirebaseFcmSender implements FcmSender {

    private final FirebaseMessaging firebaseMessaging;

    @Override
    public FcmSendResult send(FcmSendRequest request) {
        try {
            Notification notification = Notification.builder()
                .setTitle(request.title())
                .setBody(request.body())
                .build();

            Message.Builder messageBuilder = Message.builder()
                .setToken(request.token())
                .setNotification(notification);

            if (request.data() != null && !request.data().isEmpty()) {
                messageBuilder.putAllData(request.data());
            }

            firebaseMessaging.send(messageBuilder.build());
            return FcmSendResult.success();
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();

            if (errorCode == MessagingErrorCode.UNREGISTERED) {
                return FcmSendResult.tokenExpired();
            }

            if (errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("not a valid FCM registration token")) {
                    return FcmSendResult.tokenExpired();
                }
                return FcmSendResult.failure(e.getMessage());
            }

            return FcmSendResult.failure(e.getMessage());
        } catch (Exception e) {
            return FcmSendResult.failure(e.getMessage());
        }
    }
}
