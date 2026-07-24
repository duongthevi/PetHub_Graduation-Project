package com.dtvi.Pet.Ecommerce.service.impl;

import com.dtvi.Pet.Ecommerce.ai.OpenAiCompatibleClient;
import com.dtvi.Pet.Ecommerce.config.OpenAiProperties;
import com.dtvi.Pet.Ecommerce.dto.rag.KnowledgeDocumentRequest;
import com.dtvi.Pet.Ecommerce.dto.rag.KnowledgeDocumentResponse;
import com.dtvi.Pet.Ecommerce.entity.KnowledgeChunk;
import com.dtvi.Pet.Ecommerce.entity.KnowledgeDocument;
import com.dtvi.Pet.Ecommerce.exception.BusinessException;
import com.dtvi.Pet.Ecommerce.exception.NotFoundException;
import com.dtvi.Pet.Ecommerce.repository.KnowledgeDocumentRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeService {

    private final KnowledgeDocumentRepo documentRepo;
    private final OpenAiCompatibleClient aiClient;
    private final OpenAiProperties properties;
    private final ObjectMapper objectMapper;

    public KnowledgeDocumentResponse ingest(KnowledgeDocumentRequest request) {
        String normalizedContent = normalize(request.content());
        String hash = sha256(normalizedContent);
        if (documentRepo.existsByContentHash(hash)) {
            throw BusinessException.conflict(
                    "KNOWLEDGE_DOCUMENT_EXISTS",
                    "A document with the same content already exists"
            );
        }

        List<String> texts = chunk(normalizedContent);
        List<List<Double>> embeddings = aiClient.embed(texts);
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.title().trim());
        document.setSourceUrl(trimToNull(request.sourceUrl()));
        document.setContentHash(hash);

        for (int index = 0; index < texts.size(); index++) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setDocument(document);
            chunk.setChunkIndex(index);
            chunk.setContent(texts.get(index));
            chunk.setEmbedding(toJson(embeddings.get(index)));
            document.getChunks().add(chunk);
        }
        return KnowledgeDocumentResponse.from(documentRepo.save(document));
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> getAll() {
        return documentRepo.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(KnowledgeDocumentResponse::from)
                .toList();
    }

    public KnowledgeDocumentResponse setActive(Long id, boolean active) {
        KnowledgeDocument document = documentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Knowledge document not found"));
        document.setActive(active);
        return KnowledgeDocumentResponse.from(documentRepo.save(document));
    }

    public void delete(Long id) {
        KnowledgeDocument document = documentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Knowledge document not found"));
        documentRepo.delete(document);
    }

    private List<String> chunk(String content) {
        int chunkSize = Math.max(300, properties.getChunkSize());
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), chunkSize / 3));
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int targetEnd = Math.min(content.length(), start + chunkSize);
            int end = findBoundary(content, start, targetEnd);
            String value = content.substring(start, end).trim();
            if (!value.isBlank()) {
                chunks.add(value);
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return chunks;
    }

    private int findBoundary(String content, int start, int targetEnd) {
        if (targetEnd == content.length()) {
            return targetEnd;
        }
        int minimum = start + Math.max(100, (targetEnd - start) / 2);
        for (int index = targetEnd; index > minimum; index--) {
            char value = content.charAt(index - 1);
            if (value == '.' || value == '\n' || Character.isWhitespace(value)) {
                return index;
            }
        }
        return targetEnd;
    }

    private String normalize(String content) {
        return content.replace("\r\n", "\n")
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }

    private String toJson(List<Double> embedding) {
        try {
            return objectMapper.writeValueAsString(embedding);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot store embedding", ex);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
