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
}
