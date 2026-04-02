# Arquitetura do backend - JADE

Este documento detalha a arquitetura da aplicação backend do JADE, as responsabilidades de cada componente e as decisões de design que guiaram sua construção.

## 1. Visão geral da arquitetura

O backend do JADE é uma **API REST monolítica** construída com **Java 21** e o ecossistema **Spring Boot 4**. A escolha por um monólito foi estratégica para simplificar o desenvolvimento e o deploy em um estágio inicial, mantendo todas as responsabilidades de negócio em uma única base de código coesa.

A arquitetura segue o padrão **Model-View-Controller (MVC)**, adaptado para uma API REST, resultando em uma estrutura de camadas bem definida:

-   **Camada de Apresentação (`controller`):** responsável por expor os endpoints HTTP, receber requisições e retornar respostas em JSON.
-   **Camada de Serviço (`service`):** onde reside a lógica de negócio principal da aplicação.
-   **Camada de Acesso a Dados (`repository`):** abstrai a comunicação com o banco de dados.
-   **Camada de Domínio (`model`):** representa as entidades do banco de dados como objetos Java.

A aplicação utiliza **PostgreSQL** como banco de dados, com o **Flyway** para gerenciar as migrações de schema de forma versionada e automatizada. A segurança é garantida pelo **Spring Security**, com autenticação baseada em **Tokens JWT** e controle de acesso por papéis (RBAC).

---

## 2. Estrutura de pacotes (`backend`)

O código-fonte está organizado em pacotes, cada um com uma responsabilidade clara, promovendo a separação de conceitos (Separation of Concerns).

### `com.jadeproject.backend.controller`

-   **Responsabilidade:** atuar como a porta de entrada da API. Esta camada traduz requisições HTTP para chamadas de métodos na camada de serviço.
-   **Funções:**
    -   Mapear URLs (endpoints) para métodos específicos (ex: `GET /monitors/{id}`).
    -   Deserializar o corpo das requisições (JSON) para Data Transfer Objects (DTOs).
    -   Validar os dados de entrada usando anotações (`@Valid`).
    -   Orquestrar a resposta HTTP, definindo o status (200, 201, 404, etc.) e serializando os DTOs de resposta para JSON.
    -   **Realizar a primeira camada de verificação de permissões de acesso (ownership)**, garantindo que um usuário não acesse recursos de outro.

### `com.jadeproject.backend.service`

-   **Responsabilidade:** conter a lógica de negócio central e as regras do sistema. É o "cérebro" da aplicação.
-   **Funções:**
    -   Implementar as operações de CRUD (Create, Read, Update, Delete) de forma mais complexa.
    -   Orquestrar chamadas a múltiplos repositórios para construir uma resposta completa.
    -   Garantir a integridade dos dados através de transações (`@Transactional`). Se uma operação falhar no meio, todas as alterações no banco são revertidas.
    -   Implementar regras que não podem ser expressas apenas com o banco de dados (ex: um usuário não pode ter dois monitores com o mesmo nome).
    -   Aqui também reside o `MonitorScheduler`, o robô que realiza o monitoramento.

### `com.jadeproject.backend.repository`

-   **Responsabilidade:** abstrair o acesso ao banco de dados. Esta camada é a única que "fala" diretamente com o banco.
-   **Implementação:** são interfaces que estendem `JpaRepository`. O Spring Data JPA implementa automaticamente os métodos básicos de acesso a dados (como `save()`, `findById()`, `findAll()`) em tempo de execução.
-   **Função:** permitir a criação de consultas customizadas através de nomes de métodos (Query Methods) ou com a anotação `@Query` para lógicas mais complexas em SQL nativo.

### `com.jadeproject.backend.model`

