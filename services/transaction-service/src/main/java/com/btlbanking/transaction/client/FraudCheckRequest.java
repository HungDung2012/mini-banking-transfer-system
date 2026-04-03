package com.btlbanking.transaction.client;

import java.math.BigDecimal;

record FraudCheckRequest(String sourceAccount, String destinationAccount, BigDecimal amount) {
}
