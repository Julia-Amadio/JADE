# JADE
O JADE é uma aplicação desenvolvida para monitoramento de disponibilidade de serviços web (health checks). O objetivo é permitir o cadastro de URLs críticas e oferecer feedback imediato (Status UP/DOWN) e histórico de latência. Atualmente, o projeto encontra-se na fase de implementação das regras de negócio e persistência de dados no Back-End.

# Arquitetura e decisões técnicas (fase inicial)
O projeto é construído sobre a arquitetura MVC/Monolítica utilizando Java 21 e Spring Boot 3, priorizando uma configuração robusta e segura desde o início:
- **Gerenciamento de dependências:** utiliza Maven para orquestração de bibliotecas e build, garantindo reprodutibilidade do ambiente. 
- **Banco de dados:** persistência relacional com PostgreSQL, hospedado na plataforma Serverless [Neon.tech](https://neon.com/). A conexão utiliza o driver oficial do Postgres. 
- **ORM e validação:** o mapeamento objeto-relacional é feito via Spring Data JPA (Hibernate). 
- **Estratégia de Schema:** configurou-se ``hibernate.ddl-auto=validate``. O Java valida se as Entities (``@Entity``) correspondem exatamente às tabelas criadas no banco, garantindo integridade sem alterar a estrutura automaticamente. 
- **Boilerplate:** uso da biblioteca Lombok (``@Data``, ``@NoArgsConstructor``) para redução de verbosidade e limpeza do código fonte durante a compilação. 
- **Segurança de configuração:** credenciais sensíveis (senhas de banco, URLs) são injetadas via variáveis de ambiente no IntelliJ, não constando hardcoded no ``application.properties``, e protegidas via .gitignore. 
- **Versionamento de banco:** a ferramenta Flyway foi incluída nas dependências para futura migração automatizada, mas mantida inativa (``enabled=false``) nesta fase inicial de modelagem manual.

# Stack
1. **Java (JDK 21):** linguagem base de todo o backend. Responsável pela lógica de negócio, processamento de dados e execução do servidor.
2. **Spring Boot:** atua como "esqueleto" da aplicação. Gerencia o servidor web embutido (Tomcat), a injeção de dependências, a leitura de configurações (``application.properties``) e a integração entre o código Java e o BD.
3. **Maven:** ferramenta de automação de compilação e gerenciamento de dependências. Lê o ``pom.xml``, baixa automaticamente as bibliotecas necessárias, compila o código fonte e empacota tudo para execução.
4. **PostgreSQL:** memória persistente do sistema. Armazena as tabelas de Usuários, Monitores, Histórico de Logs e Incidentes.
5. **Neon:** provedor de hospedagem do banco de dados. Permite que a aplicação (local ou em deploy) acesse os dados via internet sem a necessidade de instalar e configurar um servidor PostgreSQL localmente na máquina do desenvolvedor.
6. **Spring Data JPA/Hibernate:** transforma as Classes Java (Entities) em tabelas do banco e traduz os comandos Java (como ``.save()`` ou ``.findAll()``) para comandos SQL (``INSERT``, ``SELECT``), evitando necessidade de escrever SQL puro manualmente.
7. **Lombok:** biblioteca Java que se conecta ao editor e ao compilador para automatizar a geração de código repetitivo (boilerplate), mantendo ele limpo. Através de anotações (como ``@Data``), ele cria automaticamente getters, setters, construtores e métodos ``toString()`` em tempo de compilação, reduzindo drasticamente o tamanho dos arquivos de classe.
8. **Flyway:** configurado para garantir a integridade futura. Atualmente inativo (``enabled=false``) enquanto o banco é moldado manualmente (por meio do SQL Editor do Neon), será responsável por aplicar scripts SQL de migração automaticamente quando o projeto for para produção.
9. **IntelliJ IDEA (ambiente):** IDE para Java. Além de editar o código, gerencia a estrutura de pastas e, crucialmente, simula o ambiente de produção injetando variáveis de ambiente (senhas e credenciais) de forma segura durante os testes locais.