package com.example.tomatomall.controller;

import com.example.tomatomall.vo.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.data.redis.RedisConnectionFailureException;
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Response<String> handleException(Exception e) {
        logger.error("发生异常:", e);
        return Response.buildFailure("服务器内部错误: " + e.getMessage(), "500");
    }


    @ExceptionHandler(RedisConnectionFailureException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public Response<String> handleRedisConnectionException(RedisConnectionFailureException e) {
        logger.error("Redis连接异常: {}", e.getMessage());
        return Response.buildFailure("系统暂时不可用，请稍后重试", "503");
    }
}