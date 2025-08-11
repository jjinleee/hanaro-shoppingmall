package com.ijin.hanaro.stats;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_sales")
public class DailySales {
    @Id
    @Column(name = "sales_date")
    private LocalDate salesDate;

    @Column(name = "total_orders", nullable = false)
    private int totalOrders;

    @Column(name = "total_items", nullable = false)
    private int totalItems;

    @Column(name = "total_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal totalAmount;

    protected DailySales() {}
    public DailySales(LocalDate salesDate, int totalOrders, int totalItems, BigDecimal totalAmount) {
        this.salesDate = salesDate;
        this.totalOrders = totalOrders;
        this.totalItems = totalItems;
        this.totalAmount = totalAmount;
    }

    public LocalDate getSalesDate() { return salesDate; }
    public int getTotalOrders() { return totalOrders; }
    public int getTotalItems() { return totalItems; }
    public BigDecimal getTotalAmount() { return totalAmount; }
}