package cz.cuni.mff.jordanpa.minigit.misc;

import cz.cuni.mff.jordanpa.minigit.structures.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class for handling multiple MiniGit repositories at once.
 *
 * <p>
 *     The project manager is represented by the <code>.topminigit</code> file, which stores
 *     paths to multiple MiniGit repositories. Commands can then be applied to all those repositories
 *     at once.
 * </p>
 * <p>
 *     If a project manager is not present, the code usually falls back to loading a single MiniGit repository.
 * </p>
 */
public final class ProjectManager {
    /**
     * Name of the MiniGit repository directory.
     */
    private static final String REPOSITORIES_DIR_NAME = ".minigit";

    /**
     * Name of the file representing the project manager.
     */
    private static final String PROJECT_MANAGER_FILE_NAME = ".topminigit";

    /**
     * List of repositories managed by this project manager, relative to CWD.
     */
    private final List<Path> projects = new ArrayList<>();

    /**
     * Absolute normalized path to the current working directory.
     */
    private final Path CWDAbs = Path.of(".").toAbsolutePath().normalize();

    /**
     * Absolute normalized path to the directory containing the project manager.
     */
    private final Path PMDAbs;

    /**
     * Create a new project manager in the specified directory.
     *
     * <p>
     *     The directory must not yet contain a project manager file, and it must not itself
     *     be a MiniGit repository.
     * </p>
     *
     * @param projectManagerDir The directory where the project manager should be created.
     * @throws RuntimeException If a project manager already exists there, or if the directory contains a .minigit folder.
     */
    public ProjectManager(Path projectManagerDir) throws RuntimeException {
        PMDAbs = projectManagerDir.toAbsolutePath().normalize();
        if (Files.exists(PMDAbs.resolve(PROJECT_MANAGER_FILE_NAME))) {
            throw new RuntimeException("Project manager already exists here.");
        }
        if (Files.exists(PMDAbs.resolve(REPOSITORIES_DIR_NAME))) {
            throw new RuntimeException("Project manager must not contain a .minigit directory.");
        }
    }

    /**
     * Load an existing project manager from the specified directory.
     *
     * @param projectManagerDir The directory containing the project manager.
     * @param existing Distinguishes this constructor from the public one. Should always be true here.
     * @throws IOException If the project manager does not exist, or some of its data is invalid.
     */
    private ProjectManager(Path projectManagerDir, boolean existing) throws IOException {
        PMDAbs = projectManagerDir.toAbsolutePath().normalize();
        if (!isPresent(PMDAbs)) {
            throw new IOException("Project manager does not exist here.");
        }

        // Load all project paths stored in the .topminigit file.
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

    /**
     * Load an existing project manager from the specified directory.
     *
     * @param projectManagerDir The directory containing the project manager.
     * @return The loaded project manager.
     * @throws IOException If the project manager does not exist, or some of its data is invalid.
     */
    public static ProjectManager load(Path projectManagerDir) throws IOException {
        return new ProjectManager(projectManagerDir, true);
    }

    /**
     * @return Copy the list of managed project paths and return it.
     */
    public List<Path> getProjects() {
        return List.copyOf(projects);
    }

    /**
     * Add a project to this project manager.
     *
     * <p>
     *     The project must be a directory, must not yet be added,
     *     and must contain a <code>.minigit</code> directory.
     * </p>
     *
     * @param projectDir The project directory to add.
     */
    public void addProject(Path projectDir) {
        projectDir = resolvePath(projectDir);
        if (isInvalidProjectToAdd(projectDir)) {
            return;
        }
        projects.add(relativizeToCWD(projectDir));
    }

    /**
     * Remove a project from this project manager.
     *
     * @param projectDir The project directory to remove.
     */
    public void removeProject(Path projectDir) {
        projectDir = resolvePath(projectDir);
        if (!projects.contains(relativizeToCWD(projectDir))) {
            IO.println("Project not found: " + projectDir);
            return;
        }
        projects.remove(relativizeToCWD(projectDir));
    }

    /**
     * Save this project manager into its file.
     *
     * @throws IOException If writing fails.
     */
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

    /**
     * Check whether the specified path contains a project manager.
     *
     * @param path The directory to check.
     * @return True if the project manager file exists there, false otherwise.
     */
    public static boolean isPresent(Path path) {
        return Files.exists(path.resolve(PROJECT_MANAGER_FILE_NAME));
    }

    /**
     * Load multiple repositories from the project manager, or a single repository from root/under repository.
     *
     * <p>
     *     First, if this is a directory with a project manager, load all repositories from the project manager.
     *     Otherwise, try to load a single repository if this path contains it
     *     (the .minigit folder, or if the .minigit folder is in the parent folders).
     * </p>
     * @param path Path to the folder with a project manager to load repositories from,
     *             or to search for repositories in (or parent folders)
     * @return List of repositories to apply commands to if something is invoked in this path.
     * @throws IOException If no repository is found, or some data is corrupted.
     */
    public static List<Repository> loadSingleRepoOrReposFromManager(Path path) throws IOException {
        ArrayList<Repository> repos = new ArrayList<>();

        // Project manager found
        if (ProjectManager.isPresent(path)) {

            IO.println("Project manager found in " + path.resolve(PROJECT_MANAGER_FILE_NAME).normalize());
            ProjectManager pm = ProjectManager.load(path);

            // Load all repositories from the project manager
            for (Path repo : pm.getProjects()) {
                repos.add(Repository.load(repo.resolve(".minigit")));
            }
        }

        // Project manager not found, try to load a single repository here or in parent folders
        else {
            repos.add(Repository.load(Path.of(".minigit")));
        }

        return repos;
    }

    /**
     * Convert the specified path to a path relative to the project manager directory.
     *
     * @param path The path to relativize.
     * @return The path relative to the project manager directory.
     */
    private Path relativizeToProjectManager(Path path) {
        return PMDAbs.relativize(path.toAbsolutePath()).normalize();
    }

    /**
     * Convert the specified path to a path relative to the current working directory.
     *
     * @param path The path to relativize.
     * @return The path relative to the current working directory.
     */
    private Path relativizeToCWD(Path path) {
        return CWDAbs.relativize(path.toAbsolutePath()).normalize();
    }

    /**
     * Check whether the specified project path is invalid to add.
     *
     * <p>
     *     The project must be a directory, must not yet be present in this project manager,
     *     and must contain a <code>.minigit</code> directory.
     * </p>
     *
     * @param path The project path to validate.
     * @return True if the project is invalid to add, false otherwise.
     */
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

    /**
     * Resolve the specified path to an absolute normalized path.
     *
     * @param path The path to resolve.
     * @return The absolute normalized path.
     */
    private Path resolvePath(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
