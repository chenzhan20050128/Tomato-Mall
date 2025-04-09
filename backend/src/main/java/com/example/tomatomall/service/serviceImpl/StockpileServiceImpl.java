package com.example.tomatomall.service.serviceImpl;/*
 * @date 04/09 19:27
 */

import com.example.tomatomall.dao.StockpileRepository;
import com.example.tomatomall.po.Stockpile;
import com.example.tomatomall.service.AccountService;
import com.example.tomatomall.service.StockpileService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class StockpileServiceImpl implements StockpileService{
    @Autowired
    private StockpileRepository stockpileRepository;

    @Override
    public void updateStockpile(Stockpile stockpile) {
        Stockpile curStockpile = stockpileRepository.findById(stockpile.getId()).orElse(null);

        if (curStockpile == null) {
            stockpileRepository.save(stockpile);
            return;
        }

        // 使用BeanUtils进行属性拷贝，排除id和version字段
        BeanUtils.copyProperties(stockpile, curStockpile, "id");
        stockpileRepository.save(curStockpile);
    }
}
