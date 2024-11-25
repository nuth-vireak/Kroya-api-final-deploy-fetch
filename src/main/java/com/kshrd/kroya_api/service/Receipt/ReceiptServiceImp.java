package com.kshrd.kroya_api.service.Receipt;

import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.*;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.Receipt.ReceiptResponse;
import com.kshrd.kroya_api.repository.Favorite.FavoriteRepository;
import com.kshrd.kroya_api.repository.Purchase.PurchaseRepository;
import com.kshrd.kroya_api.repository.Receipt.RecipeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptServiceImp implements ReceiptService {

    private final RecipeRepository recipeRepository;
    private final PurchaseRepository purchaseRepository;
    private final FavoriteRepository favoriteRepository;
    private final ModelMapper modelMapper;

    @Override
    public BaseResponse<?> getReceiptByPurchaseId(Long purchaseId) {
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // Retrieve the purchase and check if it belongs to the authenticated user
        PurchaseEntity purchase = purchaseRepository.findById(purchaseId)
                .orElseThrow(() -> new NotFoundExceptionHandler("Purchase not found!"));

        FoodSellEntity product = purchase.getFoodSell();
        UserEntity seller = product.getFoodRecipe().getUser();

        // Check if the current user is either the buyer or the seller
        if (!currentUser.getId().equals(seller.getId()) && !currentUser.getId().equals(purchase.getBuyer().getId())) {
            throw new NotFoundExceptionHandler("You are not authorized to view this receipt.");
        }

        RecipeEntity receipt = recipeRepository.findByPurchase_Id(purchaseId)
                .orElseThrow(() -> new NotFoundExceptionHandler("Receipt not found!"));

        FoodSellCardResponse foodSellCardResponse = FoodSellCardResponse.builder()
                .foodSellId(product.getId())
                .photo(product.getFoodRecipe().getPhotos().stream()
                        .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                        .collect(Collectors.toList()))
                .name(product.getFoodRecipe().getName())
                .dateCooking(product.getDateCooking())
                .price(product.getPrice())
                .averageRating(product.getFoodRecipe().getAverageRating())
                .totalRaters(product.getFoodRecipe().getTotalRaters())
                .isFavorite(favoriteRepository.existsByUserAndFoodSell(currentUser, product))
                .isOrderable(product.getIsOrderable())
                .sellerInformation(UserProfileDTO.builder()
                        .userId(Long.valueOf(product.getFoodRecipe().getUser().getId()))
                        .fullName(product.getFoodRecipe().getUser().getFullName())
                        .phoneNumber(product.getFoodRecipe().getUser().getPhoneNumber())
                        .profileImage(product.getFoodRecipe().getUser().getProfileImage())
                        .location(product.getFoodRecipe().getUser().getLocation())
                        .build())
                .build();

        ReceiptResponse receiptResponse = ReceiptResponse.builder()
                .recipeId(receipt.getId())
                .purchaseId(purchase.getId())
                .foodSellCardResponse(foodSellCardResponse)
                .reference(receipt.getReference())
                .orderDate(receipt.getPaidDate())
                .paidBy(receipt.getPaidBy())
                .payer(purchase.getBuyer().getFullName())
                .seller(product.getFoodRecipe().getUser().getFullName())
                .sellerPhoneNumber(product.getFoodRecipe().getUser().getPhoneNumber())
                .quantity(purchase.getQuantity())
                .totalPrice(purchase.getTotalPrice())
                .build();

        return BaseResponse.builder()
                .message("Receipt retrieved successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(receiptResponse)
                .build();
    }


}
