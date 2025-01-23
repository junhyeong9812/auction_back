package com.auction.back.domain.user.controller.command;
import com.auction.back.domain.user.dto.request.RegisterRequestDto;
import com.auction.back.domain.user.service.command.UserCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserCommandController {

    private final UserCommandService userCommandService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDto dto) {
        userCommandService.registerUser(dto);
        return ResponseEntity.ok("회원가입 성공");
    }
}

