package com.example.backend.models;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_versions")
public class ProductVersion {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String versionName; // Ví dụ: "8GB RAM - 128GB SSD"

    @Column(nullable = true)
    private Double extraPrice; // Có thể null nếu không có giá chênh lệch

    @OneToMany(mappedBy = "productVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductColor> colors = new HashSet<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Product getProduct() {
		return product;
	}

	public void setProduct(Product product) {
		this.product = product;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public Double getExtraPrice() {
		return extraPrice;
	}

	public void setExtraPrice(Double extraPrice) {
		this.extraPrice = extraPrice;
	}

	public Set<ProductColor> getColors() {
		return colors;
	}

	public void setColors(Set<ProductColor> colors) {
		this.colors = colors;
	}
}

