package com.example.tomatomall.service.serviceImpl;

import com.example.tomatomall.constant.RoleConstant;
import com.example.tomatomall.dao.AccountRepository;
import com.example.tomatomall.po.Account;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.util.JwtUtil;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    @Resource
    private AccountRepository accountRepository;

    @Resource
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Resource
    private JwtUtil jwtUtil;

    @Override
    public String createUser(Account account) {
        if (accountRepository.findByUsername(account.getUsername()) != null) {
            return "用户名已存在";
        }

        // 密码加密存储
        account.setPassword(bCryptPasswordEncoder.encode(account.getPassword()));

        // 确保有默认角色
        if (account.getRole() == null || account.getRole().trim().isEmpty()) {
            account.setRole(RoleConstant.USER); // 默认为普通用户
        }

        accountRepository.save(account);
        return "注册成功";
    }

    @Override
    public Account getUserDetail(String username) {
        Account account = accountRepository.findByUsername(username);
        if (account == null) {
            return null;
        }
        account.setPassword(null); // 返回用户信息时，密码字段设为 null，防止泄露
        return account;
    }

    @Override
    public String updateUser(Account account) {
        Account existingAccount = accountRepository.findByUsername(account.getUsername());
        if (existingAccount == null) {
            return "用户不存在";
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
        if (account.getPassword() != null && !account.getPassword().isEmpty()) {
            existingAccount.setPassword(bCryptPasswordEncoder.encode(account.getPassword()));
        }
        if (account.getRole() != null) {
            existingAccount.setRole(account.getRole());
        }
        accountRepository.save(existingAccount);
        return "更新成功";
    }

    @Override
    public Map<String,Object> login(Account account) {
        if (account == null || account.getPassword() == null) {
            throw new IllegalArgumentException("用户名或密码不能为空");
        }
        Account userAccount = null;
        userAccount = accountRepository.findByUsername(account.getUsername());

        // 先检查用户是否存在
        if (userAccount == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        // 校验密码 - 使用BCrypt对输入的密码和数据库中存储的加密密码进行比对
        if (!bCryptPasswordEncoder.matches(account.getPassword(), userAccount.getPassword())) {
            throw new IllegalArgumentException("密码错误");
        }
        // 生成JWT令牌 cz0401 20:59
        log.info("AccountService:userId:{}", userAccount.getUserId());
        log.info("AccountService:username:{}", userAccount.getUsername());

        String token = jwtUtil.generateToken(userAccount.getUserId(), userAccount.getUsername());
        Map<String,Object> res = new HashMap<>();
        res.put("token", token);
        res.put("username", userAccount.getUsername()); // 使用userAccount
        res.put("userId", userAccount.getUserId());     // 使用userAccount
        return res;
    }
}