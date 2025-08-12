package com.ijin.hanaro.stats;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailyProductSalesRepository extends JpaRepository<DailyProductSales, Long> {
    List<DailyProductSales> findBySalesDate(LocalDate date);
    List<DailyProductSales> findBySalesDateBetweenOrderBySalesDate(LocalDate from, LocalDate to);
    void deleteBySalesDate(LocalDate date);

}