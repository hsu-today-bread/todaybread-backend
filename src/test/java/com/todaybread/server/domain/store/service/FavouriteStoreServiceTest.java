package com.todaybread.server.domain.store.service;

import com.todaybread.server.domain.bread.entity.BreadEntity;
import com.todaybread.server.domain.bread.repository.BreadRepository;
import com.todaybread.server.domain.store.dto.FavouriteStoreResponse;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleRequest;
import com.todaybread.server.domain.store.dto.FavouriteStoreToggleResponse;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreBusinessHoursEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.store.repository.FavouriteStoreRepository;
import com.todaybread.server.domain.store.repository.StoreBusinessHoursRepository;
import com.todaybread.server.domain.store.repository.StoreImageRepository;
import com.todaybread.server.domain.store.repository.StoreRepository;
import com.todaybread.server.global.exception.CustomException;
import com.todaybread.server.global.exception.ErrorCode;
import com.todaybread.server.global.storage.FileStorage;
import com.todaybread.server.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

class FavouriteStoreServiceTest {

    @Mock
    private FavouriteStoreRepository favouriteStoreRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private BreadRepository breadRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private StoreBusinessHoursRepository storeBusinessHoursRepository;

    @Mock
    private FileStorage fileStorage;

    private FavouriteStoreService favouriteStoreService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        favouriteStoreService = new FavouriteStoreService(
                favouriteStoreRepository,
                storeRepository,
                breadRepository,
                storeImageRepository,
                storeBusinessHoursRepository,
                fileStorage,
                TestFixtures.FIXED_CLOCK
        );
    }

    @Test
    void toggleFavouriteStore_removesExistingFavourite() {
        FavouriteStoreEntity existing = TestFixtures.favouriteStore(1L, 1L, 100L);
        given(favouriteStoreRepository.findByUserIdAndStoreId(1L, 100L)).willReturn(Optional.of(existing));

        FavouriteStoreToggleResponse response = favouriteStoreService.toggleFavouriteStore(1L, new FavouriteStoreToggleRequest(100L));

        assertThat(response.added()).isFalse();
        verify(favouriteStoreRepository).delete(existing);
    }

    @Test
    void toggleFavouriteStore_rejectsMissingStore() {
        given(favouriteStoreRepository.findByUserIdAndStoreId(1L, 100L)).willReturn(Optional.empty());
        given(storeRepository.findByIdAndIsActiveTrue(100L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> favouriteStoreService.toggleFavouriteStore(1L, new FavouriteStoreToggleRequest(100L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.STORE_NOT_FOUND);
    }

    @Test
    void toggleFavouriteStore_rejectsWhenLimitExceeded() {
        StoreEntity store = TestFixtures.store(100L, 10L);
        given(favouriteStoreRepository.findByUserIdAndStoreId(1L, 100L)).willReturn(Optional.empty());
        given(storeRepository.findByIdAndIsActiveTrue(100L)).willReturn(Optional.of(store));
        given(favouriteStoreRepository.countByUserIdWithLock(1L)).willReturn(5L);

        assertThatThrownBy(() -> favouriteStoreService.toggleFavouriteStore(1L, new FavouriteStoreToggleRequest(100L)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FAVOURITE_STORE_LIMIT_EXCEEDED);
    }

    @Test
    void toggleFavouriteStore_returnsAddedWhenConcurrentDuplicateOccurs() {
        StoreEntity store = TestFixtures.store(100L, 10L);
        given(favouriteStoreRepository.findByUserIdAndStoreId(1L, 100L)).willReturn(Optional.empty());
        given(storeRepository.findByIdAndIsActiveTrue(100L)).willReturn(Optional.of(store));
        given(favouriteStoreRepository.countByUserIdWithLock(1L)).willReturn(1L);
        given(favouriteStoreRepository.save(any())).willThrow(new DataIntegrityViolationException("duplicate"));

        FavouriteStoreToggleResponse response = favouriteStoreService.toggleFavouriteStore(1L, new FavouriteStoreToggleRequest(100L));

        assertThat(response.added()).isTrue();
    }

    @Test
    void getMyFavouriteStores_returnsOnlyActiveStores() {
        FavouriteStoreEntity favourite1 = TestFixtures.favouriteStore(1L, 1L, 100L);
        FavouriteStoreEntity favourite2 = TestFixtures.favouriteStore(2L, 1L, 200L);
        StoreEntity activeStore = TestFixtures.store(100L, 10L);
        BreadEntity bread = TestFixtures.bread(1L, 100L, 3, 4_000, 2_000);
        StoreBusinessHoursEntity sundayHours = TestFixtures.businessHours(
                100L, 7, false, LocalTime.of(9, 0), LocalTime.of(20, 0), LocalTime.of(19, 0)
        );

        given(favouriteStoreRepository.findByUserId(1L)).willReturn(List.of(favourite1, favourite2));
        given(storeRepository.findByIdInAndIsActiveTrue(List.of(100L, 200L))).willReturn(List.of(activeStore));
        given(breadRepository.findByStoreIdIn(List.of(100L))).willReturn(List.of(bread));
        given(storeBusinessHoursRepository.findByStoreIdIn(List.of(100L))).willReturn(List.of(sundayHours));
        given(storeImageRepository.findByStoreIdInOrderByStoreIdAscDisplayOrderAsc(List.of(100L)))
                .willReturn(List.of(TestFixtures.storeImage(1L, 100L, "store.jpg", 0)));
        given(fileStorage.getFileUrl("store.jpg")).willReturn("https://cdn/store.jpg");

        List<FavouriteStoreResponse> responses = favouriteStoreService.getMyFavouriteStores(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().storeId()).isEqualTo(100L);
        assertThat(responses.getFirst().imageUrl()).isEqualTo("https://cdn/store.jpg");
        assertThat(responses.getFirst().isSelling()).isTrue();
    }
}
