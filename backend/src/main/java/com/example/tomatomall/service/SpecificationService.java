package com.example.tomatomall.service;
// SpecificationService.java
import java.util.List;

public interface SpecificationService {
    List<String> getValuesByItem(String item);
    List<Integer> getProductIdsByItemAndValue(String item, String value);
}
