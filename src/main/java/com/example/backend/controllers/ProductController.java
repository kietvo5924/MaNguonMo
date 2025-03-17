package com.example.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.backend.DTO.ProductColorDTO;
import com.example.backend.DTO.ProductDTO;
import com.example.backend.DTO.ProductVersionDTO;
import com.example.backend.services.ProductService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/admin/products")
@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
public class ProductController {

    private final ProductService productService;

    @Autowired
    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDTO>> getAllProducts() {
        return ResponseEntity.ok(productService.getAllProducts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getProductById(@PathVariable Long id) {
        ProductDTO productDTO = productService.getProductById(id);
        if (productDTO == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(productDTO);
    }

    @PostMapping
    public ResponseEntity<ProductDTO> createProduct(@RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.createProduct(productDTO));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductDTO> updateProduct(@PathVariable Long id, @RequestBody ProductDTO productDTO) {
        return ResponseEntity.ok(productService.updateProduct(id, productDTO));
    }

    @PutMapping("/versions/{versionId}")
    public ResponseEntity<ProductVersionDTO> updateProductVersion(
            @PathVariable Long versionId,
            @RequestBody ProductVersionDTO versionDTO) {

        ProductVersionDTO updatedVersion = productService.updateProductVersion(versionId, versionDTO);
        return ResponseEntity.ok(updatedVersion);
    }
    
    @PutMapping("/colors/{colorId}")
    public ResponseEntity<ProductColorDTO> updateProductColor(@PathVariable Long colorId, @RequestBody ProductColorDTO colorDTO) {
        ProductColorDTO updatedColor = productService.updateProductColor(colorId, colorDTO);
        return ResponseEntity.ok(updatedColor);
    }

    
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok("Product deleted successfully");
    }
    @PostMapping("/{productId}/upload-image")
    public ResponseEntity<String> uploadImage(@PathVariable Long productId, @RequestParam("imageFile") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File không được để trống!");
        }

        try {
            // Tạo thư mục lưu ảnh nếu chưa có
            String uploadDir = "uploads/";
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Lưu file vào thư mục
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Cập nhật tên file ảnh vào database
            productService.updateProductImage(productId, fileName);

            return ResponseEntity.ok("Ảnh đã được tải lên: " + fileName);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi lưu ảnh: " + e.getMessage());
        }
    }
    @GetMapping("/uploads/{fileName}")
    public ResponseEntity<Resource> serveFile(@PathVariable String fileName) {
        try {
            Path filePath = Paths.get("uploads").resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }


}
