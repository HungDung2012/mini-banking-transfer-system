package com.btlbanking.account.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {

  Optional<AccountEntity> findByAccountNumber(String accountNumber);

  Optional<AccountEntity> findFirstByOwnerNameIgnoreCase(String ownerName);

  boolean existsByAccountNumber(String accountNumber);
}
