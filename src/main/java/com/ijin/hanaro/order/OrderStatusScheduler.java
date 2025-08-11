package com.ijin.hanaro.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderStatusScheduler {

    private final OrderRepository orderRepository;

    /**
     * 결제완료(ORDERED) -> 배송준비(PREPARING)
     * 매 5분마다: 최근 상태 갱신 시간이 5분 이상 지난 건들을 PREPARING으로 전환
     */
    @Scheduled(cron = "0 */5 * * * *") // 매 5분
    public void toPreparing() {
        var threshold = LocalDateTime.now().minusMinutes(5);
        int changed = orderRepository.bulkUpdateStatus(
                OrderStatus.ORDERED, OrderStatus.PREPARING, threshold);
        if (changed > 0) {
            log.info("[Scheduler] ORDERED -> PREPARING 변경 건수: {}", changed);
        }
    }

    /**
     * 배송준비(PREPARING) -> 배송중(SHIPPING)
     * 매 15분마다: 최근 상태 갱신 시간이 15분 이상 지난 건들을 SHIPPING으로 전환
     */
    @Scheduled(cron = "0 */15 * * * *") // 매 15분
    public void toShipping() {
        var threshold = LocalDateTime.now().minusMinutes(15);
        int changed = orderRepository.bulkUpdateStatus(
                OrderStatus.PREPARING, OrderStatus.SHIPPING, threshold);
        if (changed > 0) {
            log.info("[Scheduler] PREPARING -> SHIPPING 변경 건수: {}", changed);
        }
    }

    /**
     * 배송중(SHIPPING) -> 배송완료(DELIVERED)
     * 매 1시간마다: 최근 상태 갱신 시간이 1시간 이상 지난 건들을 DELIVERED로 전환
     */
    @Scheduled(cron = "0 0 * * * *") // 매 정시(1시간마다)
    public void toDelivered() {
        var threshold = LocalDateTime.now().minusHours(1);
        int changed = orderRepository.bulkUpdateStatus(
                OrderStatus.SHIPPING, OrderStatus.DELIVERED, threshold);
        if (changed > 0) {
            log.info("[Scheduler] SHIPPING -> DELIVERED 변경 건수: {}", changed);
        }
    }
}