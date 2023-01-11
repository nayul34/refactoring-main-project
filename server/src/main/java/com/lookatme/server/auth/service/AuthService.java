package com.lookatme.server.auth.service;

import com.lookatme.server.auth.jwt.JwtTokenizer;
import com.lookatme.server.auth.jwt.RedisRepository;
import com.lookatme.server.exception.ErrorCode;
import com.lookatme.server.exception.ErrorLogicException;
import com.lookatme.server.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class AuthService {

    private final RedisRepository redisRepository;
    private final JwtTokenizer jwtTokenizer;
    private final PasswordEncoder passwordEncoder;

    public void logout(String accessToken, String memberUniqueKey) {
        redisRepository.expireRefreshToken(memberUniqueKey); // 1. redis에 저장된 Refresh 토큰 삭제 (액세스 토큰 재발급 방지)
        redisRepository.addAccessTokenToBlacklist(accessToken); // 2. 액세스 토큰을 블랙리스트에 추가해 해당 토큰 사용 못하도록 차단
    }

    // 단순 전달용 메서드
    public void addAccessTokenToBlacklist(String accessToken) {
        if(accessToken.startsWith("Bearer")) {
            accessToken = accessToken.replace("Bearer ", "");
        }
        redisRepository.addAccessTokenToBlacklist(accessToken);
    }

    public void encodePassword(Member member) {
        member.encodePassword(passwordEncoder);
    }

    public String getMemberUniqueKeyAtToken(String token) {
        return jwtTokenizer.getTokenSubject(token);
    }

    public String reissueAccessToken(String refreshToken, Member member) {
        // 1. 유효한 RTK인지? (redis에 저장되어 있는지?)
        String tokenSubject = jwtTokenizer.getTokenSubject(refreshToken);

        if (!redisRepository.hasRefreshToken(tokenSubject, refreshToken)) {
            throw new ErrorLogicException(ErrorCode.TOKEN_INVALID);
        }
        // 2. ATK 재발급
        String accessToken = jwtTokenizer.delegateAccessToken(member);
        return accessToken;
    }
}