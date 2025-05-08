package com.example.securefile.scheduler;

import com.example.securefile.model.SharedFile;
import com.example.securefile.repository.SharedFileRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AutoTrashScheduler {

    private static final Logger logger = LoggerFactory.getLogger(AutoTrashScheduler.class);
    private final SharedFileRepository repository;

    // Constructor injection for SharedFileRepository
    public AutoTrashScheduler(SharedFileRepository repository) {
        this.repository = repository;
    }

    /**
     * Scheduled task to move expired files to trash.
     * Runs every hour to check and mark expired files as trashed.
     */
    @Scheduled(fixedRate = 60 * 60 * 1000) // Every hour
    @Transactional
    public void moveExpiredFilesToTrash() {
        LocalDateTime now = LocalDateTime.now();
        logger.info("🔍 AutoTrashScheduler: Checking for expired files at {}", now);

        // Fetch files that are not trashed and not manually deleted
        List<SharedFile> expiredFiles = repository.findByTrashedFalseAndManuallyDeletedFalse().stream()
                .filter(file -> file.getExpiryTime() != null && file.getExpiryTime().isBefore(now)) // Expired files
                .filter(file -> !file.isHistoryFile()) // Exclude permanent history files
                .toList();

        if (expiredFiles.isEmpty()) {
            logger.debug("♻️ AutoTrashScheduler: No expired files found to move to trash.");
            return;
        }

        // Process each expired file
        for (SharedFile file : expiredFiles) {
            try {
                file.setTrashed(true);
                file.setTrashedAt(now);
                repository.save(file); // Persist changes to the database

                logger.info("🗑️ AutoTrashScheduler: Moved '{}' to trash.", file.getOriginalFilename());
            } catch (Exception e) {
                logger.error("❌ AutoTrashScheduler: Failed to move '{}' to trash: {}", file.getOriginalFilename(), e.getMessage());
            }
        }
        
        logger.info("✅ AutoTrashScheduler: Successfully moved {} expired files to trash.", expiredFiles.size());
    }
}
