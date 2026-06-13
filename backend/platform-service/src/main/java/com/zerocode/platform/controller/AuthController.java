package com.zerocode.platform.controller;

import com.zerocode.platform.dto.LoginRequest;
import com.zerocode.platform.dto.RegisterRequest;
import com.zerocode.platform.service.UserService;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.LoginVO;
import com.zerocode.platform.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(userService.register(request.username(), request.password()));
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(userService.login(request.username(), request.password()));
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout() {
        userService.logout();
        return ApiResponse.ok(true);
    }

    @GetMapping("/me")
    public ApiResponse<UserVO> me() {
        return ApiResponse.ok(userService.getCurrentUser());
    }
}
