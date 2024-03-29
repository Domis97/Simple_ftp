package goldenore.storage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

@Service
public class FileSystemStorageService implements StorageService {

    public void setRootLocation(Path rootLocation) {
        this.rootLocation = rootLocation;
    }

    public Path getRootLocation() {
        return this.rootLocation;
    }

    private Path rootLocation;
    private StorageProperties storageProperties;

    private void setStorageProperties(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    @Autowired
    public FileSystemStorageService(StorageProperties properties) {
        setStorageProperties(properties);
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file, Path rootPath) {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        try {
            if (file.isEmpty()) {
                throw new StorageException("Failed to store empty file " + filename);
            }
            if (filename.contains("..")) {
                // This is a security check
                throw new StorageException(
                        "Cannot store file with relative path outside current directory "
                                + filename);
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, rootPath.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll(Path rootPath) {
        try {
            return Files.walk(rootPath, 1)
                .filter(path -> !path.equals(rootPath))
                .map(rootPath::relativize);
        }
        catch (IOException e) {
            setRootLocation(Paths.get(storageProperties.getLocation()));
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename,Path rootPath) {
        return rootPath.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename,Path rootPath) {
        try {

            Path file = load(filename,rootPath);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            else {
                throw new StorageFileNotFoundException(
                        "Could not read file: " + filename);

            }
        }
        catch (MalformedURLException e) {
            throw new StorageFileNotFoundException("Could not read file: " + filename, e);
        }
    }

    public void createDirectory(String dirName,Path rootPath){
        try {
            Files.createDirectories(Paths.get((rootPath.toString()+"\\"+dirName)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        try {
            if(!Files.exists(this.rootLocation, NOFOLLOW_LINKS))
            Files.createDirectories(this.rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
