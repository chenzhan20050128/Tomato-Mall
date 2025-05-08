package com.example.tomatomall.controller;/*
 * @date 04/17 15:16
 */
import com.example.tomatomall.service.SpecificationService;
import com.example.tomatomall.vo.Response;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/specification")
@RequiredArgsConstructor
public class SpecificationController {

    private final SpecificationService specificationService;

    // 获取某个item的所有可能值
    @GetMapping("/items/{item}/values")
    public Response<List<String>> getValuesByItem(@PathVariable String item) {
        return Response.buildSuccess(specificationService.getValuesByItem(item));
    }

    // 根据item和value获取商品ID列表
    @GetMapping("/items/{item}/values/{value}/product-ids")
    public Response<List<Integer>> getProductIdsBySpecification(
            @PathVariable String item,
            @PathVariable String value)
    {
        return Response.buildSuccess(
                specificationService.getProductIdsByItemAndValue(item, value)
        );
    }
}