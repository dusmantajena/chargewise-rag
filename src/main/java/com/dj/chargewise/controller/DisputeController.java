package com.dj.chargewise.controller;

import com.dj.chargewise.dto.request.AnalyseDisputeRequest;
import com.dj.chargewise.dto.response.DisputeResponse;
import com.dj.chargewise.service.dispute.DisputeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping("/analyse")
    public ResponseEntity<DisputeResponse> analyse(@Valid @RequestBody AnalyseDisputeRequest request) {
        DisputeResponse response = disputeService.analyse(request);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/v1/disputes/{id}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public DisputeResponse get(@PathVariable UUID id) {
        return disputeService.get(id);
    }
}
