package com.example.tomatomall.dao;

import com.example.tomatomall.po.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> { // String 是主键 username 的类型
    Account findByUsername(String username);
}