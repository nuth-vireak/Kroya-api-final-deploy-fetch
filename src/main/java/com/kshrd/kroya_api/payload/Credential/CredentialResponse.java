package com.kshrd.kroya_api.payload.Credential;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialResponse {
    private Integer id;
    private String AccountNo;
    private Integer userId;
}
