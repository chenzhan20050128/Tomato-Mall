package com.example.tomatomall.service.serviceImpl;/*
 * @date 04/17 15:13
 */
import com.example.tomatomall.dao.SpecificationRepository;
import com.example.tomatomall.service.SpecificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class SpecificationServiceImpl implements SpecificationService {

    private final SpecificationRepository specificationRepository;

    public SpecificationServiceImpl(SpecificationRepository specificationRepository) {
        this.specificationRepository = specificationRepository;
    }

    @Override
    public List<String> getValuesByItem(String item) {
        log.info("Getting distinct values for item: {}", item);
        return specificationRepository.findDistinctValuesByItem(item);
    }

    @Override
    public List<Integer> getProductIdsByItemAndValue(String item, String value) {
        log.info("Searching products by item:{} and value:{}", item, value);
        return specificationRepository.findProductIdsByItemAndValue(item, value);
    }
}