package com.example.wallet.repository;

import com.example.wallet.entity.Wallet;
import java.math.BigDecimal;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByUser_Id(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w join fetch w.user where w.user.id = :userId")
    Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);

    @Query("select coalesce(sum(w.balance), 0) from Wallet w")
    BigDecimal sumAllBalances();

    @Query("select count(w) from Wallet w")
    long countAllWallets();
}