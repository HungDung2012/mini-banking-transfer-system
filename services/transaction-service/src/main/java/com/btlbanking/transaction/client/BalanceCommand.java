package com.btlbanking.transaction.client;

import java.math.BigDecimal;

record BalanceCommand(String accountNumber, BigDecimal amount) {
}
