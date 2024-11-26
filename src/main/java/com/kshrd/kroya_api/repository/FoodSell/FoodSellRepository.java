package com.kshrd.kroya_api.repository.FoodSell;

import com.kshrd.kroya_api.entity.FoodRecipeEntity;
import com.kshrd.kroya_api.entity.FoodSellEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface FoodSellRepository extends JpaRepository<FoodSellEntity, Long> {

    @Query("""
            SELECT fs FROM FoodSellEntity fs
            WHERE fs.foodRecipe.category.id = :categoryId
            """)
    List<FoodSellEntity> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("""
            SELECT fs FROM FoodSellEntity fs
            ORDER BY fs.foodRecipe.averageRating DESC
            """)
    List<FoodSellEntity> findAllByOrderByAverageRatingDesc();

    boolean existsByFoodRecipe(FoodRecipeEntity foodRecipeEntity);

    Optional<FoodSellEntity> findByFoodRecipe(FoodRecipeEntity foodRecipe);

    List<FoodSellEntity> findByFoodRecipeUserId(Integer id);

    @Query("""
        SELECT fs FROM FoodSellEntity fs
        WHERE fs.foodRecipe.cuisine.id = :cuisineId
        """)
    List<FoodSellEntity> findByCuisineId(@Param("cuisineId") Long cuisineId);

    List<FoodSellEntity> findByFoodRecipeNameContainingIgnoreCase(String name);

    List<FoodSellEntity> findByFoodRecipe_User_Id(Integer id);

    Collection<Object> findByFoodRecipe_User_IdAndFoodRecipe_NameContainingIgnoreCase(Integer userId, String name);

    List<FoodSellEntity> findAllByOrderByIdDesc();
}
