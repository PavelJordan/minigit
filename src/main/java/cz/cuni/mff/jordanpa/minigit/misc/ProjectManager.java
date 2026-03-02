package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProjectManager {
    private static final String REPOSITORIES_DIR_NAME = ".minigit";
    private static final String PROJECT_MANAGER_FILE_NAME = ".topminigit";
    private final List<Path> projects = new ArrayList<>();
    private final Path CWDAbs = Path.of(".").toAbsolutePath().normalize();
    private final Path PMDAbs;

    public ProjectManager(Path projectManagerDir) throws RuntimeException {
        PMDAbs = projectManagerDir.toAbsolutePath().normalize();
        if (Files.exists(PMDAbs.resolve(PROJECT_MANAGER_FILE_NAME))) {
            throw new RuntimeException("Project manager already exists here.");
        }
        if (Files.exists(PMDAbs.resolve(REPOSITORIES_DIR_NAME))) {
            throw new RuntimeException("Project manager must not contain a .minigit directory.");
        }
    }

    private ProjectManager(Path projectManagerDir, boolean existing) throws IOException {
        PMDAbs = projectManagerDir.toAbsolutePath().normalize();
        if (!isPresent(PMDAbs)) {
            throw new IOException("Project manager does not exist here.");
        }
        for (String line : Files.readAllLines(PMDAbs.resolve(PROJECT_MANAGER_FILE_NAME))) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            Path resolvedPath = resolvePath(PMDAbs.resolve(line));
            if(isInvalidProjectToAdd(resolvedPath)) {
                IO.println("You should probably remove " + resolvedPath + " from the project manager directory.");
                continue;
            }
            projects.add(relativizeToCWD(resolvedPath));
        }
    }

    public static ProjectManager load(Path projectManagerDir) throws IOException {
        return new ProjectManager(projectManagerDir, true);
    }

    public List<Path> getProjects() {
        return List.copyOf(projects);
    }

    public void addProject(Path projectDir) {
        projectDir = resolvePath(projectDir);
        if (isInvalidProjectToAdd(projectDir)) {
            return;
        }
        projects.add(relativizeToCWD(projectDir));
    }

    public void removeProject(Path projectDir) {
        projectDir = resolvePath(projectDir);
        if (!projects.contains(relativizeToCWD(projectDir))) {
            IO.println("Project not found: " + projectDir);
            return;
        }
        projects.remove(relativizeToCWD(projectDir));
    }

    private Path relativizeToProjectManager(Path path) {
        return PMDAbs.relativize(path.toAbsolutePath()).normalize();
    }

    private Path relativizeToCWD(Path path) {
        return CWDAbs.relativize(path.toAbsolutePath()).normalize();
    }

    public void save() throws IOException {
        Files.createDirectories(PMDAbs);
        Path pmdpath = PMDAbs.resolve(PROJECT_MANAGER_FILE_NAME);
        if (!Files.exists(pmdpath)) {
            Files.createFile(pmdpath);
        }
        try (var out = Files.newBufferedWriter(pmdpath)) {
            for (Path project : projects) {
                out.write(relativizeToProjectManager(project).toString());
                out.newLine();
            }
        }
    }

    private boolean isInvalidProjectToAdd(Path path) {
        if (!Files.isDirectory(path)) {
            IO.println("Project directory " + path + " must be a directory.");
            return true;
        }
        if (projects.contains(relativizeToCWD(path))) {
            IO.println("Project " + path + " already added.");
            return true;
        }
        if (!Files.isDirectory(path.resolve(REPOSITORIES_DIR_NAME))) {
            IO.println("Project directory " + path + " does not contain a .minigit directory.");
            return true;
        }

        return false;
    }

    private Path resolvePath(Path path) {
        return path.toAbsolutePath().normalize();
    }

    public static boolean isPresent(Path path) {
        return Files.exists(path.resolve(PROJECT_MANAGER_FILE_NAME));
    }

    public static List<Repository> loadSingleRepoOrReposFromManager(Path path) throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();
        if (ProjectManager.isPresent(path)) {
            IO.println("Project manager found in " + path.resolve(PROJECT_MANAGER_FILE_NAME).normalize());
            ProjectManager pm = ProjectManager.load(path);
            for (Path repo : pm.getProjects()) {
                repos.add(Repository.load(repo.resolve(".minigit")));
            }
        }
        else {
            repos.add(Repository.load(Path.of(".minigit")));
        }
        return repos;
    }
}
