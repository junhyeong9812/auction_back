package com.auction.back.domain.user.service.details;

import com.auction.back.domain.user.entity.User;
import com.auction.back.domain.user.repository.UserRepository;
import com.auction.back.domain.user.service.query.UserQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserQueryService userQueryService;

    /**
     * email이 username 역할
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // DB에서 사용자 조회
        User user = userQueryService.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("No user found with email: " + email);
        }

        // 스프링 Security의 org.springframework.security.core.userdetails.User 빌더 이용
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword()) // security 상 credential
                // 권한(ROLE_USER 등) 설정
                .roles(user.getRole().name()) // ex) role=USER
                .build();
    }
}
