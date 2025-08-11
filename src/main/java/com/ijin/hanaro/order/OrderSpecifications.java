// src/main/java/com/ijin/hanaro/order/OrderSpecifications.java
package com.ijin.hanaro.order;

import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderSpecifications {

    public static Specification<Order> statusEq(OrderStatus status) {
        return (root, q, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> orderNoLike(String like) {
        return (root, q, cb) ->
                (like == null || like.isBlank())
                        ? cb.conjunction()
                        : cb.like(cb.lower(root.get("orderNo")), "%" + like.toLowerCase() + "%");
    }

    public static Specification<Order> usernameLike(String like) {
        return (root, q, cb) -> {
            if (like == null || like.isBlank()) return cb.conjunction();
            var user = root.join("user");
            return cb.like(cb.lower(user.get("username")), "%" + like.toLowerCase() + "%");
        };
    }

    public static Specification<Order> createdBetween(LocalDate from, LocalDate to) {
        return (root, q, cb) -> {
            if (from == null && to == null) return cb.conjunction();
            jakarta.persistence.criteria.Path<LocalDateTime> path = root.<LocalDateTime>get("createdAt");
            LocalDateTime start = (from == null) ? null : from.atStartOfDay();
            LocalDateTime end   = (to == null) ? null : to.plusDays(1).atStartOfDay(); // inclusive
            if (start != null && end != null) return cb.between(path, start, end);
            if (start != null) return cb.greaterThanOrEqualTo(path, start);
            return cb.lessThan(path, end);
        };
    }
}