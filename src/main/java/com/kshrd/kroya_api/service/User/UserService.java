package com.kshrd.kroya_api.service.User;

import com.kshrd.kroya_api.entity.CredentialEntity;
import com.kshrd.kroya_api.payload.Auth.UserProfileUpdateRequest;
import com.kshrd.kroya_api.payload.BaseResponse;

public interface UserService {

    BaseResponse<?> getFoodsByCurrentUser(int page, int size);

    BaseResponse<?> getFoodsByUserId(Integer userId);

    BaseResponse<?> updateProfile(UserProfileUpdateRequest profileUpdateRequest);

    BaseResponse<?> deleteAccount();

    BaseResponse<?> connectWebill(CredentialEntity credentialEntity);

    BaseResponse<?> disconnectWebill();

    BaseResponse<?> getCredentialByUserId(Integer userId);

    BaseResponse<?> getDeviceTokenByUserId(Integer userId);

    BaseResponse<?> insertDeviceToken(String deviceToken);

    BaseResponse<?> getUserInfo();
}
