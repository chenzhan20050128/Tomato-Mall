package com.example.tomatomall.dao;

import com.example.tomatomall.po.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> { // String 是主键 username 的类型
    Account findByUserId(Integer userId);

    Account findByUsername(String username);
    /*
    JPA特性：
    findBy属性名 会转换为 SELECT * FROM table WHERE 属性名 = ?
所以findByUsername会变成SELECT * FROM users WHERE username = ?
findByTelephone会变成SELECT * FROM users WHERE telephone = ?
     */
    Account findByTelephone(String telephone);
}