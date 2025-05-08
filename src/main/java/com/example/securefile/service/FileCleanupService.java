package com.example.securefile.service;

import com.example.securefile.model.SharedFile;
import com.example.securefile.repository.SharedFileRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileCleanupService {

    @Autowired
    private SharedFileRepository sharedFileRepository;

    // Run every 1 minute (for testing); production use cron
    
    @Scheduled(fixedRate = 3600000) // 1 hour = 3600000 milliseconds now it runs every 1 hour.
    @Transactional
    public void deleteExpiredFiles() {
        List<SharedFile> expiredFiles = sharedFileRepository.findByExpiryTimeBefore(LocalDateTime.now());

        for (SharedFile file : expiredFiles) {
            if (!file.isTrashed() && !file.isDownloaded()) {
                file.setTrashed(true);
                file.setTrashedAt(LocalDateTime.now());
                sharedFileRepository.save(file);
                System.out.println("🗑 Moved to Trash: " + file.getOriginalFilename());
            }
        }
    }

    // Delete trashed files permanently after 3 days
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void deleteFilesInTrash() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(3);
        List<SharedFile> toDelete = sharedFileRepository.findByTrashedTrueAndTrashedAtBefore(cutoff);

        for (SharedFile file : toDelete) {
            try {
                Path filePath = Paths.get(file.getEncryptedPath());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                    System.out.println("✅ Deleted from disk: " + filePath);
                }

                // You can mark as "permanentlyDeleted = true" instead if you prefer soft delete
                sharedFileRepository.delete(file);
                System.out.println("❌ Deleted permanently from DB: " + file.getOriginalFilename());

            } catch (IOException e) {
                System.err.println("❌ Failed to delete: " + file.getEncryptedPath());
                e.printStackTrace();
            }
        }
    }
}
