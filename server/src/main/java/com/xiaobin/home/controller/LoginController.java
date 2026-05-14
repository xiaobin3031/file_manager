package com.xiaobin.home.controller;

import com.xiaobin.home.dto.ApiResponse;
import com.xiaobin.home.dto.login.LoginDTO;
import com.xiaobin.home.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class LoginController {

    @Autowired
    private LoginService loginService;

    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginDTO dto) {
        return ApiResponse.ok(loginService.login(dto));
    }

    @PostMapping("/register")
    public void register(String username, String password) {
        this.loginService.register(username, password);
    }
}
