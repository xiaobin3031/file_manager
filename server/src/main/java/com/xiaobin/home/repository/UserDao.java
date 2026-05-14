package com.xiaobin.home.repository;

import com.xiaobin.home.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDao extends JpaRepository<User, Integer> {

    User findUserByUsername(String username);
}
