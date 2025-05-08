package com.example.securefile.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
// @NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedFile {

    @Id
    private String code; // Unique file code (used in shareable link)

    private String originalFilename;
    private String encryptedPath;

    private String uploaderName;
    private String receiverEmail;

    private LocalDateTime uploadTime;
    private LocalDateTime expiryTime;

    private boolean downloaded;

    // Trash feature
    private boolean trashed;
    private LocalDateTime trashedAt;

    // Manually Deleted flag
    private boolean manuallyDeleted;  // Added this flag

    @Transient
    private boolean expired;

    // Optional: history file flag
    @Builder.Default
    private boolean historyFile = false;

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SharedFile.class);

    // Constructors
    
    // Hibernate requires a default constructor to instantiate entity objects during queries or persistence operations.
    public SharedFile() 
    {
    	
    }
    
    public SharedFile(String code, String originalFilename, String encryptedPath,
                      String uploaderName, String receiverEmail,
                      LocalDateTime uploadTime, LocalDateTime expiryTime, boolean downloaded) {
        this.code = code;
        this.originalFilename = originalFilename;
        this.encryptedPath = encryptedPath;
        this.uploaderName = uploaderName;
        this.receiverEmail = receiverEmail;
        this.uploadTime = uploadTime;
        this.expiryTime = expiryTime;
        this.downloaded = downloaded;
        this.trashed = false;
        this.manuallyDeleted = false;  // Default to false
        this.historyFile = false; // Default to false (change to true for history files)
    }

    // Getters and Setters
    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getEncryptedPath() {
        return encryptedPath;
    }

    public void setEncryptedPath(String encryptedPath) {
        this.encryptedPath = encryptedPath;
    }

    public String getUploaderName() {
        return uploaderName;
    }

    public void setUploaderName(String uploaderName) {
        this.uploaderName = uploaderName;
    }

    public String getReceiverEmail() {
        return receiverEmail;
    }

    public void setReceiverEmail(String receiverEmail) {
        this.receiverEmail = receiverEmail;
    }

    public LocalDateTime getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(LocalDateTime uploadTime) {
        this.uploadTime = uploadTime;
    }

    public LocalDateTime getExpiryTime() {
        return expiryTime;
    }

    public void setExpiryTime(LocalDateTime expiryTime) {
        this.expiryTime = expiryTime;
    }

    public boolean isDownloaded() {
        return downloaded;
    }

    public void setDownloaded(boolean downloaded) {
        this.downloaded = downloaded;
    }

    public boolean isTrashed() {
        return trashed;
    }

    public void setTrashed(boolean trashed) {
        this.trashed = trashed;
    }

    public LocalDateTime getTrashedAt() {
        return trashedAt;
    }

    public void setTrashedAt(LocalDateTime trashedAt) {
        this.trashedAt = trashedAt;
    }

    // Manually Deleted Getter and Setter
    public boolean isManuallyDeleted() {
        return manuallyDeleted;
    }

    public void setManuallyDeleted(boolean manuallyDeleted) {
        this.manuallyDeleted = manuallyDeleted;
    }

    /**
     * Dynamic check for expiration.
     * A file is considered expired if:
     * - The current time is after the expiry time, or
     * - It has been downloaded (one-time download)
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiryTime) || this.downloaded;
    }

    /**
     * For compatibility with frontend JSON serialization.
     */
    public boolean getExpired() {
        return isExpired();
    }

    public void setExpired(boolean expired) {
        this.expired = expired;
    }

    /**
     * Returns the relative file path used for serving the file.
     */
    @Transient
    public String getFilePath() {
        return "uploads/" + encryptedPath;
    }

    /**
     * This method checks if the file is a history file (used for exclusion from auto-trash).
     * History files should not be moved to trash automatically.
     * @return true if the file is a history file, false otherwise
     */
    @Transient
    public boolean isHistoryFile() {
        boolean isHistory = this.originalFilename != null && this.originalFilename.toLowerCase().contains("history");
        if (isHistory) {
            logger.debug("History file detected: {}", this.originalFilename);
        }
        return isHistory;
    }
}