-   **Responsabilidade:** representar as tabelas do banco de dados como classes Java (POJOs - Plain Old Java Objects).
-   **Implementação:** são as **Entidades JPA**, anotadas com `@Entity`. Cada instância de uma classe de modelo corresponde a uma linha em uma tabela.
-   **Funções:**
    -   Definir o schema do banco de dados (nomes de tabelas, colunas, tipos de dados e relacionamentos) através de anotações como `@Table`, `@Column`, `@ManyToOne`, etc.
    -   Servir como a "fonte da verdade" para a estrutura dos dados, usada pelo Hibernate para validação.

### `com.jadeproject.backend.dto` (Data Transfer Object)

-   **Responsabilidade:** servir como um contrato de dados entre o cliente (frontend) e a API. Desacopla a representação interna (Entidades) da representação externa.
-   **Funções:**
    -   **Segurança:** evitar a exposição de dados sensíveis (como `pswd_hash`) que existem na entidade do banco.
    -   **Flexibilidade:** permitir que a API receba apenas os dados necessários para uma operação específica (ex: `MonitorCreateDTO` para criar, `MonitorUpdateDTO` para atualizar).
    -   **Prevenção de Erros:** evitar problemas de serialização, como referências circulares (Lazy Loading), que podem ocorrer ao expor entidades JPA diretamente.

### `com.jadeproject.backend.security`

-   **Responsabilidade:** centralizar toda a configuração e lógica de autenticação e autorização.
-   **Componentes:**
    -   `SecurityConfig`: define as regras de acesso para cada endpoint (quais rotas são públicas, quais exigem autenticação e quais exigem um papel específico como `ROLE_ADMIN`).
    -   `SecurityFilter`: intercepta cada requisição, extrai o token JWT do cabeçalho, valida-o e, se for válido, informa ao Spring Security que o usuário está autenticado.
    -   `TokenService`: gera e valida os tokens JWT.
    -   `UserDetailsImpl` e `AuthorizationService`: adaptam o Spring Security para usar o e-mail como login e carregar os dados do usuário do banco.

### `com.jadeproject.backend.config`

-   **Responsabilidade:** agrupar classes de configuração do Spring.
-   **Componentes:**
    -   `WebConfig`: configura o CORS (Cross-Origin Resource Sharing) para permitir que o frontend (rodando em `localhost:5173`) possa se comunicar com o backend (em `localhost:8080`).

### `com.jadeproject.backend.exception`

-   **Responsabilidade:** padronizar o tratamento de erros em toda a aplicação,
    tanto as exceções de negócio quanto os erros de infraestrutura HTTP.

-   **Exceções customizadas:**
    -   `DataConflictException`: lançada quando uma operação viola uma regra de
        unicidade de negócio (ex: email já cadastrado, nome de monitor duplicado
        para o mesmo usuário). Resulta em resposta `409 Conflict`.
    -   `ResourceNotFoundException`: lançada quando um recurso solicitado não existe
        no banco de dados (ex: monitor ou usuário não encontrado pelo ID). Resulta
        em resposta `404 Not Found`.

-   **`GlobalExceptionHandler`:** classe anotada com `@RestControllerAdvice` que
    intercepta exceções lançadas em qualquer camada e as transforma em respostas
    JSON padronizadas via `StandardErrorDTO`. Cobre os seguintes casos:
    -   `MethodArgumentNotValidException` → `400 Bad Request` com mapa de erros por campo
    -   `DataConflictException` → `409 Conflict`
    -   `ResourceNotFoundException` → `404 Not Found`
    -   `NoResourceFoundException` → `404 Not Found` (rotas inexistentes no Spring 6)
    -   `HttpMessageNotReadableException` → `400 Bad Request` (JSON malformado)
    -   `ResponseStatusException` → status dinâmico (lançada pelos controllers)
    -   `Exception` → `500 Internal Server Error` com stack trace apenas no servidor
---

## 3. Fluxo de uma requisição

Para ilustrar como as camadas interagem, veja o fluxo de uma requisição para criar um novo monitor (`POST /monitors/user/{userId}`):

