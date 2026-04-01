package com.todaybread.server.domain.store.repository;

/**
 * 근처 가게 조회 시 가게 ID와 거리를 반환하는 Projection입니다.
 */
public interface StoreDistanceProjection {

    /**
     * 가게 ID를 반환합니다.
     *
     * @return 가게 ID
     */
    Long getStoreId();

    /**
     * 유저와 가게 사이의 거리를 반환합니다 (km).
     *
     * @return 거리 (km)
     */
    Double getDistance();
}
