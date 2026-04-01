package com.btlbanking.transaction.web;

import com.btlbanking.transaction.domain.TransferStatus;
import java.util.UUID;

public record TransferResponse(UUID transferId, TransferStatus status) {
}
