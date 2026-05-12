package com.todaybread.server.domain.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 사업자등록번호 원문 저장을 피하기 위한 해시 유틸리티입니다.
 */
@Component
public class BusinessNumberHasher {

    private final String hashSecret;

    public BusinessNumberHasher(@Value("${business.approval.hash-secret:${jwt.secret}}") String hashSecret) {
        this.hashSecret = hashSecret;
    }

    public String hash(String normalizedBossNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((hashSecret + normalizedBossNumber).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }

    public String last4(String normalizedBossNumber) {
        return normalizedBossNumber.substring(normalizedBossNumber.length() - 4);
    }
}
