package com.example.mtfinance.src.repositories;

import com.example.mtfinance.src.trackingengine.Category;
import com.example.mtfinance.src.trackingengine.TrackingType;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;

    @Inject
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public Category getGeneralCategory() {
        return categoryRepository.getGeneralCategory();
    }

    @Override
    public Category getIncomeCategory() {
        return categoryRepository.getIncomeCategory();
    }

    @Override
    public Category getAccountTransferCategory() {
        return categoryRepository.getAccountTransferCategory();
    }

    @Override
    public Category getOtherCategory() {
        return categoryRepository.getOtherCategory();
    }

    @Override
    public Category getRootCategoryByType(TrackingType type) {
        return categoryRepository.getRootCategoryByType(type);
    }

    @Override
    public Category getCategoryById(Long id) {
        return categoryRepository.getCategoryById(id);
    }

    @Override
    public Category getCategoryByIdRestored(Long id) {
        return categoryRepository.getCategoryByIdRestored(id);
    }

    @Override
    public Category getCategoryRestored(Category category) {
        return categoryRepository.getCategoryRestored(category);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.getAllCategories();
    }

    @Override
    public boolean exists(Long id) {
        return categoryRepository.exists(id);
    }

    @Override
    public boolean verifyExistingIds(Collection<Long> ids) {
        return categoryRepository.verifyExistingIds(ids);
    }

    @Override
    public boolean nameExists(String name) {
        return categoryRepository.nameExists(name);
    }

    @Override
    public List<Long> autoSearchCategoryIds(String query, TrackingType type) {
        return categoryRepository.autoSearchCategoryIds(query, type);
    }
}
