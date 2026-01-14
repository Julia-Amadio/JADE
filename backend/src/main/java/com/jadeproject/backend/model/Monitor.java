package com.jadeproject.backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "monitors")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Monitor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //Vários monitores pertencem a um usuário
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="user_id", nullable = false) //Mapeia a coluna FK 'user_id'
    /* Como classes User e Monitor estão relacionadas (um User tem lista de Monitores, e Monitor tem um User), ao transformar isso em JSON,
    * o Spring pode fazer: User -> Mostra Monitores -> Mostra User -> Mostra Monitores... (StackOverflowError).
    * Com @JsonIgnore, quando o spring for transformar Monitor em JSON, ele não mostra o objeto User inteiro dentro do corpo.*/
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 255)
    private String url;

    //No SQL a coluna é 'interval_seconds', então mapeamos o nome
    @Column(name = "interval_seconds", nullable = false)
    private Integer intervalSeconds = 300;  //Padrão 300 (5min) conforme SQL

    //No SQL 'is_active' é TRUE por padrão
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        /*A classe Monitor funciona como um modelo de formulário em branco.
        * Durante runtime, se temos X usuários criando monitores ao mesmo tempo, o Java cria X instâncias desse formulário na memória.
        * this se refere à cópia específica do formulário (OBJETO!) com que ele está lidando no momento.*/
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isActive == null) {
            this.isActive = true;
        }
        if (this.intervalSeconds == null) {
            this.intervalSeconds = 300;
        }
    }
}

/*O ciclo de vida de salvar algo no banco funciona da seguinte forma:
*   1. O objeto é criado no Java (new Monitor());
*   2. Os dados são preenchidos (monitor.setUrl("url.aqui"));
*   3. O repositório é chamado;
*   4. O @PrePersist roda AQUI;
*   5. O Hibernate gera o SQL (INSERT INTO...);
*   6. O banco grava.
*
* @PrePersist é um callback do ciclo de vida da entidade (Entity Lifecycle Callback), trabalhando como um interceptador síncrono
* que ocorre dentro da JVM. Ele garante que a transformação dos dados ocorra na memória antes da serialização para linguagem SQL.
* No mundo JPA/Hibernate, um objeto (como o Monitor) passa por 4 estados. O @PrePersist é o porteiro entre o estado 1 e o estado 2:
*   1. Transient: objeto é apenas memória RAM no Java. Não tem ID e o banco não sabe que ele existe (new Monitor()).
*   2. Managed: o Hibernate "abraça" o objeto. Ele está na memória do Hibernate (Persistence Context) e pronto para ir pro banco.
*   3. Detached: o objeto existe no banco, mas o Hibernate parou de olhar pra ele.
*   4. Removed: marcado para deleção.
*
* Quando chamamos monitorRepository.save(monitor), o seguinte algoritmo acontece dentro do motor do Hibernate:
*   1. Scan de metadados (boot time):
*       - Ao ligar o aplicação (SpringApplication.run), o Hibernate escaneia todas as classes @Entity.
*       - O Hibernate usa Reflection para ler o bytecode e "anota" que, na classe Monitor,
*         o método onCreate() tem a flag @PrePersist. Ele guarda esse ponteito num mapa de eventos.
*   2. Transição de estado (runtime):
*       - .save() é chamado e o Hibernate recebe o objeto Transient.
*       - Antes de promover o objeto para Managed, o Hibernate consulta o mapa de eventos e executa onCreate().
*   3. Injeção e mutação:
*       - O Hibernate invoca o método onCreate() na instância específica da memória usando o this.
*       - O código roda e muta o estado do objeto na memória (preenche createdAt, isActive).
*       - O objeto agora está modificado.
*   4. Geração do SQL (flush):
*       - Só agora o Hibernate olha para os campos do objeto para montar o SQL. Como o passo 3 já rodou, ele vê isActive = true.
*       - Hibernate monta o SQL: INSERT INTO monitors(is_active, ...) VALUES (true, ...) e envia para o Postgres.
*
* Caso deixássemos o banco resolver (default do SQL), um possível fluxo seria:
*   1. Hibernate vê isActive = null (motivo disto explicado mais abaixo).
*   2. Hibernate manda INSERT ... VALUES (null).
*   3. O banco recebe null.
* Se a coluna for NOT NULL, o banco rejeita null antes de aplicar o default ou aplica o default se a coluna aceitar.
* Porém, nesse último caso, o objeto no Java ficaria desatualizado, ainda com null, até que fosse recarregado do banco.
*
* Quando o backend recebe requisição HTTP (REST), quem cria o objeto Monitor não é new Monitor(), e sim uma biblioteca
* chamada Jackson (que transforma JSON em Java). Imaginando o cenário:
*   1. Declaração Java: private Boolean isActive = true;
*   2. O front envia um JSON incorreto devido a um bug de JavaScript:
*       {
            "url": "google.com",
            "isActive": null
        }
*   3. O Jackson lê null no JSON e sobrescreve o true padrão. Na memória, monitor.isActive agora é NULL.
*   4. Objeto chega no Hibernate. SQL gerado: INSERT INTO monitors (url, is_active) VALUES ('google.com', NULL);
*   5. O banco (Postgres):
*       - Recebe o NULL explícito.
*       - REGRA DO SQL: DEFAULT TRUE do banco só é acionado se a coluna for omitida no insert.
*         Hibernate enviou NULL, então o banco entende que ele quer salvar nulo MESMO.
*       - RESULTADO: se a coluna for NOT NULL, o banco retorna erro e a requisição falha.
* */