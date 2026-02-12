package com.example.test.service;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.example.test.dto.TestDto;

@Mapper
public interface TestMapper {
    List<TestDto> findAll();
}