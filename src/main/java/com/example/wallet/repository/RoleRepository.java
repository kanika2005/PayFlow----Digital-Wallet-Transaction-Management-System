package com.example.wallet.repository;

import com.example.wallet.entity.Role;
import com.example.wallet.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}