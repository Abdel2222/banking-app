package com.banking.repositories;

import com.banking.entities.Chat;
import com.banking.entities.ChatStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /** Tous les chats triés du plus récent au plus ancien */
    List<Chat> findAllByOrderByDateHeureDesc();

    /** Chats par client */
    List<Chat> findByClientIdOrderByDateHeureDesc(Long clientId);

    /** Demandes en attente pour la queue admin */
    List<Chat> findByStatutOrderByDateHeureDesc(ChatStatut statut);

    /** Compteur de demandes par statut (badge admin) */
    long countByStatut(ChatStatut statut);
}