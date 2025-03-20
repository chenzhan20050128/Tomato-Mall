package com.example.tomatomall.service;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.vo.Response;

public interface AccountService {
    Response<String> createUser(Account account);
    Response<Account> getUserDetail(String username);
    Response<String> updateUser(Account account);
    Response<String> login(String username, String password);
}