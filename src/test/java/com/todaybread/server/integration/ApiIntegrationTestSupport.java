package com.todaybread.server.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.todaybread.server.ServerApplication;
import com.todaybread.server.config.jwt.JwtTokenService;
import com.todaybread.server.domain.auth.repository.RefreshTokenRepository;
import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.entity.BreadImageEntity;
import com.todaybread.server.domain.bread.repository.BreadImageRepository;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.cart.repository.CartItemRepository;
import com.todaybread.server.domain.cart.repository.CartRepository;
import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.keyword.repository.KeywordRepository;
import com.todaybread.server.domain.keyword.repository.UserKeywordRepository;
import com.todaybread.server.domain.order.repository.OrderItemRepository;
import com.todaybread.server.domain.order.repository.OrderRepository;
import com.todaybread.server.domain.payment.repository.PaymentRepository;
import com.todaybread.server.domain.store.dto.BusinessHoursRequest;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.entity.StoreImageEntity;
import com.todaybread.server.domain.store.repository.FavouriteStoreRepository;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.domain.user.entity.UserEntity;
import com.todaybread.server.domain.user.repository.PasswordResetTokenRepository;
import com.todaybread.server.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@SpringBootTest(classes = {ServerApplication.class, ApiIntegrationTestSupport.FixedClockConfig.class})
@AutoConfigureMockMvc
@ActiveProfiles({"test", "stub"})
@SpringJUnitConfig
abstract class ApiIntegrationTestSupport {

    private static final Path UPLOAD_DIR = Path.of(
            System.getProperty("java.io.tmpdir"),
            "todaybread-it-" + UUID.randomUUID()
    );

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JwtTokenService jwtTokenService;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    protected RefreshTokenRepository refreshTokenRepository;

    @Autowired
    protected KeywordRepository keywordRepository;

    @Autowired
    protected UserKeywordRepository userKeywordRepository;

    @Autowired
    protected FavouriteStoreRepository favouriteStoreRepository;

    @Autowired
    protected StoreImageRepository storeImageRepository;

    @Autowired
    protected StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Autowired
    protected BreadImageRepository breadImageRepository;

    @Autowired
    protected CartItemRepository cartItemRepository;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    protected PaymentRepository paymentRepository;

    @Autowired
    protected OrderItemRepository orderItemRepository;

    @Autowired
    protected OrderRepository orderRepository;

    @Autowired
    protected BreadRepository breadRepository;

