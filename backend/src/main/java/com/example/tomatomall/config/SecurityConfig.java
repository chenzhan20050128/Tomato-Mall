package com.example.tomatomall.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .csrf().disable()  // 禁用CSRF保护
                .authorizeRequests()
                .antMatchers("/api/accounts/login", "/api/accounts", "/api/products/**").permitAll()  // 允许登录和注册接口匿名访问
                .anyRequest().authenticated()  // 其他请求需要认证
                .and()
                .formLogin().disable()  // 禁用默认登录表单
                .httpBasic().disable(); // 禁用HTTP Basic认证
    }
}