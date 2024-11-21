package com.kshrd.kroya_api.service.User;

import com.kshrd.kroya_api.entity.CredentialEntity;
import com.kshrd.kroya_api.payload.Auth.UserProfileUpdateRequest;
import com.kshrd.kroya_api.payload.BaseResponse;

public interface UserService {
    BaseResponse<?> getFoodsByCurrentUser();

    BaseResponse<?> getFoodsByUserId(Integer userId);

    BaseResponse<?> updateProfile(UserProfileUpdateRequest profileUpdateRequest);

    BaseResponse<?> deleteAccount();

    BaseResponse<?> connectWebill(CredentialEntity credentialEntity);

    BaseResponse<?> disconnectWebill();

    BaseResponse<?> getWebillAccNoByUserId(Integer userId);

    BaseResponse<?> getDeviceTokenByUserId(Integer userId);

    BaseResponse<?> insertDeviceToken(String deviceToken);

    BaseResponse<?> getUserInfo();
}
