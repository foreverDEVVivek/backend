package devPilot.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repositories",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "github_repo_id"}
        ))
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Repository {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "github_repo_id", nullable = false, updatable = false)
    private Long githubRepoId;

    @Column(name = "owner")
    private String owner;

    @Column(name = "name")
    private String name;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "default_branch")
    private String defaultBranch;

    @Column(name = "description")
    private String description;

    @Column(name = "languages")
    private String languages;

    @Column(name = "html_url")
    private String htmlUrl;

    @Column(name = "index_status")
    @Enumerated(EnumType.STRING)
    private IndexStatus indexStatus = IndexStatus.PENDING;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Column(name = "chunk_count")
    private int chunkCount = 0;

    @Column(name = "files_total")
    private int filesTotal = 0;


    @Column(name = "files_processed")
    private int filesProcessed = 0;


    @Column(name = "error_message")
    private String errorMessage;


    @PrePersist
    private void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void setPrivate(boolean aPrivate) {
        this.isPrivate = aPrivate;
    }

    public void setLanguage(String s) {
        this.languages = s;
    }

    public String getLanguage() {
        return this.languages;
    }
}
