package com.dj.chargewise.repository;

import com.dj.chargewise.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {
}
