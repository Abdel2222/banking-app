package com.banking.repositories;

import com.banking.entities.Chat;
import com.banking.entity.enums.ChatStatut;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    List<Chat> findAllByOrderByDateHeureDesc();

    List<Chat> findByClientIdOrderByDateHeureDesc(Long clientId);

    List<Chat> findByStatutOrderByDateHeureDesc(ChatStatut statut);

    long countByStatut(ChatStatut statut);

    List<Chat> findByClientIdAndReponseAdminIsNotNullOrderByDateReponseDesc(Long clientId);
}