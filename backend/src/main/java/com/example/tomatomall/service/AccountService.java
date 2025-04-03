package com.example.tomatomall.service;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.vo.Response;

import java.util.Map;

public interface AccountService {
    String createUser(Account account);
    Account getUserDetail(String username);
    String updateUser(Account account);
    Map<String,Object> login(Account account);
}