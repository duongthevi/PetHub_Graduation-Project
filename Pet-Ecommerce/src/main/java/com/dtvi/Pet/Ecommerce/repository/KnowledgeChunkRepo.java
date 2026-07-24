package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.KnowledgeChunk;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepo extends JpaRepository<KnowledgeChunk, Long> {
    @EntityGraph(attributePaths = "document")
    List<KnowledgeChunk> findByDocumentActiveTrue();
}
