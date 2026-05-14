package com.todaybread.server.integration;

import com.todaybread.server.domain.keyword.entity.KeywordEntity;
import com.todaybread.server.domain.keyword.entity.UserKeywordEntity;
import com.todaybread.server.domain.store.entity.FavouriteStoreEntity;
import com.todaybread.server.domain.store.entity.StoreEntity;
import com.todaybread.server.domain.user.entity.UserEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationTargetRepositoryIntegrationTest extends ApiIntegrationTestSupport {

    @Test
    void keywordNotificationTargetQuery_excludesBossUsers() {
        UserEntity user = saveUser("user@example.com", "user", "일반", "password1234", "010-1000-0001", false);
        UserEntity boss = saveUser("boss@example.com", "boss", "사장", "password1234", "010-1000-0002", true);
        KeywordEntity keyword = saveKeyword("소금빵");
        UserKeywordEntity userKeyword = saveUserKeyword(user.getId(), keyword.getId(), "소금빵");
        saveUserKeyword(boss.getId(), keyword.getId(), "소금빵");

        List<UserKeywordEntity> result =
                userKeywordRepository.findByKeywordIdInForUserNotificationTargets(List.of(keyword.getId()));

        assertThat(result)
                .extracting(UserKeywordEntity::getId)
                .containsExactly(userKeyword.getId());
    }

    @Test
    void favouriteStoreNotificationTargetQuery_excludesBossUsers() {
        UserEntity storeOwner = saveUser("owner@example.com", "owner", "점주", "password1234", "010-1000-0001", true);
        UserEntity user = saveUser("user@example.com", "user", "일반", "password1234", "010-1000-0002", false);
        UserEntity boss = saveUser("boss@example.com", "boss", "사장", "password1234", "010-1000-0003", true);
        StoreEntity store = saveStore(storeOwner, "02-1000-0001",
                BigDecimal.valueOf(37.5826000), BigDecimal.valueOf(127.0106000));
        FavouriteStoreEntity userFavourite = saveFavouriteStore(user.getId(), store.getId());
        saveFavouriteStore(boss.getId(), store.getId());

        List<FavouriteStoreEntity> result =
                favouriteStoreRepository.findByStoreIdForUserNotificationTargets(store.getId());

        assertThat(result)
                .extracting(FavouriteStoreEntity::getId)
                .containsExactly(userFavourite.getId());
    }
}
