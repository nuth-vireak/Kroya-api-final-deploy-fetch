package com.kshrd.kroya_api.service.Category;

import com.kshrd.kroya_api.entity.CategoryEntity;
import com.kshrd.kroya_api.exception.DuplicateFieldExceptionHandler;
import com.kshrd.kroya_api.exception.InvalidValueExceptionHandler;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.Category.CategoryRequest;
import com.kshrd.kroya_api.repository.Category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImp implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    public BaseResponse<?> postCategory(CategoryRequest categoryRequest) {
        String categoryName = categoryRequest.getCategoryName();
        log.info("Received request to create a category with name: {}", categoryName);

        // Validate category name to ensure it does not contain numbers, special characters, or blank spaces
        if (!categoryName.matches("^[a-zA-Z\\s]+$")) {
            log.warn("Invalid category name: '{}'. It must contain only letters and spaces.", categoryName);
            throw new InvalidValueExceptionHandler("Category name can only contain letters and spaces, without numbers or special characters.");
        }

        // Check if a category with the same name already exists
        Optional<CategoryEntity> existingCategory = categoryRepository.findByCategoryName(categoryName);
        if (existingCategory.isPresent()) {
            log.warn("Category with name '{}' already exists", categoryName);
            throw new DuplicateFieldExceptionHandler("Category name: '" + categoryName + "' already exists.");
        }

        // Save the new category to the database
        CategoryEntity categoryEntity = modelMapper.map(categoryRequest, CategoryEntity.class);
        categoryRepository.save(categoryEntity);
        log.info("Category saved successfully with ID: {}", categoryEntity.getId());

        // Build and return a successful response
        return BaseResponse.builder()
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(categoryEntity)
                .message("Category has been created successfully")
                .build();
    }

    @Override
    public BaseResponse<?> getAllCategory() {
        log.info("Fetching all categories...");
        List<CategoryEntity> categories = categoryRepository.findAllByOrderById();

        if (categories.isEmpty()) {
            log.info("No categories found.");
            return BaseResponse.builder()
                    .message("No categories found")
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .build();
        }

        log.info("Categories fetched successfully, count: {}", categories.size());
        return BaseResponse.builder()
                .payload(categories)
                .message("Categories fetched successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .build();
    }
}
