package com.kshrd.kroya_api.service.SaleReport;

import com.kshrd.kroya_api.dto.PhotoDTO;
import com.kshrd.kroya_api.dto.UserProfileDTO;
import com.kshrd.kroya_api.entity.FoodSellEntity;
import com.kshrd.kroya_api.entity.PurchaseEntity;
import com.kshrd.kroya_api.entity.UserEntity;
import com.kshrd.kroya_api.enums.PurchaseStatusType;
import com.kshrd.kroya_api.exception.FutureDateException;
import com.kshrd.kroya_api.exception.InvalidDateFormatException;
import com.kshrd.kroya_api.exception.NotFoundExceptionHandler;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.payload.FoodSell.FoodSellCardResponse;
import com.kshrd.kroya_api.payload.Purchase.PurchaseResponse;
import com.kshrd.kroya_api.payload.SaleReport.SaleReportSummaryResponse;
import com.kshrd.kroya_api.repository.FoodSell.FoodSellRepository;
import com.kshrd.kroya_api.repository.Purchase.PurchaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SaleReportServiceImpl implements SaleReportService {

    private final PurchaseRepository purchaseRepository;
    private final FoodSellRepository foodSellRepository;
    private final ModelMapper modelMapper;

    @Override
    public BaseResponse<?> generateReportByDate(String date) {

        // Parse the date string to LocalDate
        LocalDate selectedDate;
        try {
            selectedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            log.error("Invalid date format provided: {}", date);
            throw new InvalidDateFormatException("Invalid date format. Please use yyyy-MM-dd.");
        }

        // Validate that the selected date is not after today
        LocalDate today = LocalDate.now();
        if (selectedDate.isAfter(today)) {
            throw new FutureDateException("Selected date cannot be in the future.");
        }


        // Get the currently authenticated user
        UserEntity currentUser = (UserEntity) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.info("Generating sales report for user: {}", currentUser.getEmail());

        // Fetch all FoodSell entities sold by the current user
        List<Long> userFoodSellIds = foodSellRepository.findByFoodRecipe_User_Id(currentUser.getId())
                .stream()
                .map(FoodSellEntity::getId)
                .toList();

        if (userFoodSellIds.isEmpty()) {
            log.info("No food items found for the current user: {}", currentUser.getEmail());
//            throw new NotFoundExceptionHandler("No food items found for the current user.");
        } else {
            log.info("Found {} food items for the current user: {}", userFoodSellIds.size(), currentUser.getEmail());
        }

        // Retrieve all accepted purchases for the specified date and user’s food items
        List<PurchaseEntity> purchasesOnDate = purchaseRepository.findByFoodSell_IdInAndCreatedDateBetween(
                        userFoodSellIds,
                        selectedDate.atStartOfDay(),
                        selectedDate.plusDays(1).atStartOfDay()
                ).stream()
                .filter(purchase -> purchase.getPurchaseStatusType() == PurchaseStatusType.ACCEPTED)
                .toList();

        // Retrieve all accepted purchases for the specified month and user’s food items
        LocalDateTime startOfMonth = selectedDate.withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = selectedDate.withDayOfMonth(selectedDate.lengthOfMonth()).atTime(23, 59, 59);
        List<PurchaseEntity> purchasesInMonth = purchaseRepository.findByFoodSell_IdInAndCreatedDateBetween(
                        userFoodSellIds,
                        startOfMonth,
                        endOfMonth
                ).stream()
                .filter(purchase -> purchase.getPurchaseStatusType() == PurchaseStatusType.ACCEPTED)
                .toList();

//        if (purchasesInMonth.isEmpty()) {
//            log.info("No purchases found for the selected month.");
//            throw new NotFoundExceptionHandler("No purchases found for the selected month.");
//        }

        // Calculate total sales and orders for the specified date
        double totalSalesForDate = purchasesOnDate.stream().mapToDouble(PurchaseEntity::getTotalPrice).sum();
        int totalOrdersForDate = purchasesOnDate.size();
        List<PurchaseResponse> purchaseResponsesForDate = purchasesOnDate.stream()
                .map(purchase -> mapToPurchaseResponse(purchase, currentUser))
                .collect(Collectors.toList());

        // Calculate total sales for the entire month
        double totalSalesForMonth = purchasesInMonth.stream().mapToDouble(PurchaseEntity::getTotalPrice).sum();

        // Create SaleReportFlexibleResponse for the specified date
        SaleReportSummaryResponse reportSummaryForDate;
        if (purchasesOnDate.isEmpty()) {
            reportSummaryForDate = SaleReportSummaryResponse.builder()
                    .totalSales(totalSalesForDate)
                    .totalOrders(totalOrdersForDate)
                    .message("No Items sold on this day")
                    .build();
        } else {
            reportSummaryForDate = SaleReportSummaryResponse.builder()
                    .totalSales(totalSalesForDate)
                    .totalOrders(totalOrdersForDate)
                    .purchaseResponses(purchaseResponsesForDate)
                    .build();
        }

//        // Create SaleReportSummaryResponse for the specified date
//        SaleReportSummaryResponse reportSummaryForDate = SaleReportSummaryResponse.builder()
//                .totalSales(totalSalesForDate)
//                .totalOrders(totalOrdersForDate)
//                .purchaseResponses(purchaseResponsesForDate)
//                .build();

        // Build the complete response including both the date and month summaries
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("dailySaleReport", reportSummaryForDate);
        responseMap.put("totalMonthlySales", totalSalesForMonth);

        // Throw NotFoundExceptionHandler if no data is found for the specified date
        if (totalSalesForDate == 0 && totalOrdersForDate == 0) {
            log.info("No sales data found for the selected date: {}", selectedDate);
            return BaseResponse.builder()
                    .message("No items sold on the selected date: " + selectedDate)
                    .statusCode(String.valueOf(HttpStatus.OK.value()))
                    .payload(responseMap)
                    .build();
        }

        return BaseResponse.builder()
                .message("Sales report for date: " + selectedDate + " successfully")
                .statusCode(String.valueOf(HttpStatus.OK.value()))
                .payload(responseMap)
                .build();
    }

    // Helper method to map a PurchaseEntity to PurchaseResponse
    private PurchaseResponse mapToPurchaseResponse(PurchaseEntity purchase, UserEntity currentUser) {
        FoodSellEntity foodSell = purchase.getFoodSell();

        // Map FoodSell to FoodSellCardResponse
        FoodSellCardResponse foodSellResponse = FoodSellCardResponse.builder()
                .foodSellId(foodSell.getId())
                .photo(foodSell.getFoodRecipe().getPhotos().stream()
                        .map(photo -> new PhotoDTO(photo.getId(), photo.getPhoto()))
                        .collect(Collectors.toList()))
                .name(foodSell.getFoodRecipe().getName())
                .dateCooking(foodSell.getDateCooking())
                .price(foodSell.getPrice())
                .averageRating(foodSell.getFoodRecipe().getAverageRating())
                .totalRaters(foodSell.getFoodRecipe().getTotalRaters())
                .isFavorite(false)
                .isOrderable(foodSell.getIsOrderable())
                .sellerInformation(UserProfileDTO.builder()
                        .userId(Long.valueOf(currentUser.getId()))
                        .fullName(currentUser.getFullName())
                        .phoneNumber(currentUser.getPhoneNumber())
                        .profileImage(currentUser.getProfileImage())
                        .location(currentUser.getLocation())
                        .build())
                .build();

        // Create and return PurchaseResponse
        return PurchaseResponse.builder()
                .purchaseId(purchase.getId())
                .foodSellCardResponse(foodSellResponse)
                .remark(purchase.getRemark())
                .location(purchase.getLocation())
                .paymentType(purchase.getPaymentType())
                .purchaseStatusType(purchase.getPurchaseStatusType())
                .quantity(purchase.getQuantity())
                .totalPrice(purchase.getTotalPrice())
                .buyerInformation(UserProfileDTO.builder()
                        .userId(Long.valueOf(purchase.getBuyer().getId()))
                        .fullName(purchase.getBuyer().getFullName())
                        .phoneNumber(purchase.getBuyer().getPhoneNumber())
                        .profileImage(purchase.getBuyer().getProfileImage())
                        .location(purchase.getBuyer().getLocation())
                        .build())
                .build();
    }

}

