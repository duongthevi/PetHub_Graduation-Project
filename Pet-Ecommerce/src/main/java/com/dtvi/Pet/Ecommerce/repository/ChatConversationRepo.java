package com.dtvi.Pet.Ecommerce.repository;

import com.dtvi.Pet.Ecommerce.entity.ChatConversation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatConversationRepo extends JpaRepository<ChatConversation, String> {
    @EntityGraph(attributePaths = {"messages", "user"})
    Optional<ChatConversation> findWithMessagesById(String id);
}
