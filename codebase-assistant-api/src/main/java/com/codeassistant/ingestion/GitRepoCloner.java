package com.codeassistant.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class GitRepoCloner {

    public Path clone(String repoUrl, String branch) throws GitAPIException, IOException {
        Path tempDir = Files.createTempDirectory("codeassistant-repo-");
        log.info("Cloning {} branch={} into {}", repoUrl, branch, tempDir);

        Git.cloneRepository()
                .setURI(repoUrl)
                .setBranch(branch)
                .setDirectory(tempDir.toFile())
                .setDepth(1)
                .call()
                .close();

        log.info("Clone complete: {}", tempDir);
        return tempDir;
    }

    public void delete(Path dir) throws IOException {
        if (dir == null || !Files.exists(dir)) return;
        try (var stream = Files.walk(dir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(java.io.File::delete);
        }
        log.debug("Deleted temp dir: {}", dir);
    }
}
