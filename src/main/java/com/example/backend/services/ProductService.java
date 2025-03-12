package com.example.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.DTO.ProductColorDTO;
import com.example.backend.DTO.ProductDTO;
import com.example.backend.DTO.ProductVersionDTO;
import com.example.backend.models.Category;
import com.example.backend.models.Product;
import com.example.backend.models.ProductColor;
import com.example.backend.models.ProductVersion;
import com.example.backend.repositories.CategoryRepository;
import com.example.backend.repositories.ProductColorRepository;
import com.example.backend.repositories.ProductRepository;
import com.example.backend.repositories.ProductVersionRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVersionRepository productVersionRepository;
    private final ProductColorRepository productColorRepository;
    private final CategoryRepository categoryRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, 
                          ProductVersionRepository productVersionRepository, 
                          ProductColorRepository productColorRepository,
                          CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.productVersionRepository = productVersionRepository;
        this.productColorRepository = productColorRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductDTO> getAllProducts() {
        return productRepository.findAll().stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));
        return convertToDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setImageUrl(productDTO.getImageUrl());

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        // Lưu product trước để lấy ID
        final Product savedProduct = productRepository.save(product);

        Set<ProductVersion> versions = productDTO.getVersions().stream().map(versionDTO -> {
            ProductVersion version = new ProductVersion();
            version.setVersionName(versionDTO.getVersionName());
            version.setExtraPrice(versionDTO.getExtraPrice());
            version.setProduct(savedProduct);

            // Lưu version vào database trước và gán vào biến final
            final ProductVersion savedVersion = productVersionRepository.save(version);

            Set<ProductColor> colors = versionDTO.getColors().stream().map(colorDTO -> {
                ProductColor color = new ProductColor();
                color.setColorName(colorDTO.getColorName());
                color.setColorCode(colorDTO.getColorCode());
                color.setProductVersion(savedVersion); // Sử dụng biến final
                return productColorRepository.save(color);
            }).collect(Collectors.toSet());

            savedVersion.setColors(colors);
            return savedVersion;
        }).collect(Collectors.toSet());

        // Cập nhật lại danh sách phiên bản vào product đã lưu
        savedProduct.setVersions(versions);

        return convertToDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        Product existingProduct = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Cập nhật các trường thông tin của sản phẩm
        existingProduct.setName(productDTO.getName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setStockQuantity(productDTO.getStockQuantity());
        existingProduct.setImageUrl(productDTO.getImageUrl());

        // Cập nhật category
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        existingProduct.setCategory(category);

        // Lưu sản phẩm đã cập nhật vào cơ sở dữ liệu
        Product savedProduct = productRepository.save(existingProduct);

        return convertToDTO(savedProduct);  // Trả về DTO của sản phẩm đã được cập nhật
    }


    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setCategoryId(product.getCategory().getId());

        Set<ProductVersionDTO> versionDTOs = product.getVersions().stream().map(version -> {
            ProductVersionDTO versionDTO = new ProductVersionDTO();
            versionDTO.setId(version.getId());
            versionDTO.setVersionName(version.getVersionName());
            versionDTO.setExtraPrice(version.getExtraPrice());

            Set<ProductColorDTO> colorDTOs = version.getColors().stream().map(color -> {
                ProductColorDTO colorDTO = new ProductColorDTO();
                colorDTO.setId(color.getId());
                colorDTO.setColorName(color.getColorName());
                colorDTO.setColorCode(color.getColorCode());
                return colorDTO;
            }).collect(Collectors.toSet());

            versionDTO.setColors(colorDTOs);
            return versionDTO;
        }).collect(Collectors.toSet());

        dto.setVersions(versionDTOs);
        return dto;
    }
    
    
    
    
    @Transactional
    public ProductVersionDTO updateProductVersion(Long versionId, ProductVersionDTO versionDTO) {
        // Tìm kiếm phiên bản sản phẩm theo ID
        ProductVersion existingVersion = productVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Product Version not found"));

        // Cập nhật thông tin phiên bản sản phẩm
        existingVersion.setVersionName(versionDTO.getVersionName());
        existingVersion.setExtraPrice(versionDTO.getExtraPrice());

        // Lưu phiên bản đã cập nhật vào cơ sở dữ liệu
        ProductVersion savedVersion = productVersionRepository.save(existingVersion);

        return convertToVersionDTO(savedVersion);  // Trả về DTO của phiên bản đã được cập nhật
    }

    private ProductVersionDTO convertToVersionDTO(ProductVersion version) {
        ProductVersionDTO dto = new ProductVersionDTO();
        dto.setId(version.getId());
        dto.setVersionName(version.getVersionName());
        dto.setExtraPrice(version.getExtraPrice());

        Set<ProductColorDTO> colorDTOs = version.getColors().stream().map(color -> {
            ProductColorDTO colorDTO = new ProductColorDTO();
            colorDTO.setId(color.getId());
            colorDTO.setColorName(color.getColorName());
            colorDTO.setColorCode(color.getColorCode());
            return colorDTO;
        }).collect(Collectors.toSet());

        dto.setColors(colorDTOs);
        return dto;
    }

    
    @Transactional
    public ProductColorDTO updateProductColor(Long colorId, ProductColorDTO colorDTO) {
        // Tìm kiếm màu sắc sản phẩm theo ID
        ProductColor existingColor = productColorRepository.findById(colorId)
                .orElseThrow(() -> new RuntimeException("Product Color not found"));

        // Cập nhật thông tin màu sắc
        existingColor.setColorName(colorDTO.getColorName());
        existingColor.setColorCode(colorDTO.getColorCode());

        // Lưu màu sắc đã cập nhật vào cơ sở dữ liệu
        ProductColor savedColor = productColorRepository.save(existingColor);

        return convertToColorDTO(savedColor);  // Trả về DTO của màu sắc đã được cập nhật
    }

    private ProductColorDTO convertToColorDTO(ProductColor color) {
        ProductColorDTO dto = new ProductColorDTO();
        dto.setId(color.getId());
        dto.setColorName(color.getColorName());
        dto.setColorCode(color.getColorCode());
        return dto;
    }
}

