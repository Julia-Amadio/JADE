package com.jadeproject.backend.repository;

import com.jadeproject.backend.model.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonitorRepository extends JpaRepository<Monitor, Long>{
    //O Spring entende que Monitor tem um campo "user" e busca pelo ID desse user.
    //SELECT * FROM monitors WHERE user_id = ?
    List<Monitor> findByUserId(Long userId);

    //Buscar monitor por URL (útil para validações)
    //Agora considera duplicidade entre diferentes usuários
    List<Monitor> findByUrl(String url);

    //Verifica se existe um monitor com esta URL pertencente a este USER_ID -- CORRIGIDO
    //Dois users podem monitorar uma mesma URL, mas o user não pode ter mais de um monitor com a mesma URL
    boolean existsByUrlAndUserId(String url, Long userId);

    //Verifica se existe um monitor com este NOME pertencente a este USER_ID
    boolean existsByNameAndUserId(String name, Long userId);
}