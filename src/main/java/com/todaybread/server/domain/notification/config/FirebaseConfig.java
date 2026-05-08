package com.todaybread.server.domain.notification.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.todaybread.server.domain.notification.service.FcmSender;
import com.todaybread.server.domain.notification.service.FirebaseFcmSender;
import com.todaybread.server.domain.notification.service.NoopFcmSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.InputStream;

/**
 * Firebase Admin SDK 설정 클래스.
 *
 * <p>단일 {@code @Bean} 메서드에서 설정값을 확인하고 Firebase를 초기화한다.
 * {@code @ConditionalOnProperty}, {@code @ConditionalOnMissingBean} 패턴을 사용하지 않는다.
 * 초기화 실패 시 반드시 {@link NoopFcmSender}로 fallback하여 서버가 죽지 않도록 보장한다.
 */
@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${notification.fcm.enabled:false}")
    private boolean enabled;

    @Value("${notification.fcm.credentials-path:}")
    private String credentialsPath;

    @Bean
    public FcmSender fcmSender() {
        if (!enabled) {
            log.info("FCM disabled by configuration. Using NoopFcmSender.");
            return new NoopFcmSender();
        }

        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("FCM enabled but credentials-path is blank. Using NoopFcmSender.");
            return new NoopFcmSender();
        }

        try {
            InputStream serviceAccount = new FileInputStream(credentialsPath);
            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();
            FirebaseApp app = FirebaseApp.initializeApp(options);
            FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);
            log.info("Firebase Admin SDK initialized successfully.");
            return new FirebaseFcmSender(messaging);
        } catch (Exception e) {
            log.error("Firebase Admin SDK initialization failed. Falling back to NoopFcmSender.", e);
            return new NoopFcmSender();
        }
    }
}
