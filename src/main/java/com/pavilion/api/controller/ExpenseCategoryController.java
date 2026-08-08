package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.ExpenseCategoryRequest;
import com.pavilion.api.dto.MastersDtos.ExpenseCategoryResponse;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ExpenseCategoryRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/expense-categories")
@PreAuthorize("hasRole('ADMIN')")
public class ExpenseCategoryController {

    private final ExpenseCategoryRepository expenseCategoryRepository;

    public ExpenseCategoryController(ExpenseCategoryRepository expenseCategoryRepository) {
        this.expenseCategoryRepository = expenseCategoryRepository;
    }

    @GetMapping
    public List<ExpenseCategoryResponse> listExpenseCategories() {
        return expenseCategoryRepository.findAll().stream().map(ExpenseCategoryResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ExpenseCategoryResponse> createExpenseCategory(@Valid @RequestBody ExpenseCategoryRequest body) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(body.name());
        category.setGstSlabPercent(body.gstSlabPercent());
        category = expenseCategoryRepository.save(category);
        return ResponseEntity.status(HttpStatus.CREATED).body(ExpenseCategoryResponse.from(category));
    }

    @PutMapping("/{id}")
    public ExpenseCategoryResponse updateExpenseCategory(@PathVariable Long id, @Valid @RequestBody ExpenseCategoryRequest body) {
        ExpenseCategory category = expenseCategoryRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Expense category not found"));
        category.setName(body.name());
        category.setGstSlabPercent(body.gstSlabPercent());
        return ExpenseCategoryResponse.from(expenseCategoryRepository.save(category));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteExpenseCategory(@PathVariable Long id) {
        if (!expenseCategoryRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Expense category not found");
        }
        expenseCategoryRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
