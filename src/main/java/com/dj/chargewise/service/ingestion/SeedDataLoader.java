package com.dj.chargewise.service.ingestion;

import com.dj.chargewise.model.ReasonCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "chargewise.seed", name = "enabled", havingValue = "true")
public class SeedDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedDataLoader.class);
    private static final String SEED_FOLDER = "seed/rules/";
    private static final String CONTENT_TYPE = "text/plain";

    private static final Map<String, ReasonCategory> SEED_RULES = Map.of(
            "fraud-no-authorisation.txt", ReasonCategory.FRAUD_NO_AUTHORISATION,
            "goods-not-received.txt", ReasonCategory.GOODS_NOT_RECEIVED,
            "not-as-described.txt", ReasonCategory.NOT_AS_DESCRIBED,
            "duplicate-processing.txt", ReasonCategory.DUPLICATE_PROCESSING,
            "credit-not-processed.txt", ReasonCategory.CREDIT_NOT_PROCESSED);

    private final DocumentService documentService;

    public SeedDataLoader(DocumentService documentService) {
        this.documentService = documentService;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!documentService.isEmpty()) {
            log.info("Rules already loaded, skipping seed data");
            return;
        }

        for (Map.Entry<String, ReasonCategory> rule : SEED_RULES.entrySet()) {
            byte[] content = new ClassPathResource(SEED_FOLDER + rule.getKey()).getContentAsByteArray();
            documentService.register(rule.getKey(), CONTENT_TYPE, content, rule.getValue());
        }
        log.info("Queued {} seed rule documents for ingestion", SEED_RULES.size());
    }
}
