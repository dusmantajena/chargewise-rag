package com.dj.chargewise.model;

import com.dj.chargewise.dto.response.DisputeResponse;

public record IdempotentResult(DisputeResponse response, boolean replayed) {
}
