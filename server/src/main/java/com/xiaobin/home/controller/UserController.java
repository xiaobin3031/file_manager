package com.xiaobin.home.controller;

import com.xiaobin.home.dto.ApiResponse;
import com.xiaobin.home.dto.LogsLoadDTO;
import com.xiaobin.home.entity.Logs;
import com.xiaobin.home.repository.LogsDao;
import com.xiaobin.home.service.LoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private LogsDao logsDao;
    @Autowired
    private LoginService loginService;

    @GetMapping
    public ApiResponse readLog(@RequestParam("logId") Long logId) {
        Logs log = this.logsDao.findOneByIdAndUserId(logId, this.loginService.getLoginId());
        if(log != null) {
            log.setRead(true);
            this.logsDao.save(log);
        }
        return ApiResponse.ok();
    }

    @GetMapping("/unreadLogCount")
    public ApiResponse unreadLogCount() {
        long l = this.logsDao.countByUserIdAndRead(this.loginService.getLoginId(), false);
        return ApiResponse.ok(l);
    }

    @GetMapping("/loadLogs")
    public ApiResponse loadLogs(LogsLoadDTO dto) {
        Page<Logs> page = this.logsDao.findAllByUserId(this.loginService.getLoginId(), PageRequest.of(dto.getPage(), dto.getLimit()));
        return ApiResponse.ok(Map.of("total", page.getTotalElements(), "items", page.getContent()));
    }
}
