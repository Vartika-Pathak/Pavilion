package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.ExpenseCategoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExpenseCategoryControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;

    private ExpenseCategory createCategory(String name, int gstSlabPercent) {
        ExpenseCategory category = new ExpenseCategory();
        category.setName(name);
        category.setGstSlabPercent(gstSlabPercent);
        return expenseCategoryRepository.save(category);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/expense-categories")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotListExpenseCategories() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/expense-categories").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListExpenseCategories() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/expense-categories")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Housekeeping\",\"gstSlabPercent\":18}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Housekeeping"))
                .andExpect(jsonPath("$.gstSlabPercent").value(18));

        mockMvc.perform(get("/api/expense-categories").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Housekeeping"));
    }

    @Test
    void adminCanUpdateAndDeleteAnExpenseCategory() throws Exception {
        User admin = createUser("admin");
        ExpenseCategory category = createCategory("Housekeeping", 18);

        mockMvc.perform(put("/api/expense-categories/" + category.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Security\",\"gstSlabPercent\":18}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Security"));

        mockMvc.perform(delete("/api/expense-categories/" + category.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void gstSlabOutOfRangeFails() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/expense-categories")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Housekeeping\",\"gstSlabPercent\":150}"))
                .andExpect(status().isBadRequest());
    }
}
