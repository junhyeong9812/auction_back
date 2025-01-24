package com.auction.back.domain.user.controller.query;

import com.auction.back.domain.user.dto.response.UserEmailDto;
import com.auction.back.domain.user.dto.response.UserEmailListDto;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserQueryController {

    private final UserRepository userRepository;

    /**
     * GET /api/users
     * => 사용자 목록(이메일만) 반환
     */
    @GetMapping
    public ResponseEntity<UserEmailListDto> getUserList() {
        List<User> userList = userRepository.findAll();

        // User -> UserEmailDto
        List<UserEmailDto> emailDtoList = userList.stream()
                .map(u -> new UserEmailDto(u.getEmail()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(new UserEmailListDto(emailDtoList));
    }

}
