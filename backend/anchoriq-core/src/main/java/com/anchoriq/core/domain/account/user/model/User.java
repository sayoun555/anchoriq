package com.anchoriq.core.domain.account.user.model;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Embedded
    @AttributeOverride(name = "value", column = @Column(name = "email", nullable = false, unique = true))
    private Email email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    private User(Email email, String encodedPassword, String name, Role role) {
        this.email = email;
        this.password = encodedPassword;
        this.name = name;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public static User create(Email email, String encodedPassword, String name) {
        return new User(email, encodedPassword, name, Role.USER);
    }

    public static User createAdmin(Email email, String encodedPassword, String name) {
        return new User(email, encodedPassword, name, Role.ADMIN);
    }

    public void changePassword(String newEncodedPassword) {
        if (newEncodedPassword == null || newEncodedPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        this.password = newEncodedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    public String getEmailValue() {
        return this.email.getValue();
    }
}
