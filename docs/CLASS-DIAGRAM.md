# 클래스 다이어그램

이 문서는 커머스 코어 도메인 기준으로 주요 엔티티 관계를 한 번에 보기 위한 Mermaid 클래스 다이어그램입니다.

현재 소스는 JPA 객체 연관관계보다 `Long` FK 필드 중심으로 작성되어 있으므로, 아래 다이어그램은 엔티티 필드와 DB 스키마를 기준으로 한 "논리적 관계"를 표현합니다.

## Commerce Core

```mermaid
classDiagram
direction LR

class BaseEntity {
  <<abstract>>
  LocalDateTime createdAt
  LocalDateTime updatedAt
}

class UserEntity {
  Long id
  String email
  String name
  String nickname
  String phoneNumber
  Boolean isBoss
}

class StoreEntity {
  Long id
  Long userId
  String name
  String phoneNumber
  String addressLine1
  String addressLine2
  BigDecimal latitude
  BigDecimal longitude
  Boolean isActive
}

class BreadEntity {
  Long id
  Long storeId
  String name
  int originalPrice
  int salePrice
  int remainingQuantity
}

class CartEntity {
  Long id
  Long userId
  Long storeId
}

class CartItemEntity {
  Long id
  Long cartId
  Long breadId
  int quantity
}

class OrderEntity {
  Long id
  Long userId
  Long storeId
  OrderStatus status
  int totalAmount
  String idempotencyKey
}

class OrderItemEntity {
  Long id
  Long orderId
  Long breadId
  String breadName
  int breadPrice
  int quantity
}

class PaymentEntity {
  Long id
  Long orderId
  int amount
  PaymentStatus status
  LocalDateTime paidAt
  String idempotencyKey
}

class OrderStatus {
  <<enumeration>>
  PENDING
  CONFIRMED
  CANCELLED
}

class PaymentStatus {
  <<enumeration>>
  PENDING
  APPROVED
  FAILED
}

BaseEntity <|-- UserEntity
BaseEntity <|-- StoreEntity
BaseEntity <|-- BreadEntity
BaseEntity <|-- CartEntity
BaseEntity <|-- CartItemEntity
BaseEntity <|-- OrderEntity
BaseEntity <|-- OrderItemEntity
BaseEntity <|-- PaymentEntity

UserEntity "1" --> "0..1" StoreEntity : owns
StoreEntity "1" --> "0..*" BreadEntity : sells

UserEntity "1" --> "0..1" CartEntity : has
CartEntity "1" --> "0..*" CartItemEntity : contains
CartItemEntity "*" --> "1" BreadEntity : references

UserEntity "1" --> "0..*" OrderEntity : places
StoreEntity "1" --> "0..*" OrderEntity : receives
OrderEntity "1" --> "1..*" OrderItemEntity : snapshots
OrderItemEntity "*" --> "0..1" BreadEntity : origin

OrderEntity --> OrderStatus
PaymentEntity --> PaymentStatus
OrderEntity "1" --> "0..1" PaymentEntity : paid by
```

## 읽는 포인트

- `StoreEntity.userId` 때문에 한 명의 사장님은 최대 한 개의 가게를 가집니다.
- `CartEntity.storeId` 때문에 장바구니는 한 시점에 하나의 매장 빵만 담도록 설계되어 있습니다.
- `OrderItemEntity`는 주문 시점의 `breadName`, `breadPrice`를 별도로 저장하는 스냅샷 엔티티입니다.
- `PaymentEntity.orderId`가 unique라서 주문 하나당 결제는 최대 하나입니다.

## 기준 파일

- `src/main/java/com/todaybread/server/domain/user/entity/UserEntity.java`
- `src/main/java/com/todaybread/server/domain/store/entity/StoreEntity.java`
- `src/main/java/com/todaybread/server/domain/bread/entity/BreadEntity.java`
- `src/main/java/com/todaybread/server/domain/cart/entity/CartEntity.java`
- `src/main/java/com/todaybread/server/domain/cart/entity/CartItemEntity.java`
- `src/main/java/com/todaybread/server/domain/order/entity/OrderEntity.java`
- `src/main/java/com/todaybread/server/domain/order/entity/OrderItemEntity.java`
- `src/main/java/com/todaybread/server/domain/payment/entity/PaymentEntity.java`
- `src/main/resources/db/migration/V1__init_schema.sql`
