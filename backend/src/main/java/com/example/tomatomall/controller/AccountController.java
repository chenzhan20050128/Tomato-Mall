package com.example.tomatomall.controller;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Resource
    AccountService accountService;

    /**
     * 获取用户详情
     */
    @GetMapping("/{username}")
    public Response<Account> getUser(@PathVariable String username) {
        return accountService.getUserDetail(username);
    }

    /**
     * 创建新的用户
     */
    @PostMapping()
    public Response<String> createUser(@Valid @RequestBody Account account) {
        return accountService.createUser(account);
    }

    /**
     * 更新用户信息
     */
    @PutMapping()
    public Response<String> updateUser(@Valid @RequestBody Account account) {
        return accountService.updateUser(account);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Response<String> login(@RequestBody Account account) {
        return accountService.login(account.getUsername(), account.getPassword());
    }
}