package com.example.tomatomall.controller;/*
 * @date 05/15 10:57
 */

import com.example.tomatomall.dto.RecommendationResponse;
import com.example.tomatomall.service.ProductService;
import com.example.tomatomall.service.RagService;
import com.example.tomatomall.vo.Response;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/recommend")
public class RagController {

    @Autowired
    private RagService ragService;

    @Autowired
    private ProductService productService; // 注入商品服务



    @PostMapping
    public Response<String> recommend(
            @RequestBody Map<String, String> request,
            @RequestAttribute("userId") Integer userId
    ) {

        if (userId == null) {
            log.error("用户ID为空，拒绝请求");
            return Response.buildFailure("用户ID不能为空", "400");
        }

        String userQuery = request.get("query");


        String response = ragService.recommend(userQuery, userId);


        return Response.buildSuccess(response);
    }
}