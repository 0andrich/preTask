package org.backend.util;

import org.backend.domain.enums.NotificationChannel;
import org.backend.domain.enums.NotificationType;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class IdempotencyKeyGenerator {
    // 중복 방지 멱등성 키 생성
    public String generate(
            Long recipientId,
            NotificationType notificationType,
            String referenceId,
            NotificationChannel channel
    ) {
        String raw = recipientId + "|" + notificationType.name() + "|"
                + (referenceId != null ? referenceId : "") + "|" + channel.name();
        return sha256Hex(raw);
    }

    // SHA-256으로 암호화
    private String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
