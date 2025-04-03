package com.example.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

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
        validateProductDTO(productDTO);

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setImageUrl(productDTO.getImageUrl());

        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        product.setCategory(category);

        final Product savedProduct = productRepository.save(product);

        Set<ProductVersion> versions = productDTO.getVersions().stream().map(versionDTO -> {
            ProductVersion version = new ProductVersion();
            version.setVersionName(versionDTO.getVersionName());
            version.setExtraPrice(versionDTO.getExtraPrice());
            version.setProduct(savedProduct);

            final ProductVersion savedVersion = productVersionRepository.save(version);

            Set<ProductColor> colors = versionDTO.getColors().stream().map(colorDTO -> {
                ProductColor color = new ProductColor();
                color.setColorName(colorDTO.getColorName());
                color.setColorCode(colorDTO.getColorCode());
                color.setProductVersion(savedVersion);
                return productColorRepository.save(color);
            }).collect(Collectors.toSet());

            savedVersion.setColors(colors);
            return savedVersion;
        }).collect(Collectors.toSet());

        savedProduct.setVersions(versions);

        return convertToDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        try {
            logger.info("Starting updateProduct for product ID: {}", id);
            logger.debug("ProductDTO received: {}", productDTO);

            // Kiểm tra sản phẩm có tồn tại không
            Product existingProduct = productRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Product not found with id: " + id));
            logger.info("Found product with ID: {}", id);

            // Kiểm tra dữ liệu đầu vào
            validateProductDTO(productDTO);

            // Cập nhật các trường thông tin của sản phẩm
            existingProduct.setName(productDTO.getName());
            existingProduct.setDescription(productDTO.getDescription());
            existingProduct.setPrice(productDTO.getPrice());
            existingProduct.setStockQuantity(productDTO.getStockQuantity());
            existingProduct.setImageUrl(productDTO.getImageUrl());

            // Kiểm tra và cập nhật category
            if (productDTO.getCategoryId() == null) {
                throw new RuntimeException("Category ID cannot be null");
            }
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Category not found with id: " + productDTO.getCategoryId()));
            existingProduct.setCategory(category);
            logger.info("Category updated for product ID: {}", id);

            // Xóa toàn bộ versions và colors cũ thông qua repository
            logger.info("Deleting existing versions and colors for product ID: {}", id);
            Set<ProductVersion> existingVersions = new HashSet<>(existingProduct.getVersions()); // Tạo bản sao để tránh ConcurrentModificationException
            for (ProductVersion version : existingVersions) {
                logger.debug("Deleting colors for version ID: {}", version.getId());
                productColorRepository.deleteAll(version.getColors());
                productVersionRepository.delete(version);
            }
            existingProduct.getVersions().clear(); // Xóa sau khi đã xóa trong database
            logger.info("Existing versions and colors deleted for product ID: {}", id);

            // Lưu sản phẩm để đảm bảo trạng thái được đồng bộ
            productRepository.save(existingProduct);

            // Kiểm tra versions từ DTO
            if (productDTO.getVersions() == null) {
                throw new RuntimeException("Versions cannot be null");
            }

            // Thêm các versions và colors mới
            logger.info("Adding new versions and colors for product ID: {}", id);
            Set<ProductVersion> newVersions = new HashSet<>();
            for (ProductVersionDTO versionDTO : productDTO.getVersions()) {
                if (versionDTO.getVersionName() == null || versionDTO.getVersionName().isEmpty()) {
                    throw new RuntimeException("Version name cannot be null or empty");
                }
                if (versionDTO.getExtraPrice() == null) {
                    throw new RuntimeException("Extra price cannot be null for version: " + versionDTO.getVersionName());
                }
                if (versionDTO.getColors() == null) {
                    throw new RuntimeException("Colors cannot be null for version: " + versionDTO.getVersionName());
                }

                ProductVersion version = new ProductVersion();
                version.setVersionName(versionDTO.getVersionName());
                version.setExtraPrice(versionDTO.getExtraPrice());
                version.setProduct(existingProduct);

                ProductVersion savedVersion = productVersionRepository.save(version);
                logger.debug("Saved version: {}", savedVersion.getId());

                Set<ProductColor> colors = new HashSet<>();
                for (ProductColorDTO colorDTO : versionDTO.getColors()) {
                    if (colorDTO.getColorName() == null || colorDTO.getColorName().isEmpty()) {
                        throw new RuntimeException("Color name cannot be null or empty");
                    }
                    if (colorDTO.getColorCode() == null || colorDTO.getColorCode().isEmpty()) {
                        throw new RuntimeException("Color code cannot be null or empty for color: " + colorDTO.getColorName());
                    }

                    ProductColor color = new ProductColor();
                    color.setColorName(colorDTO.getColorName());
                    color.setColorCode(colorDTO.getColorCode());
                    color.setProductVersion(savedVersion);
                    ProductColor savedColor = productColorRepository.save(color);
                    logger.debug("Saved color: {}", savedColor.getId());
                    colors.add(savedColor);
                }

                savedVersion.setColors(colors);
                newVersions.add(savedVersion);
            }

            // Gán versions mới và để Hibernate tự quản lý
            existingProduct.getVersions().addAll(newVersions);

            Product savedProduct = productRepository.save(existingProduct);
            logger.info("Product updated successfully with ID: {}", savedProduct.getId());

            return convertToDTO(savedProduct);
        } catch (RuntimeException e) {
            logger.error("Failed to update product: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update product: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while updating product: {}", e.getMessage(), e);
            throw new RuntimeException("Unexpected error while updating product: " + e.getMessage());
        }
    }

    private void validateProductDTO(ProductDTO productDTO) {
        if (productDTO.getName() == null || productDTO.getName().isEmpty()) {
            throw new RuntimeException("Product name cannot be null or empty");
        }
        if (productDTO.getPrice() == null || productDTO.getPrice() < 0) {
            throw new RuntimeException("Price cannot be null or negative");
        }
        if (productDTO.getStockQuantity() == null || productDTO.getStockQuantity() < 0) {
            throw new RuntimeException("Stock quantity cannot be null or negative");
        }
        if (productDTO.getCategoryId() == null) {
            throw new RuntimeException("Category ID cannot be null");
        }
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
        ProductVersion existingVersion = productVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("Product Version not found"));

        existingVersion.setVersionName(versionDTO.getVersionName());
        existingVersion.setExtraPrice(versionDTO.getExtraPrice());

        ProductVersion savedVersion = productVersionRepository.save(existingVersion);

        return convertToVersionDTO(savedVersion);
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
        ProductColor existingColor = productColorRepository.findById(colorId)
                .orElseThrow(() -> new RuntimeException("Product Color not found"));

        existingColor.setColorName(colorDTO.getColorName());
        existingColor.setColorCode(colorDTO.getColorCode());

        ProductColor savedColor = productColorRepository.save(existingColor);

        return convertToColorDTO(savedColor);
    }

    private ProductColorDTO convertToColorDTO(ProductColor color) {
        ProductColorDTO dto = new ProductColorDTO();
        dto.setId(color.getId());
        dto.setColorName(color.getColorName());
        dto.setColorCode(color.getColorCode());
        return dto;
    }

    public void updateProductImage(Long productId, String imageUrl) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Sản phẩm không tồn tại"));
        product.setImageUrl(imageUrl);
        productRepository.save(product);
    }
}