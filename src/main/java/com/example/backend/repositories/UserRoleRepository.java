package com.example.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.backend.models.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

}
