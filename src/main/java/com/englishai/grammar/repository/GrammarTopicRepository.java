package com.englishai.grammar.repository;

import com.englishai.grammar.entity.GrammarTopic;
import com.englishai.common.enums.Level;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GrammarTopicRepository extends JpaRepository<GrammarTopic, UUID> {
    List<GrammarTopic> findByIsPublishedTrueOrderByOrderIndex();

    List<GrammarTopic> findByLevelAndIsPublishedTrueOrderByOrderIndex(Level level);

    Optional<GrammarTopic> findByIdAndIsPublishedTrue(UUID id);
}
