package com.dj.chargewise.security;

import com.dj.chargewise.config.ChargewiseProperties;
import com.dj.chargewise.dto.response.TokenResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TokenService {

    private static final String ROLE_PREFIX = "ROLE_";
    private static final String TOKEN_TYPE = "Bearer";

    private final JwtEncoder jwtEncoder;
    private final Duration tokenTtl;

    public TokenService(JwtEncoder jwtEncoder, ChargewiseProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.tokenTtl = properties.security().tokenTtl();
    }

    public TokenResponse issue(Authentication authentication) {
        Instant now = Instant.now();
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(authority -> authority.replaceFirst(ROLE_PREFIX, ""))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(SecurityConfig.ISSUER)
                .subject(authentication.getName())
                .issuedAt(now)
                .expiresAt(now.plus(tokenTtl))
                .claim(SecurityConfig.ROLES_CLAIM, roles)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, TOKEN_TYPE, tokenTtl.toSeconds());
    }
}
