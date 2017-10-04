package com.nv.service.storage;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


@Service
public class FileSystemStorageService implements StorageService {

    private final Path rootLocation;

    
    @Autowired
    StorageProperties properties;
    
    public FileSystemStorageService(StorageProperties properties) {
        this.rootLocation = Paths.get(properties.getLocation());
    }

    @Override
    public void store(MultipartFile file) {
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
            
            System.out.println("LOG ENTRY****"+file.getContentType()+"***** "+filename);
            Files.copy(file.getInputStream(), this.rootLocation.resolve(filename),
                    StandardCopyOption.REPLACE_EXISTING);
          saveFileData(this.rootLocation.resolve(file.getOriginalFilename()),filename);
           
        }
        catch (IOException e) {
            throw new StorageException("Failed to store file " + filename, e);
        }
    }

    @Override
    public Stream<Path> loadAll() {
        try {
            return Files.walk(this.rootLocation, 1)
                    .filter(path -> !path.equals(this.rootLocation))
                    .map(path -> this.rootLocation.relativize(path));
        }
        catch (IOException e) {
            throw new StorageException("Failed to read stored files", e);
        }

    }

    @Override
    public Path load(String filename) {
        return rootLocation.resolve(filename);
    }

    @Override
    public Resource loadAsResource(String filename) {
        try {
            Path file = load(filename);
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
   
    public void saveFileData(Path filepath,String filename) throws IOException{
    	BasicFileAttributes attr = Files.readAttributes(filepath, BasicFileAttributes.class);
    	String metaFileName = this.rootLocation+"\\properties-"+filename;
    	System.out.println("LOG ENTRY *** "+metaFileName);
    	PrintWriter writer = new PrintWriter(metaFileName, "UTF-8");
    	writer.println("creationTime: " + attr.creationTime());
    	writer.println("lastAccessTime: " + attr.lastAccessTime());
    	writer.println("lastModifiedTime: " + attr.lastModifiedTime());

    	writer.println("isDirectory: " + attr.isDirectory());
    	writer.println("isOther: " + attr.isOther());
    	writer.println("isRegularFile: " + attr.isRegularFile());
    	writer.println("isSymbolicLink: " + attr.isSymbolicLink());
    	writer.println("size: " + attr.size());
    	writer.println("Hashcode: "+attr.hashCode());
    	writer.close(); 
    }
    @Override
    public void deleteAll() {
        FileSystemUtils.deleteRecursively(rootLocation.toFile());
    }

    @Override
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            System.out.println("LOG entry "+rootLocation);
        }
        catch (IOException e) {
            throw new StorageException("Could not initialize storage", e);
        }
    }
}
