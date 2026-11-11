package com.dj.chargewise.repository;

import com.dj.chargewise.entity.Dispute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {
}
