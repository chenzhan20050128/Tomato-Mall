package com.example.tomatomall.po;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer userId;

    @Column(length = 50, nullable = false)
    @NotEmpty(message = "用户名不能为空")
    private String username;

    @Column(length = 100, nullable = false)
    @NotEmpty(message = "密码不能为空")
    private String password;
    // 注意：这个 password 字段仅用于接收前端传递的密码，Service层会处理加密存储，返回时不包含密码

    @Column(length = 50, nullable = false)
    @NotEmpty(message = "姓名不能为空")
    private String name;

    @Column(length = 255)
    private String avatar;

    @Column(length = 11)
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String telephone;

    @Column(length = 100)
    @Email(message = "邮箱格式不正确")
    private String email;

    @Column(length = 255)
    private String location;


    // 1.管理员 2.用户 3.商家
    // 修改role字段类型
    @Column(nullable = false, columnDefinition = "VARCHAR(50) DEFAULT 'user'")
    private String role;
}