package shipeditor.parsing.loading;

import shipeditor.utility.text.StringManager;

import lombok.extern.log4j.Log4j2;
import shipeditor.persistence.SettingsManager;
import shipeditor.utility.Errors;
import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Log4j2
public final class FileSystemUtils {

    private static final Map<Path, SoftReference<Map<String, List<Path>>>> DIRECTORY_INDICES = new ConcurrentHashMap<>();

    private FileSystemUtils() {
    }

    public static void clearDirectoryCache() {
        DIRECTORY_INDICES.clear();
    }

    private static Map<String, List<Path>> getOrCreateIndex(Path folderPath) {
        if (folderPath == null)
            return java.util.Collections.emptyMap();
        SoftReference<Map<String, List<Path>>> ref = DIRECTORY_INDICES.get(folderPath);
        if (ref != null) {
            Map<String, List<Path>> existing = ref.get();
            if (existing != null) {
                return existing;
            }
        }
        Map<String, List<Path>> index = new ConcurrentHashMap<>();
        try (Stream<Path> stream = walk(folderPath)) {
            stream.filter(Files::isRegularFile).forEach(file -> {
                Path fileNamePath = file.getFileName();
                if (fileNamePath == null) return;
                String fileName = fileNamePath.toString().toLowerCase(java.util.Locale.ROOT);
                index.computeIfAbsent(fileName, k -> new ArrayList<>()).add(file);
            });
        } catch (IOException e) {
            log.error(StringManager.getString("FAILED_TO_INDEX_FOLDER"), folderPath, e);
        }
        DIRECTORY_INDICES.put(folderPath, new SoftReference<>(index));
        return index;
    }

    public static Stream<Path> walk(Path start) throws IOException {
        return Files.walk(start, FileVisitOption.FOLLOW_LINKS);
    }

    public static Path searchFileInFolder(Path filePath, Path folderPath) {
        if (filePath == null || folderPath == null) {
            return null;
        }

        Path directPath = folderPath.resolve(filePath);
        if (Files.exists(directPath)) {
            return directPath;
        }

        String originalPathString = filePath.toString();
        String normalizedPathString = originalPathString.replace('\\', '/');
        
        int lastSlashIndex = normalizedPathString.lastIndexOf('/');
        String fileName = (lastSlashIndex >= 0 ? normalizedPathString.substring(lastSlashIndex + 1) : normalizedPathString).toLowerCase(java.util.Locale.ROOT);

        Map<String, List<Path>> index = getOrCreateIndex(folderPath);
        List<Path> foundFiles = index.get(fileName);
        if (foundFiles != null) {
            String lowerNormalizedPath = normalizedPathString.toLowerCase(java.util.Locale.ROOT);
            for (Path foundFile : foundFiles) {
                String foundPathString = foundFile.toString().replace('\\', '/').toLowerCase(java.util.Locale.ROOT);
                if (foundPathString.endsWith(lowerNormalizedPath)) {
                    return foundFile;
                }
            }
        }
        return null;
    }

    public static File fetchDataFile(Path filePath, Path packageFolderPath) {
        if (filePath == null) {
            log.error(StringManager.getString("FETCH_DATA_FILE_NULL"));
            return null;
        }
        Path coreDataFolder = SettingsManager.getCoreFolderPath();
        List<Path> otherModFolders = SettingsManager.getAllModFolders();
        Path result = null;

        if (packageFolderPath != null) {
            result = searchFileInFolder(filePath, packageFolderPath);
        }
        if (result == null) {
            result = searchFileInFolder(filePath, coreDataFolder);
        }
        if (result != null) return result.toFile();

        for (Path modFolder : otherModFolders) {
            Path fileNamePath = modFolder.getFileName();
            if (fileNamePath == null || !SettingsManager.isModActive(fileNamePath.toString())) {
                continue;
            }
            result = searchFileInFolder(filePath, modFolder);
            if (result != null) {
                break;
            }
        }
        if (result != null) {
            return result.toFile();
        } else {
            Path filePathName = filePath.getFileName();
            log.error(StringManager.getString("FETCH_DATA_FILE_FAILED"), filePathName != null ? filePathName.toString() : filePath.toString());
        }
        return null;
    }

    public static List<File> fetchFilesWithExtension(Path target, String dotlessExtension) {
        List<File> files = new ArrayList<>();
        if (target == null || !Files.exists(target) || dotlessExtension == null) {
            return files;
        }
        try (Stream<Path> pathStream = walk(target)) {
            pathStream.filter(path -> {
                Path fileNamePath = path.getFileName();
                if (fileNamePath == null) return false;
                String toString = fileNamePath.toString();
                return toString.endsWith("." + dotlessExtension);
            })
                    .map(p -> p.toFile())
                    .forEach(files::add);
        } catch (IOException exception) {
            log.error("Failed to fetch files with extension: {}", dotlessExtension, exception);
        }
        return files;
    }
}
