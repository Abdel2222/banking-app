package com.banking.repositories;

import com.banking.entities.Chat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, Long> {

    // Inbox du destinataire (les messages reçus)
    List<Chat> findByCompteDestinataire_IdOrderByCreatedAtDesc(Long compteId);

    // Messages envoyés par ce compte
    List<Chat> findByCompteSource_IdOrderByCreatedAtDesc(Long compteId);

    // Tous les chats où le compte est impliqué (source OU destinataire)
    List<Chat> findByCompteSource_IdOrCompteDestinataire_IdOrderByCreatedAtDesc(Long sourceId, Long destId);
}
