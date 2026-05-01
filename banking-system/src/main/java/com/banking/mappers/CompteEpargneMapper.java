package com.banking.mappers;

import com.banking.dto.response.CompteEpargneResponse;
import com.banking.entities.CompteEpargne;
import org.springframework.stereotype.Component;

@Component
public class CompteEpargneMapper {
    public CompteEpargneResponse toResponse(CompteEpargne ce) {
        return CompteEpargneResponse.builder()
                .id(ce.getId())
                .numCompteBancaire(ce.getCompteBancaire()!=null ? ce.getCompteBancaire().getNumCompte() : null)
                .numCompteEpargne(ce.getNumCompteEpargne())
                .soldeEpargne(ce.getSoldeEpargne())
                .tauxInteret(ce.getTauxInteret())
                .taxationVirtuelle(ce.getTaxationVirtuelle())
                .derniereCapitalisation(ce.getDateDerniereCapitalisation())
                .build();
    }
}
