package devPilot.backend.services;

import devPilot.backend.dto.IndexStatusResponse;
import devPilot.backend.dto.RepositoryResponse;
import devPilot.backend.entity.Repository;
import devPilot.backend.entity.User;
import devPilot.backend.exceptions.NotFoundException;
import devPilot.backend.repository.RespositoryRepository;
import devPilot.backend.services.github.GithubApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepoService {
      private final RespositoryRepository repository;
      private final UserService userService;
      private final GithubApiClient apiClient;

      @Transactional(readOnly = true)
      public List<RepositoryResponse> listSorted(UUID userId, String isPrivate) {

          List<Repository> repos;

          if ("all".equalsIgnoreCase(isPrivate)) {
              repos = repository.findByUserIdOrderByFullNameAsc(userId);
          } else {
              boolean privateValue = Boolean.parseBoolean(isPrivate);

              repos = repository.findByUserIdAndIsPrivateOrderByFullNameAsc(
                      userId,
                      privateValue
              );
          }

          return repos.stream()
                  .map(this::toResponse)
                  .toList();
      }

      @Transactional(readOnly = true)
      public Repository requireOwned(UUID repoId, UUID userId) {
          return repository.findByIdAndUserId(repoId, userId)
                  .orElseThrow(()-> new NotFoundException("Repository not found"));
      }

    @Transactional(readOnly = true)
    public IndexStatusResponse status(UUID repoId, UUID userId) {
        Repository repo = requireOwned(repoId, userId);
        return new IndexStatusResponse(
                repo.getId(),
                repo.getIndexStatus(),
                repo.getFilesTotal(),
                repo.getFilesProcessed(),
                repo.getChunkCount(),
                repo.getIndexedAt(),
                repo.getErrorMessage());
    }


    @Transactional
      public List<RepositoryResponse> syncAndListRepo(UUID userId, String is_private) {
          User user = userService.requiredById(userId);
          String token = userService.decryptAccessToken(user);

//        System.out.println("Token "+ token);
          List<Map<String,Object>> remoteRepos = apiClient.listUserRepos(token);

          List<Repository> saved = new ArrayList<>();

          for (Map<String, Object> remote : remoteRepos) {
              Long githubRepoId = toLong(remote.get("id"));
              Repository repo = repository
                      .findByUserIdAndGithubRepoId(userId, githubRepoId)
                      .orElseGet(Repository::new);

              String fullName = String.valueOf(remote.get("full_name"));
              String[] parts = fullName.split("/", 2);

              repo.setUserId(userId);
              repo.setGithubRepoId(githubRepoId);
              repo.setOwner(parts.length > 0 ? parts[0] : String.valueOf(remote.get("owner")));
              repo.setName(parts.length > 1 ? parts[1] : String.valueOf(remote.get("name")));
              repo.setFullName(fullName);
              repo.setPrivate(Boolean.TRUE.equals(remote.get("private")));
              repo.setDefaultBranch(remote.get("default_branch") != null
                      ? String.valueOf(remote.get("default_branch"))
                      : "main");
              repo.setLanguage(remote.get("language") != null ? String.valueOf(remote.get("language")) : null);
              repo.setHtmlUrl(remote.get("html_url") != null ? String.valueOf(remote.get("html_url")) : null);
              repo.setDescription(remote.get("description") != null ? String.valueOf(remote.get("description")) : null);
              repo.setUpdatedAt(Instant.now());
              if (repo.getOwner() == null || repo.getOwner().isBlank()) {
                  Object ownerObj = remote.get("owner");
                  if (ownerObj instanceof Map<?, ?> ownerMap && ownerMap.get("login") != null) {
                      repo.setOwner(String.valueOf(ownerMap.get("login")));
                  }
              }
              saved.add(repository.save(repo));
          }

        return saved.stream()
                .filter(repo -> {
                    if ("all".equalsIgnoreCase(is_private)) {
                        return true;
                    }

                    return Boolean.parseBoolean(is_private)
                            == Boolean.TRUE.equals(repo.getIsPrivate());
                })
                .sorted((a, b) -> a.getFullName()
                        .compareToIgnoreCase(b.getFullName()))
                .map(this::toResponse)
                .toList();
      }

    public RepositoryResponse toResponse(Repository repo) {
        return new RepositoryResponse(
                repo.getId(),
                repo.getGithubRepoId(),
                repo.getOwner(),
                repo.getName(),
                repo.getFullName(),
                repo.getIsPrivate(),
                repo.getDefaultBranch(),
                repo.getLanguage(),
                repo.getHtmlUrl(),
                repo.getDescription(),
                repo.getIndexStatus(),
                repo.getIndexedAt(),
                repo.getChunkCount(),
                repo.getFilesTotal(),
                repo.getFilesProcessed(),
                repo.getErrorMessage());
    }


    public static Long toLong(Object value) {
         if(value instanceof Number) {
             Number number = (Number) value;
             return number.longValue();
         }

         return Long.parseLong(String.valueOf(value));
      }


}
