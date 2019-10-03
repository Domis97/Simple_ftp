package hello;

import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;


import hello.storage.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import hello.storage.StorageFileNotFoundException;
import hello.storage.StorageService;

@Controller
public class FileUploadController {

    private StorageService storageService;

    private List fileList(Model model) {

        List k = storageService.loadAll().map(path -> {
            if(!checkIfDirectory(path.toString())) {
                Object[] notDir = {path.toString(), model.toString()};
                return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "serveFile", notDir).build().toString();
            }
            else {
                Object[] dir = {path.toString(), model.toString()};

                return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                        "listDirs", dir).build().toString();
            }


        }).collect(Collectors.toList());
        k.add(MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                "rev", model).build().toString());
        return k;
    }


    @Autowired
    public FileUploadController(StorageService storageService, StorageProperties storageProperties) {
        this.storageService = storageService;
    }

    @GetMapping("/")
    public String listUploadedFiles(Model model) {

        model.addAttribute("files", fileList(model));
        model.addAttribute("back",addRev(model));
//        model.addAttribute("files", storageService.loadAll().map(
//                path -> MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
//                        "serveFile", path.getFileName().toString()).build().toString())
//                .collect(Collectors.toList()));

        return "uploadForm";
    }

    private boolean checkIfDirectory(String filename) {
        Resource file = storageService.loadAsResource(filename);
        try {
            if (file.getFile().isDirectory()) {
                return true;
            } else
                return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public String addRev (Model model){
        return MvcUriComponentsBuilder.fromMethodName(FileUploadController.class,
                "rev", model).build().toString();
    }

    @GetMapping("/...")
    public String rev (Model model) {
        String [] splited = storageService.getRootLocation().toString().split("\\\\");
        List<String> kola = new ArrayList( Arrays.asList(splited));
        kola.remove(kola.size() -1);
        StringJoiner sj = new StringJoiner("\\");
        for (String s : kola){
            sj.add(s);
        }
        storageService.setRootLocation(Paths.get(sj.toString()));
        return listUploadedFiles(model);
    }


    @GetMapping("/dirs/{dir:.+}")
    public String listDirs (@PathVariable String dir, Model model) {
        storageService.setRootLocation(Paths.get(storageService.getRootLocation().toString()+"\\"+dir));
        return listUploadedFiles(model);
    }

    @GetMapping("/files/{filename:.+}")
    @ResponseBody
    public ResponseEntity<Resource> serveFile(@PathVariable String filename, Model model) throws IOException {

        Resource file = storageService.loadAsResource(filename);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"" + file.getFilename() + "\"").body(file);

        //        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"" + file.getFilename() + "\"").body(file);
    }

    @PostMapping("/")
    public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {

        storageService.store(file);
        redirectAttributes.addFlashAttribute("message",
                "You successfully uploaded " + file.getOriginalFilename() + "!");

        return "redirect:/";
    }

    @ExceptionHandler(StorageFileNotFoundException.class)
    public ResponseEntity<?> handleStorageFileNotFound(StorageFileNotFoundException exc) {
        return ResponseEntity.notFound().build();
    }

}
