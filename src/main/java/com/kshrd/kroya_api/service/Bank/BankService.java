package com.kshrd.kroya_api.service.Bank;

import com.kshrd.kroya_api.enums.CurrencyType;
import com.kshrd.kroya_api.payload.Bank.BankRequest;
import com.kshrd.kroya_api.payload.BaseResponse;


public interface BankService {

    BaseResponse<?> addBank(BankRequest bankRequest, CurrencyType currencyType);

    BaseResponse<?> updateBank(Long bankId, BankRequest bankRequest, CurrencyType currencyType);

    BaseResponse<?> deleteBank(Long bankId);

    BaseResponse<?> getByCurrentUser();

    BaseResponse<?> getBankByUserId(Integer userId);
}
