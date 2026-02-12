package com.jadeproject.backend.repository;

import com.jadeproject.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/*Container que encapsula a possível ausência de um valor.
* Substitui o retorno de 'null', forçando o tratamento explicito
* caso o registro não seja encontrado no banco, evitando NullPointerException.
*
* Ex.: ao retornar Optional<User>, o método sinaliza explicitamente para quem for usar:
*   "Pode ser que eu não encontre esse usuário no banco. Cheque se tem algo aqui dentro antes de tentar usar."*/
import java.util.Optional;

/*O JpaRepository é uma classe genérica (Generics). É uma "fábrica de repositórios" que precisa de duas informações para funcionar:
*   - T (type): qual é a entidade a ser gerenciada?
*     Resposta: User. Assim ele sabe que findAll() retorna uma lista de Users e que deve olhar a tabela users.
*   - ID (identifier): qual é o tipo de dado da PK (@Id) da entidade?
*     Resposta: Long. O método findById(x) precisa saber o tipo de 'x'. O Java não irá compílar com tipo errado.*/
@Repository
public interface UserRepository extends JpaRepository<User, Long>{

    //Busca usuário pelo username exato
    Optional<User> findByUsername(String username);

    //Busca pelo email exato
    Optional<User> findByEmail(String email);

    //Verifica se já existe (útil para não dar erro de chave duplicada em cadastro)
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
