package com.example.tomatomall.controller;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.vo.Response;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
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
        Account account = accountService.getUserDetail(username);
        if (account != null) {
            return Response.buildSuccess(account);
        }
        return Response.buildFailure("用户不存在", "400");
    }

    /**
     * 创建新的用户
     */
    @PostMapping()
    public Response<String> createUser(@Valid @RequestBody Account account) {
        String result = accountService.createUser(account);
        if (result == "注册成功"){
            return Response.buildSuccess(result);
        }

        return Response.buildFailure(result,"400");
    }

    /**
     * 更新用户信息
     */
    @PutMapping()
    public Response<String> updateUser(@Valid @RequestBody Account account) {
        String result = accountService.updateUser(account);
        if (result == "更新成功"){
            return Response.buildSuccess(result);
        }
        return Response.buildFailure(result,"400");
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Response<String> login(@RequestBody Account account, HttpServletResponse response) {
        Response<String> loginResult = accountService.login(account);

        // 如果登录成功，在响应头中设置token
        if ("200".equals(loginResult.getCode()) && loginResult.getData() != null) {
            response.addHeader("token", loginResult.getData());
        }

        return loginResult;
    }
}