package hello;

import hello.storage.StorageFileNotFoundException;
import hello.storage.StorageProperties;
import hello.storage.StorageService;
import hello.storage.UserSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

@Controller
public class FileUploadController {
    @Autowired
    private UserSpace userSpace;


    private StorageService storageService;
    private StorageProperties storageProperties;


    @Autowired
    public FileUploadController(StorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
        this.storageProperties = storageProperties;
    }


    @GetMapping("/")
    public String listUploadedFiles(Model model) {

        model.addAttribute("files", fileList(model));
        model.addAttribute("back", addRev(model));
        return "test";
    }
    private List fileList(Model model) {

        List k = storageService.loadAll(userSpace.getRootLocation()).map(path -> {
            if (!checkIfDirectory(path.toString())) {
                Object[] notDir = {path.toString(), model.toString()};
                return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", notDir).build().toString();
            } else {
                Object[] dir = {path.toString(), model.toString()};

                return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "listDirs", dir).build().toString();
            }


        }).collect(Collectors.toList());
        k.add(MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                "rev", model).build().toString());
        return k;
    }

    private boolean checkIfDirectory(String filename) {
        Resource file = storageService.loadAsResource(filename,userSpace.getRootLocation());
        try {
            return file.getFile().isDirectory();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private String addRev(Model model) {
        return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                "rev", model).build().toString();
    }

    @GetMapping("/...")
    public String rev(Model model) {
        String revS = userSpace.getRootLocation().toString();
        if (storageProperties.getLocation().equals(revS)) {
            return listUploadedFiles(model);
        } else {
            String[] splited = revS.split("\\\\");
            List<String> kola = new ArrayList(Arrays.asList(splited));
            kola.remove(kola.size() - 1);
            StringJoiner sj = new StringJoiner("\\");
            for (String s : kola) {
                sj.add(s);
            }

            userSpace.setRootLocation(Paths.get(sj.toString()));
            return listUploadedFiles(model);
        }
    }


    @GetMapping("/dirs/{dir:.+}")
    public String listDirs(@PathVariable String dir, Model model) {
        userSpace.setRootLocation(Paths.get(userSpace.getRootLocation().toString() + "\\" + dir));
        //storageService.setRootLocation(Paths.get(storageService.getRootLocation().toString() + "\\" + dir));
        return listUploadedFiles(model);
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename, Model model) {

        Resource file = storageService.loadAsResource(filename,userSpace.getRootLocation());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);

    }

    @PostMapping("/folder")
    public String handleDirCreate(@RequestParam("FolderName") String folderName,
                                  RedirectAttributes redirectAttributes) {

        storageService.createDirectory(folderName,userSpace.getRootLocation());

        redirectAttributes.addFlashAttribute("message",
                "Stworzono folder" + folderName);

        return "redirect:/";
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        storageService.store(file,userSpace.getRootLocation());
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
