package com.agrimarket.demo.model.mongo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "auth_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSession {

    @Id
    private String id;

    @Indexed(unique = true)
    private String tokenHash;

    private String userId;
    private String email;
    private String role;

    @Indexed(expireAfter = "0s")
    private LocalDateTime expiresAt;

    private LocalDateTime createdAt;
}
