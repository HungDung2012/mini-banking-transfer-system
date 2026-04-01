package com.btlbanking.transaction.domain;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<TransferEntity, UUID> {
}
