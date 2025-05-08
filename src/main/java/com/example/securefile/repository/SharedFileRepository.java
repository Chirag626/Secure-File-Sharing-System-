package com.example.securefile.repository;

import com.example.securefile.model.SharedFile;
import org.springframework.data.jpa.repository.JpaRepository;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SharedFileRepository extends JpaRepository<SharedFile, String> {

    // Find files that are expired
    List<SharedFile> findByExpiryTimeBefore(LocalDateTime time);


    // Find trashed files that were trashed before a specific time
    List<SharedFile> findByTrashedTrueAndTrashedAtBefore(LocalDateTime time);

    // Find files by their unique code (primary key)
    Optional<SharedFile> findByCode(String code);

    // Find non-trashed files and those not manually deleted
    List<SharedFile> findByTrashedFalseAndManuallyDeletedFalse();

//     New method for finding trashed files that are not manually deleted
    List<SharedFile> findByTrashedTrueAndManuallyDeletedFalse();

 }
