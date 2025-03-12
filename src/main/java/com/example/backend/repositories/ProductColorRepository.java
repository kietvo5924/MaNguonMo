package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.models.ProductColor;

public interface ProductColorRepository extends JpaRepository<ProductColor, Long> {

}
