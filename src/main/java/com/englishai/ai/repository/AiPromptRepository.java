package com.englishai.ai.repository;

import com.englishai.ai.entity.AiPrompt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiPromptRepository extends JpaRepository<AiPrompt, UUID> {
    Optional<AiPrompt> findByName(String name);
}
