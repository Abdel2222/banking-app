package com.banking.controllers;

import com.banking.services.OperationService;
import com.banking.services.ReleveDeCompteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.springframework.format.annotation.DateTimeFormat.ISO;

@RestController
@RequestMapping("/api/statements")
public class ReleveDeCompteController {

    private final ReleveDeCompteService releveService; // ta version @Service qui produit le texte
    private final OperationService operationService;   // pour le PDF

    public ReleveDeCompteController(ReleveDeCompteService releveService,
                                    OperationService operationService) {
        this.releveService = releveService;
        this.operationService = operationService;
    }

    // Relevé TEXTE "lisible" (plain text) : /api/statements/{num}/plain?start=YYYY-MM-DD&end=YYYY-MM-DD
    @GetMapping("/{numeroCompte}/plain")
    public ResponseEntity<byte[]> getPlain(
            @PathVariable String numeroCompte,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate end) {

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to   = end.atTime(23,59,59);

        String txt = releveService.generatePlainTextStatement(numeroCompte, from, to);
        byte[] body = txt.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        headers.setContentLength(body.length);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("releve-" + numeroCompte + ".txt").build()
        );
        return ResponseEntity.ok().headers(headers).body(body);
    }

    // Relevé PDF : /api/statements/{num}/pdf?start=YYYY-MM-DD&end=YYYY-MM-DD
    @GetMapping("/{numeroCompte}/pdf")
    public ResponseEntity<byte[]> getPdf(
            @PathVariable String numeroCompte,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate end) {

        LocalDateTime from = start.atStartOfDay();
        LocalDateTime to   = end.atTime(23,59,59);

        byte[] pdf = operationService.exportOperationsToPdf(numeroCompte, from, to);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentLength(pdf.length);
        headers.setContentDisposition(
                ContentDisposition.attachment().filename("releve-" + numeroCompte + ".pdf").build()
        );
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
