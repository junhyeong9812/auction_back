package com.auction.back.domain.user.service.command;

import com.auction.back.domain.user.dto.request.RegisterRequestDto;

public interface UserCommandService {
    public void registerUser(RegisterRequestDto dto);
}
