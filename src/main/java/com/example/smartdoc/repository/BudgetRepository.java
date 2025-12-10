package com.example.smartdoc.repository;

import com.example.smartdoc.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserId(Long userId);
    Budget findByUserIdAndCategory(Long userId, String category);
}
