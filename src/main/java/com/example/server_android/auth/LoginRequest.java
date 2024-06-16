package com.example.server_android.auth;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NonNull
    private String email;
    @NonNull
    private String password;
}
