package com.ijin.hanaro.stats;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface DailySalesRepository extends JpaRepository<DailySales, LocalDate> {
    List<DailySales> findBySalesDateBetweenOrderBySalesDate(LocalDate from, LocalDate to);
}