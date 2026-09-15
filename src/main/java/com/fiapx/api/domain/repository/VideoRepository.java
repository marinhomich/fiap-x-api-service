package com.fiapx.api.domain.repository;

import com.fiapx.api.domain.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Page<Video> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Optional<Video> findByIdAndUserId(Long id, Long userId);
}
