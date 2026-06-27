package com.xiaobin.home.service;

import com.xiaobin.home.config.LoginConfig;
import com.xiaobin.home.dto.login.LoginDTO;
import com.xiaobin.home.dto.login.LoginUser;
import com.xiaobin.home.dto.login.UserFtpCache;
import com.xiaobin.home.entity.User;
import com.xiaobin.home.exception.LoginException;
import com.xiaobin.home.exception.NotLoginException;
import com.xiaobin.home.exception.RegisterException;
import com.xiaobin.home.repository.UserDao;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class LoginService {

    @Autowired
    private LoginConfig loginConfig;
    @Autowired
    private UserDao userDao;

    private static final Map<String, LoginUser> loginUserMap = new HashMap<>();
    private static final Map<String, UserFtpCache> userFtpCacheMap = new HashMap<>();

    public Integer getLoginId(){
        return getLoginUser().getId();
    }

    public LoginUser getLoginUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) return null;

        HttpServletRequest request = attributes.getRequest();
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            throw new NotLoginException();
        }

        token = token.replace("Bearer ", "");
        return getLoginUser(token);
    }

    public LoginUser getLoginUser(String token) {
        LoginUser loginUser = loginUserMap.get(token);
        if (loginUser == null) {
            throw new NotLoginException();
        }
        if (loginUser.getLoginTime().plusMinutes(this.loginConfig.getMaxAliveMinutes()).isBefore(LocalDateTime.now())) {
            loginUserMap.remove(token);
            throw new NotLoginException();
        }
        loginUser.setLoginTime(LocalDateTime.now());
        return loginUser;
    }

    public LoginUser login(LoginDTO dto) {
        User user = this.userDao.findUserByUsername(dto.getUsername());
        if (user == null) {
            if(dto.isAgree()){
                user = this.register(dto.getUsername(), dto.getPassword());
            }else{
                throw new LoginException("user not exist");
            }
        }
        String password = encodePassword(dto.getPassword());
        if (password == null) {
            throw new LoginException("password encode error");
        }
        if (!password.equals(user.getPassword())) {
            throw new LoginException("password not equal");
        }
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        LoginUser loginUser = new LoginUser();
        loginUser.setId(user.getId());
        loginUser.setToken(token);
        loginUser.setUsername(dto.getUsername());
        loginUser.setLoginTime(LocalDateTime.now());
        user.setLastLoginTime(LocalDateTime.now());
        this.userDao.save(user);
        loginUserMap.put(loginUser.getToken(), loginUser);
        return loginUser;
    }

    public User register(String username, String password) {
        User user = this.userDao.findUserByUsername(username);
        if (user != null) {
            throw new RegisterException("username exist");
        }
        password = encodePassword(password);
        if (password == null) {
            throw new RegisterException("password encode error");
        }
        user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setCreateAt(LocalDateTime.now());
        user.setDeleted(false);
        user.setUpdateAt(LocalDateTime.now());
        User save = this.userDao.save(user);
        if (save.getId() == null) {
            throw new RegisterException("password create error");
        }
        return user;
    }

    private String encodePassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            for (byte b : bytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    stringBuilder.append('0');
                }
                stringBuilder.append(hex);
            }
            return stringBuilder.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("encode password error: {}", e.getMessage(), e);
            return null;
        }
    }

    public UserFtpCache getFtpCache() {
        LoginUser loginUser = getLoginUser();
        return getFtpCache(loginUser.getToken());
    }

    private UserFtpCache getFtpCache(String token) {
        UserFtpCache ftpCache = userFtpCacheMap.get(token);
        if (ftpCache == null) {
            ftpCache = new UserFtpCache();
            userFtpCacheMap.put(token, ftpCache);
        }
        return ftpCache;
    }

}
