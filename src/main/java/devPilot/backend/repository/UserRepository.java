package devPilot.backend.repository;

import java.util.UUID;
import org.springframework.stereotype.Repository;
import devPilot.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByGithubId(Long githubId);
}
