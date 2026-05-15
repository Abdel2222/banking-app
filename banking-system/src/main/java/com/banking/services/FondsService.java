package com.banking.services;

import com.banking.dto.request.CreateFondsRequest;
import com.banking.dto.response.FondsResponse;
import com.banking.entities.Fonds;
import com.banking.entity.enums.NiveauRisque;
import com.banking.exceptions.BusinessException;
import com.banking.exceptions.ResourceNotFoundException;
import com.banking.repositories.FondsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FondsService {

    private final FondsRepository fondsRepository;

    public FondsService(FondsRepository fondsRepository) {
        this.fondsRepository = fondsRepository;
    }

    @Transactional
    public FondsResponse creerFonds(CreateFondsRequest request) {
        if (fondsRepository.existsByCodeIdentification(request.getCodeIdentification())) {
            throw new BusinessException("Un fonds avec ce code d'identification existe déjà");
        }

        Fonds fonds = new Fonds(
                request.getNomFonds(),
                request.getCodeIdentification(),
                request.getRendement(),
                request.getNiveauRisque(),
                request.getMontantMinimum()
        );

        Fonds saved = fondsRepository.save(fonds);
        return new FondsResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<FondsResponse> getTousLesFonds() {
        return fondsRepository.findAll()
                .stream()
                .map(FondsResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FondsResponse> getFondsActifs() {
        return fondsRepository.findByEstActifTrue()
                .stream()
                .map(FondsResponse::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public FondsResponse getFondsById(Long id) {
        Fonds fonds = fondsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fonds introuvable avec l'id : " + id));

        return new FondsResponse(fonds);
    }

    @Transactional(readOnly = true)
    public List<FondsResponse> getFondsParRisque(NiveauRisque niveauRisque) {
        return fondsRepository.findByNiveauRisque(niveauRisque)
                .stream()
                .map(FondsResponse::new)
                .toList();
    }

    @Transactional
    public FondsResponse desactiverFonds(Long id) {
        Fonds fonds = fondsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fonds introuvable avec l'id : " + id));

        fonds.setEstActif(false);
        return new FondsResponse(fondsRepository.save(fonds));
    }

    @Transactional
    public FondsResponse activerFonds(Long id) {
        Fonds fonds = fondsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fonds introuvable avec l'id : " + id));

        fonds.setEstActif(true);
        return new FondsResponse(fondsRepository.save(fonds));
    }
}