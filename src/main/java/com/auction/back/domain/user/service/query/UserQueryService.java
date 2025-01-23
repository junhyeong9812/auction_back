package com.auction.back.domain.user.service.query;

import com.auction.back.domain.user.entity.User;

public interface UserQueryService {
    public User findByEmail(String email);
}
