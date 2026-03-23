package com.tracker.gateway.user;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@ToString
@EqualsAndHashCode
public class User {

    @Id
    @GeneratedValue
    private String id;

    private String email;
    private String password;
}
