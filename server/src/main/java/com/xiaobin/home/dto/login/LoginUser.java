package com.xiaobin.home.dto.login;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class LoginUser {

    @JsonIgnore
    private Integer id;

    private String token;

    private String username;

    @JsonIgnore
    private LocalDateTime loginTime;
}
