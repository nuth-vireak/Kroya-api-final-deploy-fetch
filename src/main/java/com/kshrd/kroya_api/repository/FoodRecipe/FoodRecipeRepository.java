package com.kshrd.kroya_api.repository.FoodRecipe;

import com.kshrd.kroya_api.entity.FoodRecipeEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRecipeRepository extends JpaRepository<FoodRecipeEntity, Integer> {
    List<FoodRecipeEntity> findByCategoryId(Long categoryId);

    List<FoodRecipeEntity> findAllByOrderByAverageRatingDesc();

    List<FoodRecipeEntity> findByUserId(Integer id);

    List<FoodRecipeEntity> findByCuisineId(Long cuisineId);

    List<FoodRecipeEntity> findByNameContainingIgnoreCase(String name);

    Page<FoodRecipeEntity> findByUserId(Integer userId, Pageable pageable);

}