    @Autowired
    protected StoreRepository storeRepository;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("app.file.upload-dir", () -> UPLOAD_DIR.toString());
    }

    @AfterEach
    void cleanup() throws IOException {
        paymentRepository.deleteAllInBatch();
        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        cartItemRepository.deleteAllInBatch();
        cartRepository.deleteAllInBatch();
        breadImageRepository.deleteAllInBatch();
        breadRepository.deleteAllInBatch();
        storeImageRepository.deleteAllInBatch();
        storeBusinessHoursRepository.deleteAllInBatch();
        favouriteStoreRepository.deleteAllInBatch();
        storeRepository.deleteAllInBatch();
        userKeywordRepository.deleteAllInBatch();
        keywordRepository.deleteAllInBatch();
        refreshTokenRepository.deleteAllInBatch();
        passwordResetTokenRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();

        FileSystemUtils.deleteRecursively(UPLOAD_DIR);
        Files.createDirectories(UPLOAD_DIR);
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String bearerToken(UserEntity user) {
        String role = user.getIsBoss() ? "BOSS" : "USER";
        return jwtTokenService.generateAccessToken(user.getId(), user.getEmail(), role);
    }

    protected UserEntity saveUser(String email, String nickname, String name, String rawPassword, String phone, boolean boss) {
        UserEntity user = UserEntity.builder()
                .email(email)
                .nickname(nickname)
                .name(name)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .phoneNumber(phone)
                .build();
        if (boss) {
            user.approveBoss();
        }
        return userRepository.save(user);
    }

    protected StoreEntity saveStore(UserEntity boss, String phone, BigDecimal lat, BigDecimal lng) {
        StoreEntity store = StoreEntity.builder()
                .userId(boss.getId())
                .name("store-" + boss.getId())
                .phoneNumber(phone)
                .description("store-description")
                .addressLine1("address-line-1")
                .addressLine2("address-line-2")
                .latitude(lat)
                .longitude(lng)
                .build();
        return storeRepository.save(store);
    }

    protected List<StoreBusinessHoursEntity> saveStandardBusinessHours(Long storeId) {
        List<StoreBusinessHoursEntity> hours = List.of(
                businessHours(storeId, 1),
                businessHours(storeId, 2),
                businessHours(storeId, 3),
                businessHours(storeId, 4),
                businessHours(storeId, 5),
                businessHours(storeId, 6),
                businessHours(storeId, 7)
        );
        return storeBusinessHoursRepository.saveAll(hours);
    }

    protected BreadEntity saveBread(Long storeId, String name, int originalPrice, int salePrice, int quantity) {
        BreadEntity bread = BreadEntity.builder()
                .storeId(storeId)
                .name(name)
                .description(name + "-description")
                .originalPrice(originalPrice)
                .salePrice(salePrice)
                .remainingQuantity(quantity)
                .build();
        return breadRepository.save(bread);
    }

    protected StoreImageEntity saveStoreImage(Long storeId, String storedFilename, int displayOrder) {
        return storeImageRepository.save(StoreImageEntity.builder()
                .storeId(storeId)
                .originalFilename("store.jpg")
                .storedFilename(storedFilename)
                .displayOrder(displayOrder)
                .build());
    }

    protected BreadImageEntity saveBreadImage(Long breadId, String storedFilename) {
        return breadImageRepository.save(BreadImageEntity.builder()
                .breadId(breadId)
                .originalFilename("bread.jpg")
                .storedFilename(storedFilename)
                .build());
    }

    protected FavouriteStoreEntity saveFavouriteStore(Long userId, Long storeId) {
        return favouriteStoreRepository.save(FavouriteStoreEntity.builder()
                .userId(userId)
                .storeId(storeId)
                .build());
    }

    protected KeywordEntity saveKeyword(String normalisedText) {
        return keywordRepository.save(KeywordEntity.builder()
                .normalisedText(normalisedText)
                .build());
    }

    protected UserKeywordEntity saveUserKeyword(Long userId, Long keywordId, String displayText) {
        return userKeywordRepository.save(UserKeywordEntity.builder()
                .userId(userId)
                .keywordId(keywordId)
                .displayText(displayText)
                .build());
    }

    protected MockMultipartFile jsonPart(String name, Object value) throws Exception {
        return new MockMultipartFile(name, "", "application/json", objectMapper.writeValueAsBytes(value));
    }

    protected MockMultipartFile imagePart(String name, String filename) {
        return new MockMultipartFile(name, filename, "image/jpeg", "image-bytes".getBytes());
    }

    protected String storedUploadFilename(String originalPath) {
        return Path.of(originalPath).getFileName().toString();
    }

    protected List<BusinessHoursRequest> standardBusinessHoursRequest() {
        return List.of(
                new BusinessHoursRequest(1, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(2, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(3, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(4, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(5, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(6, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)),
                new BusinessHoursRequest(7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0))
        );
    }

    private StoreBusinessHoursEntity businessHours(Long storeId, int dayOfWeek) {
        return StoreBusinessHoursEntity.builder()
                .storeId(storeId)
                .dayOfWeek(dayOfWeek)
                .isClosed(false)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(20, 0))
                .lastOrderTime(LocalTime.of(19, 0))
                .build();
    }

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedTestClock() {
            return Clock.fixed(Instant.parse("2026-04-05T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        }
    }
}
