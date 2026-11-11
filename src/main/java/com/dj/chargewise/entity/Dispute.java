package com.dj.chargewise.entity;

import com.dj.chargewise.model.ReasonCategory;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReasonCategory category;

    @Column(nullable = false)
    private String[] requiredEvidence;

    @Column(nullable = false)
    private String explanation;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "dispute_citations", joinColumns = @JoinColumn(name = "dispute_id"))
    @OrderColumn(name = "citation_order")
    private List<DisputeCitation> citations = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Dispute() {
    }

    public Dispute(String description, ReasonCategory category, List<String> requiredEvidence, String explanation,
                   List<DisputeCitation> citations) {
        this.description = description;
        this.category = category;
        this.requiredEvidence = requiredEvidence.toArray(String[]::new);
        this.explanation = explanation;
        this.citations = new ArrayList<>(citations);
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public ReasonCategory getCategory() {
        return category;
    }

    public List<String> getRequiredEvidence() {
        return List.of(requiredEvidence);
    }

    public String getExplanation() {
        return explanation;
    }

    public List<DisputeCitation> getCitations() {
        return List.copyOf(citations);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
