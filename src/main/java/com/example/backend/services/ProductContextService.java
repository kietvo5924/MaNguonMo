package com.example.backend.services;

import com.example.backend.models.Category;
import com.example.backend.models.Product;
import com.example.backend.models.ProductColor;
import com.example.backend.models.ProductVersion;
import com.example.backend.repositories.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ProductContextService {

    private static final Logger logger = LoggerFactory.getLogger(ProductContextService.class);

    private final ProductRepository productRepository;

    @Autowired
    public ProductContextService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public String getProductInfoForPrompt(String userInput) {
        if (!StringUtils.hasText(userInput)) {
            logger.warn("getProductInfoForPrompt called with empty input."); // Giữ WARN
            return "Không có thông tin sản phẩm nào được yêu cầu.";
        }

        logger.info("Attempting to find products for user input: '{}'", userInput); // Giữ INFO
        List<Product> products = productRepository.findByNameContainingIgnoreCase(userInput);

        // Fallback tìm bằng các từ trong câu (từ cuối lên)
        if (products.isEmpty()) {
            logger.info("No products found containing the full input '{}'. Attempting fallback search by words.", userInput); // Giữ INFO
            String cleanedInput = userInput.replaceAll("[.,?!]", "").toLowerCase();
            List<String> words = Arrays.asList(cleanedInput.split("\\s+"));

            for (int i = words.size() - 1; i >= 0; i--) {
                String word = words.get(i);
                if (word.length() <= 3) {
                    continue;
                }
                List<Product> foundByWord = productRepository.findByNameContainingIgnoreCase(word);
                if (!foundByWord.isEmpty()) {
                    logger.info("Fallback search successful! Found {} products containing the word '{}'", foundByWord.size(), word); // Giữ INFO
                    products = foundByWord;
                    break;
                }
            }
        }

        if (products.isEmpty()) {
            logger.info("No products found matching input '{}' after all search attempts.", userInput); // Giữ INFO
            return "Không tìm thấy thông tin sản phẩm nào khớp với yêu cầu '" + userInput + "'. Vui lòng thử lại với tên sản phẩm cụ thể hơn.";
        }

        logger.info("Found {} products matching the query. Processing top {} products for context.", products.size(), 3); // Giữ INFO

        int limit = 3;
        List<Product> limitedProducts = products.stream().limit(limit).toList();

        StringBuilder builder = new StringBuilder("Thông tin sản phẩm liên quan:\n");

        for (Product p : limitedProducts) {
             if (p == null) {
                 logger.warn("Skipping null product found in the list."); // Giữ WARN
                 continue;
             }

             builder.append("--------------------\n");
             builder.append("Tên sản phẩm: ").append(p.getName() != null ? p.getName() : "(Không có tên)").append("\n");

             try {
                 Category category = p.getCategory();
                 if (category != null && category.getName() != null) {
                     builder.append("  Danh mục: ").append(category.getName()).append("\n");
                 } else {
                     builder.append("  Danh mục: (Không xác định)\n");
                 }
             } catch (Exception e) {
                  logger.error("Error accessing Category for product ID {}: {}", p.getId(), e.getMessage(), e); // Giữ ERROR
                  builder.append("  Danh mục: (Lỗi khi tải)\n");
             }

             if (StringUtils.hasText(p.getDescription())) {
                 builder.append("  Mô tả: ").append(p.getDescription()).append("\n");
             }

             Double price = p.getPrice();
             if (price != null) {
                  try {
                       String formattedPrice = String.format(Locale.forLanguageTag("vi-VN"), "%,.0f VND", price);
                       builder.append("  Giá gốc: ").append(formattedPrice).append("\n");
                  } catch (Exception e) {
                       logger.error("Error formatting price {} for product ID {}: {}", price, p.getId(), e.getMessage()); // Giữ ERROR
                       builder.append("  Giá gốc: (Lỗi định dạng)\n");
                  }
             } else {
                  builder.append("  Giá gốc: (Chưa cập nhật)\n");
             }

             builder.append("  Số lượng tồn kho: ").append(p.getStockQuantity() != null ? p.getStockQuantity() : "Không xác định").append("\n");

             try {
                  Set<ProductVersion> versions = p.getVersions();

                  if (versions != null && !versions.isEmpty()) {
                      builder.append("  Các phiên bản:\n");
                      for (ProductVersion pv : versions) {
                          if (pv == null) {
                              logger.warn("Encountered a null ProductVersion in the set for product ID {}", p.getId()); // Giữ WARN
                              continue;
                          }

                          builder.append("    - Phiên bản: ").append(pv.getVersionName() != null ? pv.getVersionName() : "(Chưa có tên)").append("\n");

                          Double extraPrice = pv.getExtraPrice();
                          if (extraPrice != null && extraPrice != 0 && price != null) {
                              double versionPrice = price + extraPrice;
                               try {
                                    String formattedVersionPrice = String.format(Locale.forLanguageTag("vi-VN"), "%,.0f VND", versionPrice);
                                    builder.append("      Giá phiên bản: ").append(formattedVersionPrice).append("\n");
                               } catch (Exception e) {
                                    logger.error("Error formatting version price {} for version ID {}: {}", versionPrice, pv.getId(), e.getMessage()); // Giữ ERROR
                                    builder.append("      Giá phiên bản: (Lỗi định dạng)\n");
                               }
                          } else if (extraPrice != null && extraPrice != 0) {
                               builder.append("      Giá thêm: ").append(String.format(Locale.forLanguageTag("vi-VN"), "%,.0f VND", extraPrice)).append("\n");
                          }

                          try {
                               Set<ProductColor> colors = pv.getColors();

                               if (colors != null && !colors.isEmpty()) {
                                   String colorNames = colors.stream()
                                                             .filter(c -> c != null && c.getColorName() != null)
                                                             .map(ProductColor::getColorName)
                                                             .collect(Collectors.joining(", "));
                                   if (!colorNames.isEmpty()) {
                                        builder.append("      Màu sắc khả dụng: ").append(colorNames).append("\n");
                                   } else {
                                        builder.append("      Màu sắc: (Không có tên màu hợp lệ)\n");
                                   }
                               } else {
                                    builder.append("      Màu sắc: (Chưa cập nhật)\n");
                               }
                          } catch (Exception colorEx) {
                               logger.error("Error processing colors for version ID {}: {}", pv.getId(), colorEx.getMessage(), colorEx); // Giữ ERROR
                               builder.append("      Màu sắc: (Lỗi khi tải)\n");
                          }
                      } // Kết thúc vòng lặp version
                  } else { // Nếu không có version nào
                      builder.append("  Phiên bản: (Không có thông tin phiên bản)\n");
                  }
             } catch (Exception versionEx) { // Bắt lỗi khi truy cập p.getVersions() hoặc lỗi chung khác
                  logger.error("Error processing versions for product ID {}: {}", p.getId(), versionEx.getMessage(), versionEx); // Giữ ERROR
                  builder.append("  Phiên bản/Màu sắc: (Lỗi khi tải dữ liệu)\n");
             }
              builder.append("--------------------\n");
         } // Kết thúc vòng lặp product

        if (products.size() > limit) {
            builder.append("...\n(Và một số sản phẩm khác phù hợp với tìm kiếm của bạn)\n");
        }

        String contextResult = builder.toString();
        // Đã loại bỏ log context cuối cùng
        return contextResult;
    }
}
