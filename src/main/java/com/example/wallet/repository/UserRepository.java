package com.example.wallet.repository;

import com.example.wallet.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = {"role", "wallet"})
    List<User> findAllByOrderByIdAsc();
}