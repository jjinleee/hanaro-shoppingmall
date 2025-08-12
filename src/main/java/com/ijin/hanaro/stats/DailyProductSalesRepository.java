package com.ijin.hanaro.stats;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DailyProductSalesRepository extends JpaRepository<DailyProductSales, Long> {
    List<DailyProductSales> findBySalesDate(LocalDate date);
    List<DailyProductSales> findBySalesDateBetweenOrderBySalesDate(LocalDate from, LocalDate to);
    void deleteBySalesDate(LocalDate date);
    @Modifying
    @Query("delete from DailySales d where d.salesDate = :statDate")
    void deleteByStatDate(@Param("statDate") LocalDate statDate);
}