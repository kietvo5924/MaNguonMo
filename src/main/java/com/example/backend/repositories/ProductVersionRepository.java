package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.models.ProductVersion;

public interface ProductVersionRepository extends JpaRepository<ProductVersion, Long> {

}
