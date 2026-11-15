package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.util.Collection;
import java.util.List;

public interface CategoryService {
    Category getGeneralCategory();
    Category getIncomeCategory();
    Category getAccountTransferCategory();
    Category getOtherCategory();
    Category getRootCategoryByType(TrackingType type);
    Category getCategoryById(Long id);
    Category getCategoryByIdRestored(Long id);
    Category getCategoryRestored(Category category);
    List<Category> getAllCategories();
    boolean exists(Long id);
    boolean verifyExistingIds(Collection<Long> ids);
    boolean nameExists(String name);
    List<Long> autoSearchCategoryIds(String query, TrackingType type);
}