1.  **`SecurityFilter`:** o filtro de segurança intercepta a requisição. Ele valida o token JWT no cabeçalho `Authorization`. Se o token for válido, ele popula o `SecurityContext` com os dados do usuário autenticado.
2.  **`MonitorController`:** a requisição chega ao método `createMonitor`.
    -   O `@RequestBody` é deserializado para um `MonitorCreateDTO`.
    -   O `@Valid` dispara as validações (ex: campos não nulos).
    -   O método `checkUserPermission` é chamado para garantir que o usuário autenticado é o dono do `userId` ou um `ADMIN`.
    -   O controller converte o `MonitorCreateDTO` para uma entidade `Monitor`.
3.  **`MonitorService`:** o controller chama o método `createMonitor` no serviço, passando a entidade.
    -   O `@Transactional` inicia uma transação com o banco de dados.
    -   O serviço realiza validações de negócio: verifica se o usuário existe e se já não há um monitor com o mesmo nome para aquele usuário.
4.  **`MonitorRepository`:** o serviço chama `monitorRepository.save(monitor)`.
    -   O Spring Data JPA traduz essa chamada para um comando `INSERT` em SQL.
5.  **Banco de dados:** o novo monitor é persistido.
6.  **Retorno:** a operação volta pelas camadas.
    -   O repositório retorna a entidade salva com o ID gerado.
    -   O serviço retorna a entidade para o controller.
    -   O controller converte a entidade `Monitor` para um `MonitorResponseDTO` (para não expor dados internos).
    -   O controller cria uma `ResponseEntity` com status `201 Created` e o DTO no corpo, que é serializado para JSON e enviado de volta ao cliente.

Se qualquer passo falhar (ex: validação de negócio no serviço), uma exceção é lançada, a transação (`@Transactional`) faz o rollback de qualquer alteração no banco, e o `GlobalExceptionHandler` captura a exceção para formatar uma resposta de erro padronizada.

---

## 4. O coração do JADE: `MonitorScheduler`

O `MonitorScheduler` é o componente proativo do sistema, responsável por executar as verificações de disponibilidade de forma autônoma e inteligente.

### Funcionamento e "smart polling"

Em vez de verificar todos os monitores a cada ciclo, o que seria ineficiente, o scheduler adota uma estratégia de **"Smart Polling"**:

1.  **Consulta Inteligente:** A cada 10 segundos (`@Scheduled(fixedDelay = 10000)`), o scheduler não busca todos os monitores. Em vez disso, ele executa uma query SQL nativa otimizada (`findMonitorsToProcess`) que pede ao banco de dados para retornar **apenas os monitores cuja hora da próxima verificação já passou**. A lógica é:
    ```sql
    -- Retorna monitores onde:
    -- 1. A última verificação é nula (nunca rodou)
    -- OU
    -- 2. A (última verificação + intervalo) já é menor que o tempo atual
    SELECT * FROM monitors WHERE last_checked IS NULL OR (last_checked + make_interval(secs => interval_seconds)) < CURRENT_TIMESTAMP
    ```
    Isso delega o trabalho pesado de cálculo de tempo para o banco de dados, que é extremamente eficiente nisso.

2.  **Processamento Individual:** Para cada monitor retornado pela query, o scheduler:
    -   Executa o "ping" na URL configurada, medindo o tempo de resposta e capturando o status HTTP.
    -   Chama o `MonitorHistoryService` para salvar um registro de log (sucesso ou falha).
    -   Atualiza o campo `last_checked` do monitor com o timestamp atual (`OffsetDateTime.now(ZoneOffset.UTC)`). Isso "reseta" o cronômetro do monitor, garantindo que ele só será pego pela query novamente quando seu intervalo expirar.

### Gestão de Incidentes

O scheduler também é responsável por dar inteligência ao monitoramento, decidindo quando abrir ou fechar um incidente:

-   **Evento DOWN (`isUp = false`):**
    -   O scheduler chama `incidentService.handleDownEvent()`.
    -   O serviço verifica se **já existe um incidente com status "OPEN"** para aquele monitor.
    -   Se **não** existe, ele cria um novo incidente, registrando o motivo da falha.
    -   Se **já existe**, ele não faz nada, evitando a criação de múltiplos incidentes para uma única falha contínua.

