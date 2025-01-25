package com.auction.back.domain.user.service.query;

import com.auction.back.domain.user.entity.User;

import java.util.List;

public interface UserQueryService {
    User findByEmail(String email);
    // 유저 전체 조회
    List<User> findAllUsers();
}
