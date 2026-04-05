package com.todaybread.server.integration;

import com.todaybread.server.domain.bread.dto.BreadCommonRequest;
import com.todaybread.server.domain.store.dto.StoreCommonRequest;
import com.todaybread.server.domain.user.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BossCatalogApiIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void bossCanCreateStoreAndBreadAndUserCanBrowseThem() throws Exception {
        UserEntity boss = saveUser(
                "boss@example.com",
                "boss-user",
                "Boss User",
                "password1234",
                "010-4444-4444",
                true
        );
        UserEntity user = saveUser(
                "customer@example.com",
                "customer-user",
                "Customer User",
                "password1234",
                "010-5555-5555",
                false
        );
        String bossToken = bearerToken(boss);
        String userToken = bearerToken(user);

        StoreCommonRequest storeRequest = new StoreCommonRequest(
                "Today Bread",
                "02-1111-2222",
                "fresh bakery",
                "Seoul address 1",
                "Seoul address 2",
                BigDecimal.valueOf(37.5000000),
                BigDecimal.valueOf(127.0000000),
                standardBusinessHoursRequest()
        );

        mockMvc.perform(multipart("/api/boss/store")
                        .file(jsonPart("request", storeRequest))
                        .file(imagePart("images", "store.jpg"))
                        .header("Authorization", "Bearer " + bossToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.name").value("Today Bread"))
                .andExpect(jsonPath("$.images[0].imageUrl").value(org.hamcrest.Matchers.startsWith("/images/")));

        Long storeId = storeRepository.findByUserIdAndIsActiveTrue(boss.getId()).orElseThrow().getId();

        BreadCommonRequest breadRequest = new BreadCommonRequest(
                "Sourdough",
                5000,
                3000,
                5,
                "daily baked"
        );

        MvcResult addBreadResult = mockMvc.perform(multipart("/api/boss/bread")
                        .file(jsonPart("request", breadRequest))
                        .file(imagePart("image", "bread.jpg"))
                        .header("Authorization", "Bearer " + bossToken)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sourdough"))
                .andExpect(jsonPath("$.imageUrl").value(org.hamcrest.Matchers.startsWith("/images/")))
                .andReturn();

        Long breadId = json(addBreadResult).get("id").asLong();

        mockMvc.perform(get("/api/boss/bread")
                        .header("Authorization", "Bearer " + bossToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(breadId));

        mockMvc.perform(get("/api/store/nearby")
                        .header("Authorization", "Bearer " + userToken)
                        .param("lat", "37.5000000")
                        .param("lng", "127.0000000")
                        .param("radius", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].storeId").value(storeId))
                .andExpect(jsonPath("$[0].isSelling").value(true));

        mockMvc.perform(get("/api/bread/nearby")
                        .header("Authorization", "Bearer " + userToken)
                        .param("lat", "37.5000000")
                        .param("lng", "127.0000000")
                        .param("radius", "1")
                        .param("sort", "price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(breadId))
                .andExpect(jsonPath("$[0].storeId").value(storeId));

        mockMvc.perform(get("/api/store/{storeId}", storeId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.store.id").value(storeId))
                .andExpect(jsonPath("$.breads[0].id").value(breadId))
                .andExpect(jsonPath("$.isSelling").value(true));

        mockMvc.perform(get("/api/bread/detail/{breadId}", breadId)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(breadId))
                .andExpect(jsonPath("$.storeId").value(storeId))
                .andExpect(jsonPath("$.isSelling").value(true));

        assertThat(storeImageRepository.findAll()).hasSize(1);
        assertThat(breadImageRepository.findAll()).hasSize(1);
        assertThat(breadRepository.findById(breadId)).isPresent();
    }
}