-   **Evento UP (`isUp = true`):**
    -   O scheduler chama `incidentService.handleUpEvent()`.
    -   O serviço verifica se **existe um incidente com status "OPEN"** para aquele monitor.
    -   Se existe, ele o atualiza para o status **"RESOLVED"** e registra a data de finalização, sinalizando que o serviço voltou ao normal.

Essa abordagem transforma uma simples sequência de falhas em um evento único e gerenciável (um "incidente"), que representa o período total de indisponibilidade.

---

## 5. Componentes auxiliares e testes de resiliência

Para garantir que o sistema de monitoramento funcione corretamente em cenários adversos, o projeto inclui componentes específicos para simulação de falhas e carga inicial de dados.

### `DataLoader.java`

Uma classe de configuração executada na inicialização do sistema
(`CommandLineRunner`). Sua função é garantir que o banco de dados não nasça vazio,
facilitando o desenvolvimento e demonstração.

-   Cria automaticamente um usuário administrador (`jade_admin`) caso não exista.
-   Popula o banco com monitores de teste pré-configurados apontando para o
    `ChaosController`, garantindo que o scheduler tenha trabalho a fazer imediatamente.

Uma decisão intencional é que o `DataLoader` **não acessa os repositórios diretamente**
— ele passa pelos `UserService` e `MonitorService`, sujeitando a carga inicial às
mesmas validações de negócio que qualquer requisição real. Isso garante que os dados
de teste são criados com as mesmas regras (unicidade de email, criptografia de senha,
validação de intervalo mínimo) e evita que o ambiente de desenvolvimento fique em
um estado inconsistente que não reproduziria em produção.

### `ChaosController.java` (Engenharia do Caos)
Um controlador especial projetado para **falhar de propósito**. Ele expõe endpoints locais que simulam comportamentos instáveis de servidores reais, permitindo validar se o Scheduler está registrando os incidentes corretamente:
- `/fantoche/up`: retorna sempre 200 OK.
- `/fantoche/down`: retorna sempre 500 Internal Server Error.
- `/fantoche/slow`: aguarda 5 segundos antes de responder, forçando um Timeout no scheduler (que tem limite de 3s).
- `/fantoche/random`: retorna sucesso ou falha aleatoriamente, simulando intermitência.

---

## 6. Decisões de arquitetura

### Facilidade de ambiente com Docker e fallback de propriedades

Para facilitar a vida de desenvolvedores e testers que clonam o repositório, foi adotada uma configuração híbrida:

-   **`docker-compose.yml`:** fornece um banco de dados PostgreSQL local e pré-configurado, eliminando a necessidade de uma conta no Neon ou de instalar o Postgres manualmente.
-   **`application.properties` com Fallback:** as propriedades de conexão com o banco são configuradas para serem resilientes. Elas tentam primeiro usar variáveis de ambiente (destinadas à produção ou ao ambiente do desenvolvedor principal com Neon). Se as variáveis não existirem, elas automaticamente utilizam os valores fixos do banco de dados do Docker.
    ```properties
    spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5433/jade_local_db}
    spring.datasource.username=${DB_USERNAME:tester_jade}
    ```
    Isso permite que um terceiro simplesmente execute `docker-compose up` e `mvn spring-boot:run` sem nenhuma configuração adicional.

### Controle de execução do scheduler

O `MonitorScheduler` é anotado com `@ConditionalOnProperty`. Isso permite ligar ou desligar o robô de monitoramento através de uma simples propriedade no `application.properties`:
```properties
jade.scheduler.enabled=true
```
Essa chave é útil para:
-   **Desenvolvimento:** testar endpoints da API sem que o console seja poluído com logs do scheduler.
-   **Testes:** evitar que o banco de dados de teste seja preenchido com dados de monitoramento desnecessários.

### Uso de `OffsetDateTime` para timezones

