package com.banking.services;

import com.banking.entities.Operation;
import java.math.BigDecimal;

public interface ReleveDeCompteService {
    void logOperation(Operation op, BigDecimal soldeApres);
}
