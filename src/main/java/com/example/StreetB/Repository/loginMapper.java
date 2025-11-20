package com.example.StreetB.Repository;

import com.example.StreetB.dto.loginDTO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface loginMapper {
    loginDTO findById(loginDTO loginDTO);
    int signIn(loginDTO loginDTO);
    int checkId(String id);
}
