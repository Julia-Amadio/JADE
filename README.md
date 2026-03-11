# JADE - Just Another Downtime Examiner

O JADE é uma aplicação de monitoramento de disponibilidade de serviços web (health checks). Seu objetivo é permitir o cadastro de URLs críticas e oferecer feedback imediato (status UP/DOWN), histórico de latência e gestão de incidentes.

O backend está totalmente implementado e o frontend está em desenvolvimento utilizando React + Vite.

---

## Funcionalidades

*   **Monitoramento de uptime:** verifica automaticamente os serviços web cadastrados em intervalos definidos pelo usuário.
*   **Histórico de latência:** armazena e exibe o tempo de resposta para cada verificação.
*   **Gestão de incidentes:** agrupa falhas consecutivas de forma inteligente em um único incidente, resolvendo-o automaticamente quando o serviço volta a operar.
*   **Gerenciamento de usuários:** sistema seguro de registro e autenticação.
*   **Controle de Acesso Baseado em Papéis (RBAC):** permissões diferenciadas para usuários comuns e administradores.
*   **API REST:** API projetada para integração e comunicação com o frontend.

---

## Tech stack

| Categoria               | Tecnologia                                                                                                                                                             |
|-------------------------| ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Backend**             | [Java 21](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html) + [Spring Boot 3](https://spring.io/projects/spring-boot)                         |
| **Frontend**            | [React](https://react.dev/) + [Vite](https://vitejs.dev/)                                                                                                              |
| **Banco de dados**      | [PostgreSQL](https://www.postgresql.org/) (hospedado em [Neon.tech](https://neon.tech/))                                                                                    |
| **ORM**                 | [Spring Data JPA](https://spring.io/projects/spring-data-jpa) (Hibernate)                                                                                              |
| **Migração de BD**      | [Flyway](https://flywaydb.org/)                                                                                                                                        |
| **Autenticação**        | [Spring Security](https://spring.io/projects/spring-security) + [JWT](https://jwt.io/)                                                                                 |
| **Build tool**          | [Maven](https://maven.apache.org/)                                                                                                                                     |
| **Qualidade de código** | [Lombok](https://projectlombok.org/)                                                                                                                                   |

---

## Arquitetura

O projeto é construído sobre uma arquitetura monolítica utilizando Java e Spring Boot. Os diagramas abaixo ilustram o fluxo da aplicação, desde a compilação até a execução.

### *Build time* (tempo de compilação)
O processo de build utiliza processamento de anotações para reduzir código repetitivo. O Maven resolve as dependências e invoca o compilador Java. O Lombok intercepta a Árvore de Sintaxe Abstrata (AST) do compilador para injetar métodos como getters, setters e construtores diretamente antes da geração do bytecode final.

![Diagrama de fluxo do build time](https://i.postimg.cc/s2wj6XYj/JADE-(build-time).png)

### *Runtime* (tempo de execução)
Ao iniciar a aplicação, o Spring Boot orquestra a inicialização em ordem de dependência:
1.  **Configuração:** lê o `application.properties` e injeta variáveis de ambiente.
2.  **Migração de Banco de Dados (Flyway):** verifica a versão do banco e aplica scripts SQL pendentes.
3.  **Validação JPA (Hibernate):** valida se as classes `@Entity` correspondem às tabelas reais do banco.
4.  **Servidor web:** o servidor Tomcat embutido inicia e abre a porta 8080 para receber requisições.

![Diagrama de fluxo do runtime](https://i.postimg.cc/wB0qfvXd/JADE-(runtime).png)

---

## Controle de Acesso Baseado em Papéis (RBAC)

A API implementa um sistema robusto de controle de acesso baseado nos papéis dos usuários, garantindo que eles possam acessar apenas seus próprios recursos.

*   **`ROLE_USER` (padrão)**
    *   Pode se registrar, fazer login e gerenciar seu próprio perfil.
    *   Pode criar, visualizar, atualizar e deletar seus próprios monitores.
    *   Pode visualizar o histórico e os incidentes associados aos seus monitores.
    *   **Não pode** visualizar ou modificar dados de outros usuários.

*   **`ROLE_ADMIN`**
    *   Possui todas as permissões de um `ROLE_USER`.
    *   Pode visualizar uma lista de todos os usuários no sistema.
    *   Pode visualizar uma lista de todos os monitores de todos os usuários.
    *   Pode deletar a conta de qualquer usuário.
    *   *As verificações de propriedade são ignoradas para fins de visualização administrativa.*

---

## Como executar

Para executar este projeto localmente, você precisará de:

*   Java (JDK 21)
*   Maven
*   Um banco de dados PostgreSQL (você pode usar uma instância local ou um provedor em nuvem gratuito como o [Neon](https://neon.tech/))
*   Node.js e npm (para o frontend)

### Setup do backend

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/Julia-Amadio/JADE.git
    cd JADE/backend
    ```

2.  **Configure as variáveis de ambiente:**
    A aplicação usa variáveis de ambiente para dados sensíveis. Na sua IDE (como o IntelliJ), crie uma configuração de execução e defina as seguintes variáveis:
    *   `DB_URL`: a URL JDBC para seu banco de dados PostgreSQL (ex: `jdbc:postgresql://...`)
    *   `DB_USER`: seu nome de usuário do banco de dados.
    *   `DB_PASSWORD`: sua senha do banco de dados.

3.  **Habilite o agendador (opcional):**
    O agendador de monitoramento é desabilitado por padrão para facilitar o desenvolvimento. Para habilitá-lo, adicione a seguinte linha em `src/main/resources/application.properties`:
    ```properties
    jade.scheduler.enabled=true
    ```

4.  **Execute a aplicação:**
    As migrações do Flyway serão executadas automaticamente na primeira inicialização, criando as tabelas necessárias.
    ```bash
    mvn spring-boot:run
    ```
    A API estará disponível em `http://localhost:8080`.

### Setup do frontend

1.  **Navegue até o diretório do frontend:**
    ```bash
    cd ../frontend
    ```

2.  **Instale as dependências:**
    ```bash
    npm install
    ```

3.  **Execute o servidor de desenvolvimento:**
    ```bash
    npm run dev
    ```
    A aplicação React estará disponível em `http://localhost:5173`.

---

## Visão geral dos endpoints da API

A API fornece os seguintes recursos principais:

*   **Autenticação:**
    *   `POST /auth/login`: autentica um usuário e retorna um JWT.
*   **Usuários:**
    *   `POST /users`: registra um novo usuário.
    *   `GET /users`: lista todos os usuários (somente Admin).
    *   `GET /users/{id}`: obtém detalhes de um usuário.
    *   `PUT /users/{id}`: atualiza um usuário.
    *   `DELETE /users/{id}`: deleta um usuário.
*   **Monitores:**
    *   `POST /monitors`: cria um novo monitor.
    *   `GET /monitors`: lista todos os monitores do usuário logado (ou todos para Admin).
    *   `GET /monitors/{id}`: obtém detalhes de um monitor.
    *   `PUT /monitors/{id}`: atualiza um monitor.
    *   `DELETE /monitors/{id}`: deleta um monitor.
*   **Histórico e incidentes:**
    *   `GET /monitors/{monitorId}/history`: obtém o histórico paginado de um monitor.
    *   `GET /monitors/{monitorId}/incidents`: obtém os incidentes de um monitor.

*Esta é uma visão simplificada. Por favor, consulte a camada `controllers` no código-fonte para formatos detalhados de requisição/resposta.*
