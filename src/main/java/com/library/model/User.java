package com.library.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    private String phone;
    private String address;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoleName role;

    @Column(nullable = false)
    private boolean active = true;

    public User(String username, String password, String fullName, String email, String phone, String address, RoleName role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.role = role;
        this.active = true;
    }

    public User(String username, String password, String fullName, String email, String phone, String address, LocalDate dateOfBirth, RoleName role) {
        this(username, password, fullName, email, phone, address, role);
        this.dateOfBirth = dateOfBirth;
    }

    public String getReaderCode() {
        if (this.role == RoleName.ROLE_ADMIN) {
            return String.format("QL-%04d", this.id != null ? this.id : 1);
        }
        return String.format("DG-%04d", this.id != null ? this.id : 1);
    }
}