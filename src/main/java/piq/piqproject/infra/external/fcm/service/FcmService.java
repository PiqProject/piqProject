package piq.piqproject.infra.external.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.WebpushConfig;
import com.google.firebase.messaging.WebpushFcmOptions;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import org.springframework.stereotype.Service;
import piq.piqproject.domain.alarms.service.AlarmService;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final AlarmService alarmService;

    /**
     * 특정 디바이스 토큰으로 푸시 알림을 보냅니다. (앱 알림)
     *
     * @param targetToken 대상 디바이스 토큰
     * @param title       알림 제목
     * @param body        알림 내용
     */
    public void sendMessageToApp(String targetToken, String title, String body, Map<String, String> data) {
        try {
            Message message = Message.builder()
                    .setToken(targetToken)
                    // 알림 클릭 시 이동할 경로에 필요한 정보
                    .putAllData(data)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM error for token {}: {}", targetToken, e.getMessagingErrorCode());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("Token is no longer valid. Deleting from DB: {}", targetToken);
            }
            alarmService.deleteAlarm(targetToken);
        } catch (Exception e) {
            log.error("Unexpected error sending FCM message to token: {}", targetToken, e);
        }
    }

    /**
     * 특정 디바이스 토큰으로 푸시 알림을 보냅니다. (웹 알림)
     *
     * @param targetToken 대상 디바이스 토큰
     * @param title       알림 제목
     * @param body        알림 내용
     */
    public void sendWebMessageToWeb(String targetToken, String title, String body, String targetUrl) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(targetToken)
                    // 알림 클릭 시 이동할 경로에 필요한 정보
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build());

            messageBuilder.setWebpushConfig(WebpushConfig.builder()
                    .setFcmOptions(WebpushFcmOptions.withLink(targetUrl)) // 알림 클릭 시 이동할 URL
                    .build());

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.info("Successfully sent message: {}", response);
        } catch (FirebaseMessagingException e) {
            log.error("FCM error for token {}: {}", targetToken, e.getMessagingErrorCode());
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                log.warn("Token is no longer valid. Deleting from DB: {}", targetToken);
            }
            alarmService.deleteAlarm(targetToken);
        } catch (Exception e) {
            log.error("Unexpected error sending FCM message to token: {}", targetToken, e);
        }
    }
}