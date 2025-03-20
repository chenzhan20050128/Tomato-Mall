package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.dao.AccountRepository;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.vo.Response;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

@Service
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder; // Spring Security 提供的密码加密工具

    @Override
    public Response<String> createUser(Account account) {
        if (accountRepository.findByUsername(account.getUsername()) != null) {
            return Response.buildFailure("用户名已存在", "400");
        }
        // 密码加密存储
        account.setPassword(bCryptPasswordEncoder.encode(account.getPassword()));
        account.setRole(2);
        accountRepository.save(account);
        return Response.buildSuccess("创建用户成功");
    }

    @Override
    public Response<Account> getUserDetail(String username) {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            return Response.buildFailure("用户不存在", "400");
        }
        account.setPassword(null); //  返回用户信息时，密码字段设为 null，防止泄露
        return Response.buildSuccess(account);
    }

    @Override
    public Response<String> updateUser(Account account) {
        Account existingAccount = accountRepository.findByUsername(account.getUsername());
        if (existingAccount == null) {
            return Response.buildFailure("用户不存在", "400");
        }
        // 更新用户信息，允许部分更新
        if (account.getName() != null) {
            existingAccount.setName(account.getName());
        }
        if (account.getAvatar() != null) {
            existingAccount.setAvatar(account.getAvatar());
        }
        if (account.getTelephone() != null) {
            existingAccount.setTelephone(account.getTelephone());
        }
        if (account.getEmail() != null) {
            existingAccount.setEmail(account.getEmail());
        }
        if (account.getLocation() != null) {
            existingAccount.setLocation(account.getLocation());
        }
        accountRepository.save(existingAccount);
        return Response.buildSuccess("用户信息更新成功");
    }

    @Override
    public Response<String> login(String account, String password) {
        if (account == null || password == null) {
            return Response.buildFailure("账号或密码不能为空", "400");
        }
        Account userAccount = null;
        // 判断输入的是手机号还是用户名
        if (account.matches("^1\\d{10}$")) {
            userAccount = accountRepository.findByTelephone(account);
        } else {
            userAccount = accountRepository.findByUsername(account);
        }
        // 先检查用户是否存在
        if (userAccount == null) {
            return Response.buildFailure("用户不存在/用户密码错误", "400");
        }
        // 校验密码 - 使用BCrypt对输入的密码和数据库中存储的加密密码进行比对
        if (!bCryptPasswordEncoder.matches(password, userAccount.getPassword())) {
            return Response.buildFailure("用户不存在/用户密码错误", "400");
        }
        return Response.buildSuccess("登录成功");
    }
}