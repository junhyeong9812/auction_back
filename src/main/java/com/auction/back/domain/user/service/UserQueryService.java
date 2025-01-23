package com.auction.back.domain.user.service;

import com.auction.back.domain.user.entity.User;

public interface UserQueryService {
    public User findByEmail(String email);
}
