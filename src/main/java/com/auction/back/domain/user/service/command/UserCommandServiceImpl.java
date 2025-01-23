package com.auction.back.domain.user.service.command;

import com.auction.back.domain.user.dto.request.RegisterRequestDto;
import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.enums.UserRole;
import com.auction.back.domain.user.enums.UserStatus;
import com.auction.back.domain.user.repository.UserRepository;
import com.auction.back.global.enums.Gender;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserCommandServiceImpl implements UserCommandService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public void registerUser(RegisterRequestDto dto) {
        // 이메일 중복 체크 등 Validation 로직 가능
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new RuntimeException("이미 가입된 이메일입니다.");
        }

        // 패스워드 암호화
        String encodedPassword = passwordEncoder.encode(dto.getPassword());

        User user = User.builder()
                .email(dto.getEmail())
                .password(encodedPassword)
                .name(dto.getName())
                .nickname(dto.getNickname())
                .phoneNumber(dto.getPhoneNumber())
                .age(dto.getAge())
                .gender(Gender.valueOf(dto.getGender())) // "MALE" -> Gender.MALE
                .status(UserStatus.ACTIVE)
                .role(UserRole.USER)
                .pointBalance(0.0)
                .build();

        userRepository.save(user);
    }
}

