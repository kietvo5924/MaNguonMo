package com.example.backend.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils; // Import thêm để kiểm tra String

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

import jakarta.persistence.EntityNotFoundException; // Sử dụng exception cụ thể hơn

import java.util.Collections; // Sử dụng cho Set rỗng an toàn
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;
import java.nio.file.InvalidPathException;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);
    private static final String UPLOAD_DIR = "uploads/";

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
        return productRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProductDTO getProductById(Long id) {
        Product product = findProductByIdOrThrow(id);
        return convertToDTO(product);
    }

    @Transactional
    public ProductDTO createProduct(ProductDTO productDTO) {
        validateProductDTO(productDTO);

        Category category = findCategoryByIdOrThrow(productDTO.getCategoryId());

        Product product = new Product();
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setImageUrl(productDTO.getImageUrl()); // ImageUrl được set từ DTO khi tạo mới
        product.setCategory(category);
        product.setPopular(productDTO.isPopular());
        // Lưu product trước để có ID
        Product savedProduct = productRepository.save(product);

        // Xử lý versions và colors nếu có trong DTO
        if (productDTO.getVersions() != null && !productDTO.getVersions().isEmpty()) {
            Set<ProductVersion> versions = processVersions(productDTO.getVersions(), savedProduct);
            savedProduct.setVersions(versions);
            // Không cần save lại product vì version đã được lưu và liên kết
        }

        logger.info("Created product with ID: {}", savedProduct.getId());
        return convertToDTO(savedProduct);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, ProductDTO productDTO) {
        // 1. Tìm sản phẩm hiện có
        Product existingProduct = findProductByIdOrThrow(id);
        logger.info("Found product with ID: {} for update", id);

        // 2. Validate DTO đầu vào
        validateProductDTO(productDTO);

        // 3. Tìm Category mới
        Category category = findCategoryByIdOrThrow(productDTO.getCategoryId());

        // 4. Cập nhật thông tin cơ bản của Product
        existingProduct.setName(productDTO.getName());
        existingProduct.setDescription(productDTO.getDescription());
        existingProduct.setPrice(productDTO.getPrice());
        existingProduct.setStockQuantity(productDTO.getStockQuantity());
        existingProduct.setCategory(category);
        // Cập nhật imageUrl từ DTO (theo logic gốc bạn cung cấp)
        existingProduct.setImageUrl(productDTO.getImageUrl());
        existingProduct.setPopular(productDTO.isPopular());
        // 5. Xóa versions và colors cũ (theo logic gốc: xóa hết tạo lại)
        deleteExistingVersionsAndColors(existingProduct);

        // 6. Thêm versions và colors mới từ DTO
        if (productDTO.getVersions() != null && !productDTO.getVersions().isEmpty()) {
             Set<ProductVersion> newVersions = processVersions(productDTO.getVersions(), existingProduct);
             existingProduct.getVersions().addAll(newVersions); // Thêm vào collection
        } else {
            // Đảm bảo collection rỗng nếu DTO không có version
             existingProduct.setVersions(Collections.emptySet());
        }

        // 7. Lưu lại Product đã cập nhật
        Product updatedProduct = productRepository.save(existingProduct);
        logger.info("Successfully updated product with ID: {}", updatedProduct.getId());

        return convertToDTO(updatedProduct);
    }

    @Transactional
    public void deleteProduct(Long id) {
        logger.info("Attempting to delete product with ID: {}", id);
        // 1. Tìm sản phẩm để lấy thông tin ảnh (ném lỗi nếu không tìm thấy)
        Product product = findProductByIdOrThrow(id);

        // 2. Xóa file ảnh liên quan trước khi xóa entity
        deleteImageFile(product.getImageUrl());

        // 3. Xóa sản phẩm khỏi DB (logic gốc của bạn)
        // Lưu ý: Nếu không cấu hình CascadeType.REMOVE hoặc orphanRemoval=true,
        // việc này có thể gây lỗi nếu Product vẫn còn Version/Color liên kết.
        // Logic xóa con thủ công nằm trong hàm updateProduct của bạn.
        productRepository.deleteById(id);
        logger.info("Product with ID: {} requested for deletion from database.", id);
    }

    @Transactional
    public void updateProductImage(Long productId, String newImageUrl) {
        Product product = findProductByIdOrThrow(productId);
        String oldImageUrl = product.getImageUrl();

        // Chỉ xóa file cũ nếu nó tồn tại và khác file mới
        if (StringUtils.hasText(oldImageUrl) && !oldImageUrl.equals(newImageUrl)) {
            deleteImageFile(oldImageUrl);
        }

        product.setImageUrl(newImageUrl);
        productRepository.save(product);
        logger.info("Updated image URL for product ID {} to {}", productId, newImageUrl);
    }

    // --- Các phương thức private helper ---

    private Product findProductByIdOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Product not found with id: " + id));
    }

    private Category findCategoryByIdOrThrow(Long id) {
         if (id == null) {
             throw new IllegalArgumentException("Category ID cannot be null");
         }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + id));
    }

    private void deleteExistingVersionsAndColors(Product product) {
        logger.info("Deleting existing versions and colors for product ID: {}", product.getId());
        Set<ProductVersion> existingVersions = new HashSet<>(product.getVersions()); // Copy để tránh lỗi khi xóa
        if (!existingVersions.isEmpty()) {
            for (ProductVersion version : existingVersions) {
                logger.debug("Deleting colors for version ID: {}", version.getId());
                productColorRepository.deleteAll(version.getColors()); // Xóa colors trước
                productVersionRepository.delete(version); // Sau đó xóa version
            }
            product.getVersions().clear(); // Xóa khỏi collection của product
            productRepository.flush(); // Đồng bộ DB sau khi xóa
            logger.info("Finished deleting existing versions and colors for product ID: {}", product.getId());
        } else {
             logger.info("No existing versions to delete for product ID: {}", product.getId());
        }
    }

     // Helper để xử lý tạo versions và colors từ DTO
     private Set<ProductVersion> processVersions(Set<ProductVersionDTO> versionDTOs, Product product) {
         return versionDTOs.stream().map(versionDTO -> {
             // Có thể thêm validation versionDTO ở đây nếu cần
             if (versionDTO.getVersionName() == null || versionDTO.getVersionName().trim().isEmpty()) {
                 throw new IllegalArgumentException("Version name cannot be null or empty");
             }
              if (versionDTO.getExtraPrice() == null) {
                 throw new IllegalArgumentException("Extra price cannot be null for version: " + versionDTO.getVersionName());
             }

             ProductVersion version = new ProductVersion();
             version.setVersionName(versionDTO.getVersionName());
             version.setExtraPrice(versionDTO.getExtraPrice());
             version.setProduct(product);

             ProductVersion savedVersion = productVersionRepository.save(version); // Lưu version

             // Xử lý colors cho version này
             if (versionDTO.getColors() != null && !versionDTO.getColors().isEmpty()) {
                 Set<ProductColor> colors = processColors(versionDTO.getColors(), savedVersion);
                 savedVersion.setColors(colors);
             } else {
                 savedVersion.setColors(Collections.emptySet());
             }
             return savedVersion; // Trả về version đã lưu và có colors
         }).collect(Collectors.toSet());
     }

     // Helper để xử lý tạo colors từ DTO
     private Set<ProductColor> processColors(Set<ProductColorDTO> colorDTOs, ProductVersion version) {
         return colorDTOs.stream().map(colorDTO -> {
             // Có thể thêm validation colorDTO ở đây nếu cần
              if (colorDTO.getColorName() == null || colorDTO.getColorName().trim().isEmpty()) {
                 throw new IllegalArgumentException("Color name cannot be null or empty");
             }
             if (colorDTO.getColorCode() == null || colorDTO.getColorCode().trim().isEmpty()) {
                 throw new IllegalArgumentException("Color code cannot be null or empty for color: " + colorDTO.getColorName());
             }

             ProductColor color = new ProductColor();
             color.setColorName(colorDTO.getColorName());
             color.setColorCode(colorDTO.getColorCode());
             color.setProductVersion(version);
             return productColorRepository.save(color); // Lưu color
         }).collect(Collectors.toSet());
     }


    private void validateProductDTO(ProductDTO productDTO) {
        // Sử dụng IllegalArgumentException cho lỗi validation
        if (!StringUtils.hasText(productDTO.getName())) { // Kiểm tra cả null, rỗng và chỉ chứa khoảng trắng
            throw new IllegalArgumentException("Product name cannot be null or empty");
        }
        if (productDTO.getPrice() == null || productDTO.getPrice() < 0) {
            throw new IllegalArgumentException("Price cannot be null or negative");
        }
        if (productDTO.getStockQuantity() == null || productDTO.getStockQuantity() < 0) {
            throw new IllegalArgumentException("Stock quantity cannot be null or negative");
        }
        if (productDTO.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID cannot be null");
        }
        // Không validate version/color ở đây vì chúng có thể được xử lý riêng
    }

    private void deleteImageFile(String fileName) {
        if (!StringUtils.hasText(fileName)) { // Dùng StringUtils cho gọn
            return; // Không có gì để xóa
        }
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName).normalize();
            Path uploadDirPath = Paths.get(UPLOAD_DIR).normalize();
            // Kiểm tra bảo mật: file phải nằm trong thư mục upload
            if (!filePath.startsWith(uploadDirPath)) {
                logger.warn("Attempted to delete file outside designated upload directory: {}", fileName);
                return;
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                logger.info("Successfully deleted image file: {}", filePath);
            }
        } catch (IOException e) {
            logger.error("Could not delete image file: {}. Error: {}", fileName, e.getMessage());
        } catch (InvalidPathException e) {
            logger.error("Invalid path generated for filename: {}. Error: {}", fileName, e.getMessage());
        } catch (Exception e) { // Bắt các lỗi không mong muốn khác
            logger.error("An unexpected error occurred while deleting file: {}. Error: {}", fileName, e.getMessage());
        }
    }

    // --- DTO Conversion Methods ---
    // (Giữ nguyên các hàm convert và các hàm update version/color riêng lẻ)

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setStockQuantity(product.getStockQuantity());
        dto.setImageUrl(product.getImageUrl());
        dto.setPopular(product.isPopular());
        // Kiểm tra null trước khi truy cập ID của category
        dto.setCategoryId(product.getCategory() != null ? product.getCategory().getId() : null);

        // Chuyển đổi versions, trả về set rỗng nếu không có
        dto.setVersions(product.getVersions() != null ?
                product.getVersions().stream().map(this::convertToVersionDTO).collect(Collectors.toSet()) :
                Collections.emptySet());

        return dto;
    }

    private ProductVersionDTO convertToVersionDTO(ProductVersion version) {
        ProductVersionDTO dto = new ProductVersionDTO();
        dto.setId(version.getId());
        dto.setVersionName(version.getVersionName());
        dto.setExtraPrice(version.getExtraPrice());
        // Chuyển đổi colors, trả về set rỗng nếu không có
        dto.setColors(version.getColors() != null ?
                version.getColors().stream().map(this::convertToColorDTO).collect(Collectors.toSet()) :
                Collections.emptySet());
        return dto;
    }

    private ProductColorDTO convertToColorDTO(ProductColor color) {
        ProductColorDTO dto = new ProductColorDTO();
        dto.setId(color.getId());
        dto.setColorName(color.getColorName());
        dto.setColorCode(color.getColorCode());
        return dto;
    }

    // --- Các hàm cập nhật version/color riêng lẻ (Giữ nguyên) ---
    @Transactional
    public ProductVersionDTO updateProductVersion(Long versionId, ProductVersionDTO versionDTO) {
        ProductVersion existingVersion = productVersionRepository.findById(versionId)
                .orElseThrow(() -> new EntityNotFoundException("Product Version not found with id: " + versionId));

        // Chỉ cập nhật các trường cơ bản của version theo yêu cầu gốc
        existingVersion.setVersionName(versionDTO.getVersionName());
        existingVersion.setExtraPrice(versionDTO.getExtraPrice());

        ProductVersion savedVersion = productVersionRepository.save(existingVersion);
        return convertToVersionDTO(savedVersion);
    }

    @Transactional
    public ProductColorDTO updateProductColor(Long colorId, ProductColorDTO colorDTO) {
        ProductColor existingColor = productColorRepository.findById(colorId)
                .orElseThrow(() -> new EntityNotFoundException("Product Color not found with id: " + colorId));

        existingColor.setColorName(colorDTO.getColorName());
        existingColor.setColorCode(colorDTO.getColorCode());

        ProductColor savedColor = productColorRepository.save(existingColor);
        return convertToColorDTO(savedColor);
    }
}
