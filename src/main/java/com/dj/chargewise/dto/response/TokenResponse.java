package com.dj.chargewise.dto.response;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
