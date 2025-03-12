package com.example.backend.DTO;

import java.util.Set;

public class ProductVersionDTO {
	private Long id;
    private String versionName;
    private Double extraPrice;
    private Set<ProductColorDTO> colors;
    
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
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
	public Set<ProductColorDTO> getColors() {
		return colors;
	}
	public void setColors(Set<ProductColorDTO> colors) {
		this.colors = colors;
	}
    
    
}
