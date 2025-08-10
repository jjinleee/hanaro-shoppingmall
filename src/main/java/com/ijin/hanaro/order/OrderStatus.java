package com.ijin.hanaro.order;

public enum OrderStatus {
    ORDERED,      // 결제 완료(주문 생성 직후)
    PREPARING,    // 배송 준비
    SHIPPING,     // 배송 중
    DELIVERED,    // 배송 완료
    CANCELED      // 취소
}