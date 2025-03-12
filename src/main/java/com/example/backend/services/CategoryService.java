package com.example.backend.services;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.backend.DTO.CategoryDTO;
import com.example.backend.models.Category;
import com.example.backend.repositories.CategoryRepository;

import jakarta.transaction.Transactional;

@Service
public class CategoryService {
	private final CategoryRepository categoryRepository;
	
	@Autowired
    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }
	
	public List<CategoryDTO> getAllCategories() {
        List<Category> categories = categoryRepository.findAll();
        return categories.stream()
                .map(category -> new CategoryDTO(category.getId(), category.getName(), category.getDescription()))
                .toList();
    }
	
	public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        return new CategoryDTO(category.getId(), category.getName(), category.getDescription());
    }
	
	@Transactional
    public String createCategory(CategoryDTO categoryDTO) {
        if (categoryRepository.existsByName(categoryDTO.getName())) {
            throw new RuntimeException("Category with this name already exists");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        categoryRepository.save(category);

        return "Category created successfully";
    }
	
	@Transactional
    public String updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());
        categoryRepository.save(category);

        return "Category updated successfully";
    }

    @Transactional
    public String deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        categoryRepository.delete(category);

        return "Category deleted successfully";
    }
}
