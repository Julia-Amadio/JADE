package com.jadeproject.backend.model;

//API de persistência do JPA. Traz anotações (mapeamento objeto-relacional) que 'ensinam' o Java a conversar com o BD.
//Origem de @Entity, @Table, @Id, @Column, @GeneratedValue, etc.
import jakarta.persistence.*;

//@Data gera getters e setters, toString(), equals() e hashCode() em tempo de compilação.
//Evita linhas de código repetitivo (boilerplate)
import lombok.Data;

//Gerador de construtor vazio. Cria algo como public User() {} invisível
/*O Hibernate (JPA) >exige< 1 construtor vazio.
  Ele precisa instanciar o objeto vazio primeiro para depois preencher com os dados do BD.
  Sem isso o código quebra ao tentar ler do banco.*/
import lombok.NoArgsConstructor;

//Gerador de construtor completo.
/*Cria um construtor que aceita todos os campos como argumento
  ex: new user(id, nome, email, senha...))
  útil para testes ou para criar um objeto novo já preenchido em uma linha só.*/
import lombok.AllArgsConstructor;

//Para ignorar o campo passwordHash em logs
import lombok.ToString;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "users") //Garante que o Java procure a tabela 'users' (plural) e não 'User'
@Data //O LOMBOK cria getters, setters, toString, equals e hashcode automaticamente
@NoArgsConstructor //O LOMBOK cria o construtor vazio (obrigatório pro JPA)
@AllArgsConstructor //O LOMBOK cria o construtor com todos os argumentos

public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "pswd_hash", nullable = false, length = 255) //'name' diz qual é a coluna no banco
    @ToString.Exclude
    private String passwordHash;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    //Método extra para garantir que a data seja preenchida antes de salvar,
    //caso o banco não faça isso sozinho (backup de segurança)
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }
}