package com.example.backend.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_colors")
public class ProductColor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_version_id", nullable = false)
    private ProductVersion productVersion;

    @Column(nullable = false, length = 30)
    private String colorName; // Ví dụ: "Đen", "Bạc", "Xanh"

    @Column(nullable = true, length = 7)
    private String colorCode; // Có thể null nếu không có mã màu cụ thể

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ProductVersion getProductVersion() {
		return productVersion;
	}

	public void setProductVersion(ProductVersion productVersion) {
		this.productVersion = productVersion;
	}

	public String getColorName() {
		return colorName;
	}

	public void setColorName(String colorName) {
		this.colorName = colorName;
	}

	public String getColorCode() {
		return colorCode;
	}

	public void setColorCode(String colorCode) {
		this.colorCode = colorCode;
	}
    
    
}
