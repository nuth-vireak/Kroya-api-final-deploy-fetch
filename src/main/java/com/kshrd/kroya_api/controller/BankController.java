package com.kshrd.kroya_api.controller;

import com.kshrd.kroya_api.enums.CurrencyType;
import com.kshrd.kroya_api.payload.Bank.BankRequest;
import com.kshrd.kroya_api.payload.BaseResponse;
import com.kshrd.kroya_api.service.Bank.BankService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/banks")
@RequiredArgsConstructor
@Slf4j
public class BankController {

    private final BankService bankService;

    @Operation(
            summary = "🏦 Create a New Bank Account",
            description = "Creates a new bank account with the specified details.")
    @PostMapping
    public BaseResponse<?> addBank(
            @Valid @RequestBody BankRequest bankRequest,
            @RequestParam CurrencyType currencyType) {
        log.info("Creating a new bank account");
        return bankService.addBank(bankRequest, currencyType);
    }

    @Operation(
            summary = "🔍 Get Bank Account by Current User",
            description = "Retrieves the bank account associated with the currently authenticated user.")
    @GetMapping
    public BaseResponse<?> getByCurrentUser() {
        return bankService.getByCurrentUser();
    }

    @Operation(
            summary = "🔍 Get Bank Account by User ID",
            description = "Retrieves the bank account associated with the specified user ID.")
    @GetMapping("/{userId}")
    public BaseResponse<?> getBankByUserId(@PathVariable Integer userId) {
        return bankService.getBankByUserId(userId);
    }

    @Operation(
            summary = "✏️ Update Bank Account",
            description = "Updates an existing bank account by ID with the provided details.")
    @PutMapping("/{id}")
    public BaseResponse<?> updateBank(
            @PathVariable Long id,
            @RequestBody BankRequest bankRequest,
            @RequestParam CurrencyType currencyType) {
        log.info("Updating bank account with ID: {}", id);
        return bankService.updateBank(id, bankRequest, currencyType);
    }

    @Operation(
            summary = "🗑️ Delete Bank Account",
            description = " Deletes an existing bank account by ID.")
    @DeleteMapping("/{id}")
    public BaseResponse<?> deleteBank(@PathVariable Long id) {
        log.info("Deleting bank account with ID: {}", id);
        return bankService.deleteBank(id);
    }
}
