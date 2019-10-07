package hello.storage;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import java.nio.file.Path;
import java.nio.file.Paths;

@Scope(value = WebApplicationContext.SCOPE_SESSION, proxyMode = ScopedProxyMode.TARGET_CLASS)
@Component
public class UserSpace{

    private Path rootLocation = Paths.get("Z:\\upload");

    public Path getRootLocation() {
        return rootLocation;
    }

    public void setRootLocation(Path rootLocation) {
        this.rootLocation = rootLocation;
    }
}