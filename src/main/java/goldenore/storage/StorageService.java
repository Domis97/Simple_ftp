package goldenore.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.stream.Stream;

public interface StorageService {

    void setRootLocation(Path rootLocation);

    void createDirectory(String dirName,Path rootPath);

    Path getRootLocation();

    void init();

    void store(MultipartFile file, Path rootPath);

    Stream<Path> loadAll(Path rootPath);

    Path load(String filename,Path rootPath);

    Resource loadAsResource(String filename,Path rootPath);



}
