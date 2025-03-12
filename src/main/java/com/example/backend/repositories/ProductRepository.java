package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.backend.models.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
	
}

