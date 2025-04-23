package com.example.backend.DTO;

import java.util.Set;

public class ProductDTO {
	 private Long id;
	    private String name;
	    private String description;
	    private Double price;
	    private Integer stockQuantity;
	    private String imageUrl;
	    private Long categoryId; // Chỉ lưu ID của Category
	    private Set<ProductVersionDTO> versions;
	    private boolean popular;
	    
		public boolean isPopular() {
			return popular;
		}
		public void setPopular(boolean popular) {
			this.popular = popular;
		}
		public Long getId() {
			return id;
		}
		public void setId(Long id) {
			this.id = id;
		}
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public Double getPrice() {
			return price;
		}
		public void setPrice(Double price) {
			this.price = price;
		}
		public Integer getStockQuantity() {
			return stockQuantity;
		}
		public void setStockQuantity(Integer stockQuantity) {
			this.stockQuantity = stockQuantity;
		}
		public String getImageUrl() {
			return imageUrl;
		}
		public void setImageUrl(String imageUrl) {
			this.imageUrl = imageUrl;
		}
		public Long getCategoryId() {
			return categoryId;
		}
		public void setCategoryId(Long categoryId) {
			this.categoryId = categoryId;
		}
		public Set<ProductVersionDTO> getVersions() {
			return versions;
		}
		public void setVersions(Set<ProductVersionDTO> versions) {
			this.versions = versions;
		}
	    
}
