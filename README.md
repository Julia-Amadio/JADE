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

Para facilitar a avaliação do projeto, o backend está configurado para rodar em um banco de dados local isolado via Docker, sem a necessidade de configurar variáveis de ambiente ou credenciais na nuvem.

**Pré-requisitos:**
*   [Java (JDK 21)](https://www.oracle.com/br/java/technologies/downloads/#java21);
*   [Docker Desktop](https://www.docker.com/products/docker-desktop) (o mesmo precisa já estar rodando na máquina antes da inicialização do *backend*);
*   [Node.js](https://nodejs.org/en/download) e npm (para o *frontend*).

Não é necessário ter o Maven instalado, usaremos o *Wrapper* incluído no projeto.

### 1) Setup do backend
1.  **Clone o repositório e navegue até a pasta do backend:**
    ```bash
    git clone https://github.com/Julia-Amadio/JADE.git
    cd JADE/backend
    ```
2.  **Inicie o Banco de Dados local (PostgreSQL):** 

    Suba o contêiner do banco de dados na porta 5433.
    ```bash
    docker-compose up -d
    ```
3.  **Execute a aplicação Spring Boot:** 

    Utilize o Maven Wrapper para baixar as dependências, compilar e rodar o projeto. O Flyway detectará o banco zerado e criará as tabelas automaticamente.

    *No Windows:*
    ```bash
    .\mvnw spring-boot:run
    ```
    *No Linux/Mac:*
    ```bash
    ./mvnw spring-boot:run
    ```
4.  **Explore a API no Postman/Insomnia:**

    Assim que a aplicação iniciar (*porta 8080*), o script `DataLoader.java` injetará automaticamente um Usuário Admin e 4 Monitores "Fantoches". O motor de agendamento em background já começará a realizar os pings e gerar históricos de latência.

### 2) Setup do frontend

1.  **Navegue até o diretório do frontend:**
    ```bash
    cd ../frontend
    ```

2.  **Instale as dependências:**
    ```bash
    npm install
    ```

3.  **Inicie o servidor de desenvolvimento:**
    ```bash
    npm run dev
    ```
    A aplicação React estará disponível em `http://localhost:5173`.

### 3) Encerrando a aplicação

Quando terminar de testar, você pode derrubar os serviços da seguinte forma:

1. **Frontend e Backend:**

   Pressione `Ctrl + C` nos terminais onde as aplicações estão rodando.

3. **Banco de Dados:**

   No terminal, dentro da pasta `backend`, execute o comando abaixo para parar e remover o contêiner do Docker:
   ```bash
   docker-compose down
   ```
