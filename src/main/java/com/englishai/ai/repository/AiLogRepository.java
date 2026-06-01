package com.englishai.ai.repository;

import com.englishai.ai.entity.AiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface AiLogRepository extends JpaRepository<AiLog, UUID> {
    long countByUserIdAndCreatedAtAfter(UUID userId, Instant since);

    @Query("SELECT COALESCE(SUM(al.totalTokens), 0) FROM AiLog al WHERE al.user.id = :userId AND al.createdAt BETWEEN :start AND :end")
    long sumTotalTokensByUserIdAndCreatedAtBetween(
            @Param("userId") UUID userId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );
}
