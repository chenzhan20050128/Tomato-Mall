package com.example.tomatomall.service;

import com.example.tomatomall.po.Account;
import com.example.tomatomall.vo.Response;

public interface AccountService {
    String createUser(Account account);
    Account getUserDetail(String username);
    String updateUser(Account account);
    Response<String> login(Account account);
    //注意，只有login这个方法不改Response封装 陈展 0402
}