package com.travel.management.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;
    @Column(name = "first_name" , nullable = false)
    private String firstName;
    @Column(name = "last_name" , nullable = false)
    private String lastName;
    @Column(unique = true,nullable = false)
    private String email;
    @Column(nullable = false)
    //This setting ensures that the password will be accepted when reading JSON (deserialization)
    // but will not be included in the JSON output (serialization).
    // This is generally preferred for sensitive data like passwords.
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;
    @Column(unique = true)
    private String phoneNumber;

    private String country;

    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "authorities",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>(); //set as we need it to be unique
    private boolean enabled;
    @Column(name = "verification")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private long verification;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<Booking> bookings;
    private String refreshToken;
    private LocalDateTime refreshTokenExpiry;
    private String resetPasswordToken;
    private LocalDateTime resetPasswordTokenExpiry;
    private int resetPasswordAttempts;
    private LocalDateTime verificationCodeExpiry;


    private int failedLoginAttempts = 0;
    private LocalDateTime lockoutTime;
    private String lastPassword;

    public boolean isAccountNonLocked() {
        if (lockoutTime != null && lockoutTime.isAfter(LocalDateTime.now())) {
            return false; // Account is locked
        }
        return resetPasswordAttempts < 6; // Your existing check
    }
    public boolean isTokenValid(String token) {
        return refreshToken != null &&
                refreshToken.equals(token) &&
                refreshTokenExpiry.isAfter(LocalDateTime.now());
    }

    public void incrementFailedAttempts() {
        failedLoginAttempts++;
        if (failedLoginAttempts >= 5) {
            lockoutTime = LocalDateTime.now().plusMinutes(15);
        }
    }
    public void resetFailedAttempts() {
        failedLoginAttempts = 0;
        lockoutTime = null;
    }
    public boolean isPasswordReused(String newPassword) {
        return lastPassword != null && lastPassword.equals(newPassword);
    }
}
