package com.ijin.hanaro.cart;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Cart() {}
    public Cart(Long userId) { this.userId = userId; }

    public Long getUserId() { return userId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}