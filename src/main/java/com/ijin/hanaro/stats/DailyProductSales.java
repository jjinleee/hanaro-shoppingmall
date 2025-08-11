package com.ijin.hanaro.stats;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "daily_product_sales",
        uniqueConstraints = @UniqueConstraint(columnNames={"sales_date","product_id"}))
public class DailyProductSales {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sales_date", nullable = false)
    private LocalDate salesDate;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "qty", nullable = false)
    private int qty;

    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    protected DailyProductSales() {}
    public DailyProductSales(LocalDate date, Long productId, int qty, BigDecimal amount) {
        this.salesDate = date; this.productId = productId; this.qty = qty; this.amount = amount;
    }

    public Long getId() { return id; }
    public LocalDate getSalesDate() { return salesDate; }
    public Long getProductId() { return productId; }
    public int getQty() { return qty; }
    public BigDecimal getAmount() { return amount; }
}