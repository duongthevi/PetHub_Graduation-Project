package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.KnowledgeDocument;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeDocumentRepo extends JpaRepository<KnowledgeDocument, Long> {
    boolean existsByContentHash(String contentHash);
    Optional<KnowledgeDocument> findByContentHash(String contentHash);

    @EntityGraph(attributePaths = "chunks")
    List<KnowledgeDocument> findAllByOrderByCreatedAtDesc();
}
