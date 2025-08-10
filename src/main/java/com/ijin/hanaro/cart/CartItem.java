package com.ijin.hanaro.cart;

import com.ijin.hanaro.product.Product;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items",
        uniqueConstraints = @UniqueConstraint(columnNames = {"cart_user_id","product_id"}))
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cart_user_id", nullable = false)
    private Long cartUserId; // carts.user_id

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected CartItem() {}

    public CartItem(Long cartUserId, Product product, int quantity) {
        this.cartUserId = cartUserId;
        this.product = product;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Long getCartUserId() { return cartUserId; }
    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int q) { this.quantity = q; }
}