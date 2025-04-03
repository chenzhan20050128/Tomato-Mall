package com.example.tomatomall.controller;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.Map;

@RestController
@Slf4j
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
        try{
            Map<String,Object> map = accountService.login(account);
            String token = (String) map.get("token");
            String username = (String) map.get("username");
            Integer userId = (Integer) map.get("userId");
            log.info("token:{},username:{},userId:{}",token,username,userId);
            response.setHeader("token",token);
            response.setHeader("username",username);
            response.setHeader("userId",String.valueOf(userId));
        }
        catch (IllegalArgumentException e){
            return Response.buildFailure(e.getMessage(), "400");
        }
        return Response.buildSuccess("登录成功");
    }
}