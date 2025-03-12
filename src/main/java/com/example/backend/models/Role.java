package com.example.backend.models;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private RoleType name;

    public enum RoleType {
        ADMIN, NHAN_VIEN, KHACH_HANG
    }

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public RoleType getName() {
		return name;
	}

	public void setName(RoleType name) {
		this.name = name;
	}
    
    
}