Todos os campos de data e hora da aplicação utilizam `OffsetDateTime` com UTC
(`ZoneOffset.UTC`), garantindo comportamento previsível em qualquer ambiente de
nuvem independente do fuso horário do servidor.

No banco de dados, todos os campos de data são declarados como `TIMESTAMPTZ`
(timestamp with time zone), o tipo nativo do PostgreSQL que preserva a informação
de fuso horário. Isso é definido no script de migração inicial (V1) e garante que
a comparação de datas na query do `MonitorScheduler` (`CURRENT_TIMESTAMP`) seja
sempre consistente com os valores armazenados.

> **Nota histórica:** o banco de dados de produção (Neon) foi criado manualmente
> antes do Flyway ser integrado ao projeto, utilizando `timestamp without time zone`
> nos campos de data. Essa inconsistência foi corrigida pela migração `V2`, que
> converteu todas as colunas para `TIMESTAMPTZ` via `ALTER COLUMN ... TYPE TIMESTAMPTZ
> USING ... AT TIME ZONE 'UTC'`. O banco local via Docker sempre utilizou o tipo
> correto, pois nasceu com o Flyway ativo.

### Estrutura de migrações Flyway

As migrações seguem o padrão de versionamento `V{n}__{descricao}.sql` e ficam
em `src/main/resources/db/migration`. Cada arquivo contém apenas o delta:
exclusivamente o que muda naquela versão, nunca uma cópia do *schema* anterior.

| Versão | Arquivo                           | Descrição                                                                                                                            |
|--------|-----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| V1     | `V1__Create_initial_schema.sql`   | Criação de todas as tabelas com `TIMESTAMPTZ`, `BIGSERIAL` e constraints de FK com `ON DELETE CASCADE`                               |
| V2     | `V2__Fix_timezone_columns.sql`    | Correção das colunas do banco de produção (criado manualmente) de `timestamp without time zone` para `TIMESTAMPTZ`                   |
| V3     | `V3__Add_performance_indexes.sql` | Índices de performance em `monitor_history(monitor_id, checked_at DESC)`, `incidents(monitor_id, status)` e `monitors(last_checked)` |

O `baseline-on-migrate=true` no `application.properties` instrui o Flyway a
marcar o V1 como já aplicado caso encontre um banco existente sem histórico de
migrações, evitando que ele tente recriar tabelas que já existem.

### Transações explícitas e rejeição do *Open Session in View*

O Spring Boot habilita por padrão o padrão **Open Session in View (OSIV)**, que
mantém a sessão do Hibernate aberta durante todo o ciclo de vida de uma requisição
HTTP. Isso permite que relacionamentos `FetchType.LAZY` sejam carregados em qualquer
ponto — inclusive nos controllers — sem lançar `LazyInitializationException`.

O OSIV foi mantido ativo para não quebrar comportamentos existentes, mas a aplicação
**não depende dele** para funcionar corretamente. Todos os acessos a relacionamentos
lazy são feitos explicitamente dentro de métodos `@Transactional` no service, garantindo
que o comportamento seja previsível mesmo em contextos sem requisição HTTP — como o
`MonitorScheduler`, que roda em background e nunca passa pelo ciclo de vida HTTP.

O exemplo mais concreto dessa decisão é o método `findOwnerIdByMonitorId` no
`MonitorService`:
```java
@Transactional(readOnly = true)
public Long findOwnerIdByMonitorId(Long monitorId) {
    Monitor monitor = monitorRepository.findById(monitorId)
        .orElseThrow(() -> new ResourceNotFoundException("Monitor não encontrado"));
    return monitor.getUser().getId(); //acesso lazy dentro da transação
}
```

Sem esse método, os controllers de `DELETE` e `PUT /monitors/{id}` acessariam
`monitor.getUser().getId()` fora de qualquer transação, dependendo silenciosamente
do OSIV para não quebrar. O método centraliza o acesso no service, onde a transação
é explícita e controlada.
