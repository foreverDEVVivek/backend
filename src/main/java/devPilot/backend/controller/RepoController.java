package devPilot.backend.controller;

import devPilot.backend.dto.IndexStatusResponse;
import devPilot.backend.dto.RepositoryResponse;
import devPilot.backend.entity.Repository;
import devPilot.backend.security.CurrentUser;
import devPilot.backend.services.RepoService;
import devPilot.backend.services.indexing.IndexingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;


@RestController
@RequestMapping("/api/repos")
@RequiredArgsConstructor
public class RepoController {
    private final RepoService service;
    private final CurrentUser currentUser;
    private final IndexingService indexingService;

    @GetMapping("")
    public List<RepositoryResponse> list(
            @RequestParam(name = "refresh", defaultValue = "true")
            boolean refresh,

            @RequestParam(name = "isPrivate", defaultValue = "all")
            String isPrivate
    ) {

        UUID userId = currentUser.require().getId();

        if (!isPrivate.equalsIgnoreCase("all")
                && !isPrivate.equalsIgnoreCase("true")
                && !isPrivate.equalsIgnoreCase("false")) {
            throw new IllegalArgumentException(
                    "isPrivate must be all, true, or false"
            );
        }

        if (refresh) {
            return service.syncAndListRepo(userId, isPrivate);
        }

        return service.listSorted(userId, isPrivate);
    }

    @GetMapping("/{id}")
    public RepositoryResponse get(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();

        return service.toResponse(service.requireOwned(id, userId));
    }

    @GetMapping("/{id}/status")
    public IndexStatusResponse status(@PathVariable UUID id) {
        UUID userId = currentUser.require().getId();
        return service.status(id, userId);
    }


    @PostMapping("/{id}/index")
    public ResponseEntity<RepositoryResponse> index(@PathVariable("id") UUID id) {
        UUID userId = currentUser.require().getId();

        Repository repo = indexingService.startIndexing(id, userId);

        indexingService.indexAsync(id, userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(service.toResponse(repo));
    }
}
