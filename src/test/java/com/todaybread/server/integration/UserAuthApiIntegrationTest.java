package com.todaybread.server.integration;

import com.todaybread.server.domain.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAuthApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void registerLoginReissueAndLogoutFlow() throws Exception {
        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user1@example.com",
                                  "nickname": "user1",
                                  "name": "User One",
                                  "password": "password1234",
                                  "phoneNumber": "010-1111-1111"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/user/exist/email").param("value", "user1@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));

        MvcResult loginResult = mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user1@example.com",
                                  "password": "password1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String accessToken = json(loginResult).get("accessToken").asText();
        String refreshToken = json(loginResult).get("refreshToken").asText();

        MvcResult reissueResult = mockMvc.perform(post("/api/auth/reissue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String reissuedRefreshToken = json(reissueResult).get("refreshToken").asText();
        UserEntity savedUser = userRepository.findByEmail("user1@example.com").orElseThrow();
        assertThat(refreshTokenRepository.findByUserId(savedUser.getId())).isPresent();
        assertThat(passwordEncoder.matches(
                reissuedRefreshToken,
                refreshTokenRepository.findByUserId(savedUser.getId()).orElseThrow().getToken()
        )).isTrue();

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(refreshTokenRepository.findByUserId(savedUser.getId())).isEmpty();
    }

    @Test
    void bossApproveUnlocksBossEndpoints() throws Exception {
        UserEntity user = saveUser(
                "boss-apply@example.com",
                "bossapply",
                "Boss Apply",
                "password1234",
                "010-2222-2222",
                false
        );
        String accessToken = bearerToken(user);

        mockMvc.perform(get("/api/boss/store/status")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("STORE_001"));

        MvcResult approveResult = mockMvc.perform(post("/api/user/boss-approve")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "bossNumber": "1234567890"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        String bossAccessToken = json(approveResult).get("accessToken").asText();

        mockMvc.perform(get("/api/boss/store/status")
                        .header("Authorization", "Bearer " + bossAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasStore").value(false));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getIsBoss()).isTrue();
        assertThat(refreshTokenRepository.findByUserId(user.getId())).isPresent();
    }

    @Test
    void recoveryEndpointsResetPasswordAndAllowNewLogin() throws Exception {
        saveUser(
                "recover@example.com",
                "recover-user",
                "Recover User",
                "oldpassword123",
                "010-3333-3333",
                false
        );

        mockMvc.perform(get("/api/user/find-email")
                        .param("phone", "010-3333-3333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maskedEmail").value("r*****r@example.com"));

        mockMvc.perform(get("/api/user/verify-identity")
                        .param("phone", "010-3333-3333")
                        .param("email", "recover@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.email").value("recover@example.com"));

        mockMvc.perform(post("/api/user/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "recover@example.com",
                                  "newPassword": "newpassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "recover@example.com",
                                  "password": "newpassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
