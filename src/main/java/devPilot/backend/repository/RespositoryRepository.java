package devPilot.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RespositoryRepository extends JpaRepository<devPilot.backend.entity.Repository, UUID> {

    List<devPilot.backend.entity.Repository> findByUserIdOrderByFullNameAsc(UUID userId);

    List<devPilot.backend.entity.Repository> findByUserIdAndIsPrivateOrderByFullNameAsc(UUID userId, boolean isPrivate);

    Optional<devPilot.backend.entity.Repository> findByIdAndUserId(UUID id, UUID userId);

    Optional<devPilot.backend.entity.Repository> findByUserIdAndGithubRepoId(UUID userId, Long githubRepoId);
}
