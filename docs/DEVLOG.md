# Estrutura inicial do Banco de Dados
1. Tabela **users** (quem acessa). Aqui ficam os dados de login.
   - **id:** (PK) número único ou UUID;
   - **username:** nome de login;
   - **email:** para contato;
   - **pswd_hash:** senha criptografada (nunca salvar senha pura, usar BCrypt);
   - **created_at:** data de cadastro.
2. Tabela **monitors** (o que será vigiado). Aqui ficam as URLs que o usuário cadastrou.
   - **id:** (PK);
   - **user_id:** (FK) ATENÇAO: esta coluna diz "a quem este monitor pertence". Ela aponta para o id da tabela users;
   - **name:** apelido (ex: "API de Produção");
   - **url:** o endereço monitorado;
   - **interval_seconds:** de quanto em quanto tempo checar (ex: 300 segundos);
   - **is_active:** boolean para pausar o monitoramento sem deletar.
3. Tabela **monitor_history** (logs/pings). Aqui o volume de dados cresce. Cada vez que o script rodar e fizer um ping, ele salva uma linha aqui.
   - **id:** (PK);
   - **monitor_id:** (FK) aponta para a tabela monitors;
   - **status_code:** resultado HTTP (200, 404, 500);
   - **latency_ms:** quanto tempo demorou para a resposta (ex: 120ms);
   - **checked_at:** data e hora exata da verificação;
   - **is_successful:** Boolean (facilitador para consultas rápidas de uptime).
4. Tabela **incidents** (registro manual de problemas). Para a funcionalidade de postmortem.
   - **id:** (PK);
   - **user_id:** (FK) quem reportou;
   - **title:** título do problema;
   - **severity:** (enum ou Varchar) 'LOW', 'MEDIUM', 'HIGH', 'CRITICAL';
   - **description:** texto longo;
   - **status:** 'OPEN', 'RESOLVED'.

A estrutura foi hospedada no [neon.tech](https://neon.com/) (Postgre) por meio da execução do seguinte script:
```SQL
-- 1. Tabela de usuários
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    pswd_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Tabela de monitores
CREATE TABLE monitors (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    url VARCHAR(255) NOT NULL,
    interval_seconds INT DEFAULT 300,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela de histórico (logs de uptime)
CREATE TABLE monitor_history (
    id SERIAL PRIMARY KEY,
    monitor_id INT NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    status_code INT,
    latency_ms INT,
    is_successful BOOLEAN,
    checked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Tabela de incidentes
CREATE TABLE incidents (
    id SERIAL PRIMARY KEY,
    monitor_id INT NOT NULL REFERENCES monitors(id) ON DELETE CASCADE,
    title VARCHAR(150) NOT NULL,
    severity VARCHAR(20) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'OPEN',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

# <br>06/01 - Camada de Modelo (``com.jadeproject.backend.model``)
O pacote ``model`` constitui a camada de domínio da aplicação. Contém as entidades JPA (Java Persistence API), que são classes Java responsáveis por representar a estrutura de dados do sistema e suas regras de integridade. Estas classes desempenham dois papéis fundamentais na arquitetura:
- **Mapeamento Objeto-Relacional (ORM):** através de anotações (como ``@Entity``, ``@Table``, ``@Column``), estas classes definem como o banco de dados deve ser estruturado. O framework utiliza esses "modelos" para criar e validar automaticamente as tabelas, colunas e chaves estrangeiras no PostgreSQL.
- **Representação de estado:** durante a execução da aplicação (runtime), estas classes são instanciadas para manipular os dados na memória RAM, transportando informações entre o BD e as camadas de regra de negócio.

Em resumo, o diretório model é a fonte da verdade sobre a estrutura dos dados, servindo tanto como espelho para a criação das tabelas no banco quanto como molde para os objetos manipulados pela aplicação.

# <br>08/01 - Camada Repository (``com.jadeproject.backend.repository``)
O Java fala em objetos, classes, variáveis, herança. O BD fala em tabelas, linhas, colunas, SQL, FKs. O Repository é um agente intermediário inteligente que traduz operações Java automaticamente para a língua do banco (INSERT, SELECT, DELETE), executa e devolve o resultado já convertido para objetos Java, sem que seja necessário abrir qualquer conexão manual. Podemos pensar nele como um bibliotecário da aplicação: o código Java não “entra no estoque” (BD) para caçar onde o livro deve ser guardado -- ao invés disso, o código Java “vai até o balcão” e diz ao bibliotecário:
- “Guarde este Usuário para mim.” (``.save(user)``)
- “Me dê o Usuário com ID 1.” (``.findById(1)``)
- “Apague este monitor.” (``.delete(monitor)``)
```java
public interface UserRepository extends JpaRepository<User, Long>{...}
```
Usamos a palavra ``interface`` e não ``class``. O corpo também é bem pequeno. Basicamente, estamos dizendo ao Spring: “quero um bibliotecário que saiba lidar com a entidade User, que tem ID Long. Não quero programar como ele faz o trabalho sujo de SQL, então faça isto para mim.”

Ao estender ``JpaRepository``, o Spring automaticamente fornece dezenas de métodos prontos sem precisar escrever uma linha de código sequer. Ele também fornece métodos personalizados: escrevendo apenas a assinatura ``findByEmail(String email)``, o Spring lê o nome do método em Inglês e entende que queremos ``SELECT * FROM users WHERE email = ?``, criando o código em tempo de execução.

Em resumo, o Repository serve para isolar o código Java da complexidade do SQL, transformando o BD em métodos simples de Java.

# <br>09/01 - Testes
## 1. SMOKE TEST (“teste de fumaça”)
Simplesmente rodar a aplicação (``public class JadeprojectBackendApplication``). Isso testa:
- Se o Spring consegue se conectar ao banco de dados.
- Se as anotações (``@Entity``, ``@Column``) estão corretas.
- Se o Hibernate consegue criar as tabelas no banco.

**Resultado esperado:** console deve terminar com uma mensagem parecida com ``Started JadeProjectApplication in X seconds``, não exibindo Exception ou quaisquer outros avisos de erro.

## 2. Alterações no banco
O console exibiu o seguinte erro:
```diff
- 2026-01-09T12:57:35.130-03:00 ERROR 36756 --- [JADE] [main] j.LocalContainerEntityManagerFactoryBean : Failed to initialize JPA EntityManagerFactory: 
! Unable to build Hibernate SessionFactory  [persistence unit: default] ; nested exception is org.hibernate.tool.schema.spi.SchemaManagementException: Schema validation: wrong column type encountered in column [id] in table [incidents]; found [serial (Types#INTEGER)], but expecting [bigint (Types#BIGINT)]
```
No Java, ``id`` tinha sido inicialmente declarado como ``private Long``; para o Hibernate, ``Long`` é um número de 64 bits (BIGINT). Enquanto isso, no banco, as tabelas foram criadas usando ``id SERIAL PRIMARY KEY``; no Postgres, ``SERIAL`` cria um número de 32 bits (INTEGER).

Devido a esta incongruência, o tipo das colunas ``id`` foram alteradas para ``BIGINT`` dentro do banco. Como ``SERIAL`` foi usado em todas as tabelas, o erro aparece para as outras por mais que a tabela ``incidents`` seja consertada. Por meio do SQL Editor do Neon, foi rodado o seguinte script:
```SQL
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
ALTER TABLE monitors ALTER COLUMN id TYPE BIGINT;
ALTER TABLE monitor_history ALTER COLUMN id TYPE BIGINT;
ALTER TABLE incidents ALTER COLUMN id TYPE BIGINT;
```
Mesmo com o script acima, o erro persistiu, porém apontando disparidade para a coluna ``monitor_id``. Isso se deve ao fato de que as chaves estrangeiras (FKs) não foram atualizadas e precisam ter o mesmo tipo da chave primária que apontam. Foi executado outro script:
```SQL
ALTER TABLE incidents ALTER COLUMN monitor_id TYPE BIGINT;
ALTER TABLE monitor_history ALTER COLUMN monitor_id TYPE BIGINT;
ALTER TABLE monitors ALTER COLUMN user_id TYPE BIGINT;
```

## 3. Teste de carga inicial ([6398df3](https://github.com/Julia-Amadio/JADE/commit/6398df3ff27e1797e3afc5599fcace5704d3b8b0))
Envolve a criação da classe temporária chamada ``DataLoader.java``, que é inserida no pacote raiz e roda assim que o sistema liga. Ela vai tentar salvar um usuário e mostrar no console, provando que as camadas estão conversando perfeitamente. Inicialmente, ela está elaborada para funcionar sem a camada Service.

# <br>10/01 - Camada Service (``com.jadeproject.backend.service`` - [ec1f925](https://github.com/Julia-Amadio/JADE/commit/ec1f9253652233a1ccf814f787268e89eb9ca1f2))
A camada Service serve para garantir o princípio do SOC (*Separation of Concerns*, ou separação de responsabilidades). Suas funções principais incluem:
- **Centralização da lógica de negócio:** o Service impede que regras importantes fiquem espalhadas por "Controllers" (que devem apenas rotear requisições) ou "Repositories" (que devem apenas buscar dados).
- **Orquestração de dados:** muitas vezes, uma única ação do usuário exige consultar várias tabelas diferentes. O Service atua como um maestro, chamando múltiplos repositórios e compilando a resposta final.
- **Garantia de integridade (transações):** é responsabilidade do Service assegurar que uma operação seja atômica. Se um erro ocorrer no meio do processo, o Service impede que dados incompletos sejam salvos.
- **Reutilização de código:** como a lógica está centralizada, diferentes partes do sistema (API, Web, CLI, Scheduler) podem chamar o mesmo método do Service sem duplicar código.

No contexto do sistema de monitoramento desenvolvido, a camada Service foi fundamental para implementar regras que não poderiam ser resolvidas apenas com SQL simples:
- No ``MonitorService``: implementou-se a regra de escopo onde nomes de monitores devem ser únicos apenas dentro da conta do mesmo usuário, permitindo repetição entre usuários diferentes. Além disso, orquestra a validação da existência do usuário antes de vincular um novo monitor.
- No ``UserService:`` garante a unicidade dos dados, impedindo o cadastro de e-mails ou nomes de usuário duplicados antes mesmo de tentar a inserção no banco de dados.
- No ``MonitorHistoryService:`` responsável pela persistência de métricas de alta frequência. Fornece a interface para o futuro scheduler registrar as verificações (log de latência e status) e disponibiliza consultas otimizadas para alimentar os gráficos de desempenho no dashboard do usuário.
- No ``IncidentService:`` atua como uma máquina de estado, decidindo inteligentemente se um evento de falha deve gerar um novo incidente (abrir chamado) ou se deve ser ignorado pois já existe um incidente em aberto.

## Alteração na tabela ``incident``
Durante a construção do ``IncidentService``, constatou-se que em uma situação onde um site volta ao normal, e o incidente é “fechado”, é necessário que o banco registre também a data e horário do fechamento. Por isso, a tabela ``incident`` ganhou uma nova coluna por meio da execução do seguinte comando SQL:
```SQL
ALTER TABLE incidents ADD COLUMN ended_at TIMESTAMP;
```
Diferente de ``created_at``, ``ended_at`` não tem um default ou valor a ser auto-preenchido, pois deve ser considerada como nula até que o incidente em aberto seja resolvido de fato. Além disso, foi necessária alteração na classe ``com.jadeproject.backend.model.Incident`` para a inclusão desta nova coluna, conforme mostrado [neste commit](https://github.com/Julia-Amadio/JADE/commit/40fa2b2c42ce315b05f9b56ad6a94352b209f344).

## Atualizações no ``DataLoader.java`` ([be8d96f](https://github.com/Julia-Amadio/JADE/commit/be8d96fcbcb9a45811a84267caba8650035a56a5))
O teste de carga de dados foi reestruturado para que fizesse uso da camada de Serviços. Em sequência, ele realiza:
- A criação de um Usuário;
- A criação de um Monitor, pertencente ao Usuário teste;
- A criação de quatro logs para o Monitor, com três sucessos e uma falha;
- Simulação da “queda” do monitor, emitindo um Incidente.
    - Inicialmente, o Incidente é registrado com status ``OPEN`` e, após isso, a thread de execução é pausada por trinta segundos (``Thread.sleep(30000);``) para verificação visual do status dentro do banco. 
    - Após o intervalo, o status do Incidente é alterado para ``RESOLVED``, simulando a retomada do funcionamento do monitor.

# <br>13/01 - Camada Controller (``com.jadeproject.backend.controller``)
( [User](https://github.com/Julia-Amadio/JADE/commit/417ef83cb299e2a83029851d345ea0a5095063b6) • [Monitor](https://github.com/Julia-Amadio/JADE/commit/f3983c6c8a4606ab18776f3ad31fca1a3d75ec59) • [MonitorHistory](https://github.com/Julia-Amadio/JADE/commit/6393ec9781379dd24843a10c57f9818e5742ccb7) • [Incident](https://github.com/Julia-Amadio/JADE/commit/848e6e0d97a732d492890c2fffcbd3fd11eba69a) )

A camada Controller é o componente responsável por expor as funcionalidades do sistema para o mundo externo. No contexto de uma API REST (como o JADE), ela atua como um intermediário que traduz as requisições HTTP (vindas da Web) para comandos Java que o sistema entende.

O funcionamento técnico segue este ciclo para cada ação (ex: criar monitor):
1. **Recepção (listening):** o Controller fica "ouvindo" endereços específicos (Endpoints), como POST /monitors.
2. **Deserialização:** quando uma requisição chega, o Controller pega os dados (geralmente em formato JSON) e os converte para Objetos Java (ex: transforma ``{ "name": "Google" }`` em ``new Monitor()``).
3. **Delegação:** o Controller não toma decisões complexas. Ele repassa o objeto para a camada Service, que contém a inteligência do negócio.
4. **Resposta (serialization):**
    - Se o Service processar com sucesso, o Controller recebe o resultado, empacota em um JSON e devolve com Status ``200 OK``.
    - Se o Service lançar um erro, o Controller captura esse erro e devolve uma mensagem apropriada com Status ``400 Bad Request`` ou ``500 Internal Error``.

# <br>27/01 - Scheduler ([6a1955a](https://github.com/Julia-Amadio/JADE/commit/6a1955a5fb1b4546b9f1119e0397877f03f4cbea))
O Scheduler é o coração do JADE. Sem ele, o sistema é apenas um cadastro de links. No Spring Boot, a forma mais nativa de fazer tarefas repetitivas é usando a anotação ``@Scheduled``. Foram traçados três passos simples para o mínimo produto viável:
1. **Habilitar o agendamento:** avisar o Spring que ele deve procurar tarefas agendadas. Isso foi feito por meio da adição da anotação ``@EnableScheduling`` na classe principal;
2. **Criar o serviço agendador:** a classe MonitorScheduler que acorda a cada X segundos. Por enquanto, ele roda a cada 1 minuto (60000 ms) e apenas imprime no console se o site está UP ou DOWN;
3. **Lógica do ping:** o código que bate na URL e verifica se é ``200 OK``.

## ChaosController
Visando a condução de testes mais próximos da realidade, foi criado o ``ChaosController.java`` com endpoints que simulam falhas sistêmicas:
- /up: retorna HTTP ``200 (OK)`` consistentemente;
- /down: simula falha interna retornando HTTP ``500 (Internal Server Error)``;
- /slow: simula latência alta. O endpoint aguarda 5s ``(Thread.sleep)``, forçando o MonitorScheduler a disparar um ``SocketTimeoutException``, já que seu limite de leitura é de apenas 3s;
- /random: alterna aleatoriamente entre status de sucesso e erro para testar a intermitência. 

O ``DataLoader.java`` também foi refatorado de forma que estes endpoints sejam inseridos na tabela ``monitors`` de um usuário existente.

# <br>03/02 - Ajustes no scheduler + lógica de registro dos logs no banco
Alterações:
1. [Ajustes](https://github.com/Julia-Amadio/JADE/commit/572a898e888269cfc8b8ca1414f8cdf7b21b2273) para que o console exiba o HTTP Status Code nas baterias de testes.
2. [Conexão do scheduler ao BD](https://github.com/Julia-Amadio/JADE/commit/e93357636bcc867f87452e0c26ae0a6b98786191) para que os logs sejam salvos no histórico, além de atualização do método ``saveLog`` no ``MonitorHistoryService.java`` para que ele salve valores na coluna ``is_successful`` no BD.

# <br>04/02 - Gestão de incidentes ([603f786](https://github.com/Julia-Amadio/JADE/commit/603f78667af74bb0720ffad54555a9198d51d138)) e ajustes para o MonitorHistory ([4c3ba4c](https://github.com/Julia-Amadio/JADE/commit/4c3ba4ccf33e54d48a21f169624aaf79d1eab800))
Com o registro de logs pronto, o próximo passo lógico é dar inteligência ao monitoramento. Se um monitor onde um ping é lançado a cada minuto possui 60 logs de DOWN, não temos 60 problemas, mas sim um problema que durou 1 hora -- assim, temos um INCIDENTE.

Foi implementada a seguinte lógica no scheduler:
1. **UP --> DOWN:** a URL monitorada caiu. O sistema verificará **se já existe um incidente aberto** para esse monitor. Se não, ABRIR INCIDENTE (``Status: OPEN``).
2. **DOWN --> DOWN:** a URL continua sem fornecer resposta, o incidente continua OPEN.
3. **DOWN --> UP:** a URL forneceu resposta. O sistema verificará **se existe um incidente aberto**. Se sim, FECHAR INCIDENTE (``Status: RESOLVED``).

## Atualizações no ``MonitorHistoryRepository`` e ``MonitorHistoryService``
O método de busca dos últimos 10 logs de um monitor foi atualizado para que utilize paginação. Quando é feita a passagem do objeto ``Pageable`` para o repositório, o Spring Data intercepta a chamada, entendendo que não queremos a lista toda e que deve reescrever a consulta SQL automaticamente.

No ``Service``, foi criado o seguinte objeto:
```java
PageRequest.of(0, 10, Sort.by("checkedAt").descending());
```
Isso traduz para:
- 0: forneça a primeira página.
- 10: forneça pacotes de 10 itens por vez.
- Sort...: forneça os mais recentes primeiro (decrescente).

*Além das duas alterações citadas anteriormente, foi removida uma pequena redundância em relação ao ``boolean isUp`` dentro do ``MonitorHistoryRepository``.*

# <br>06/02 - Agendamento dinâmico ([2f0f900](https://github.com/Julia-Amadio/JADE/commit/2f0f900788534735069dd6b965810fa14fd433c5))
Até o momento, o sistema tratava todos os monitores de forma igual ou dependia de uma verificação pesada no histórico. Para permitir que cada monitor tenha seu próprio intervalo de verificação (ex: um crítico a cada 30s, outro a cada 5 min) de forma performática, foi implementada uma estratégia de **"Smart Polling"**.

A ideia central é evitar trazer todos os monitores para a memória ou fazer buscas complexas na tabela gigante de histórico. O próprio registro do monitor deve saber quando foi sua última verificação.

## 1. Alterações na estrutura (BD e ``Model``)
Foi criada uma coluna de "cache" na tabela principal. Isso permite consultar rapidamente quem está "atrasado" sem precisar fazer joins ou aggregations na tabela de logs.

SQL executado (migração manual):
```SQL
ALTER TABLE monitors ADD COLUMN last_checked TIMESTAMP;
```
No Model (``Monitor.java``):
```java
@Column(name = "last_checked")
private OffsetDateTime lastChecked;
```

## 2. Query inteligente (``Repository``)
Ao invés de verificar datas no Java, delegamos o filtro para o Banco de Dados. No ``MonitorRepository``, foi criada uma *Native Query* que utiliza funções de tempo do PostgreSQL para calcular o próximo disparo de ping.
A query retorna apenas os monitores onde:
1. Nunca rodaram (``last_checked IS NULL``); OU
2. O tempo atual já ultrapassou o agendamento (``last_checked + interval_seconds``).
```java
@Query(value = "SELECT * FROM monitors WHERE last_checked IS NULL OR (last_checked + make_interval(secs => interval_seconds)) < CURRENT_TIMESTAMP", nativeQuery = true)
List<Monitor> findMonitorsToProcess();
```

## 3. Refatoração do Scheduler
O ``MonitorScheduler`` deixou de ser um simples laço cego. O fluxo agora é transacional em relação ao tempo:
1. Busca apenas os monitores pendentes (via query acima);
2. Executa o ping e salva o histórico/incidentes;
3. Atualiza o ``lastChecked`` do monitor para ``OffsetDateTime.now(ZoneOffset.UTC)`` e salva.
Isso garante que ele só será pego pela query novamente quando o intervalo definido pelo usuário passar.

## 4. Regras de negócio (``Service``)
Para evitar abuso do sistema ou problemas de performance (ex: usuário configurando intervalo de 1 segundo), foi estabelecida uma regra de validação no momento da criação do monitor.

No ``MonitorService``, garantimos um hard limit:
```java
if (monitor.getIntervalSeconds() < 30) {
    throw new RuntimeException("O intervalo mínimo é de 30 segundos.");
}
```
O valor padrão (default) permanece 300 segundos (5 minutos) caso o campo venha nulo, conforme estabelecido no ``Model`` e na estrutura do BD.

## 5. Refatoração: migração para UTC (timezone)
Durante a validação da arquitetura para o deploy (futuro uso no Render/AWS), identificou-se uma dívida técnica crítica relacionada ao controle de tempo. O uso de ``LocalDateTime`` atrela a aplicação ao relógio local do servidor.
Em ambientes Cloud (serverless/containers), os servidores geralmente operam em UTC, enquanto o ambiente de desenvolvimento local opera em UTC-3 (Brasília). Isso causaria:
1. **Falha no Scheduler:** a query SQL poderia ignorar monitores ou executá-los em horários errados ao comparar o ``NOW()`` do banco (UTC) com o horário salvo pelo Java (local).
2. **Inconsistência de dados:** incidentes poderiam ter durações negativas ou logs aparecerem no "futuro" dependendo do fuso horário do visualizador.

**A solução:** foi realizada a migração completa do padrão de datas da aplicação para UTC (OffsetDateTime).
1. **Models (``Monitor``, ``MonitorHistory``, ``Incident``, ``User``):** substituição de ``LocalDateTime`` por ``OffsetDateTime``. Agora, toda data salva no banco carrega explicitamente o carimbo Z (Zulu Time/UTC).
2. **Services:** ajuste na criação de objetos para usar ``OffsetDateTime.now(ZoneOffset.UTC)``, garantindo que a aplicação seja a fonte da verdade sobre o fuso, independente da configuração do SO do servidor.
3. **Native query (``MonitorRepository``):** atualização da query do scheduler. Substituição do ``NOW()`` genérico por ``CURRENT_TIMESTAMP``, permitindo que o PostgreSQL (operando com colunas ``TIMESTAMPTZ``) faça a aritmética correta entre o último checagem e o intervalo definido, respeitando o offset de tempo.

# <br>09/02 - Camada DTO e tratamento de exceções ([a8ac872](https://github.com/Julia-Amadio/JADE/commit/a8ac8729e82cd8049ab4c1076c39a5be34d55891))
Foi realizada uma refatoração estrutural separando as responsabilidades de persistência (Entity) das responsabilidades de contrato de API (DTO). Foi implementado também um tratamento de erros centralizado para melhorar a experiência do cliente da API.

## 1. Implementação do padrão DTO (*Data Transfer Object*)
Anteriormente, os Controllers estavam expondo diretamente as Entidades JPA (``User`` e ``Monitor``). Isso trazia riscos de segurança (exposição do hash da senha) e problemas técnicos (referência circular/StackOverflow ao serializar JSON). A solução foi dividir cada modelo em três representações para as entidades User e Monitor, cada um com uma responsabilidade exclusiva no ciclo de vida REST:
- **CreateDTO (input de criação)** 
   - Uso exclusivo para requisições ``POST``.
   - **Responsabilidade:** define os dados mínimos obrigatórios para instanciar um novo registro.
   - **Comportamento:** aplica validações rígidas (``@NotNull``, ``@NotBlank``) para garantir que o objeto não nasça em estado inválido. Não contém campo ID, pois este é gerado pelo banco.
- **UpdateDTO (input de atualização)** 
   - Uso exclusivo para requisições ``PUT`` ou ``PATCH``.
   - **Responsabilidade:** transportar apenas as alterações desejadas.
   - **Comportamento:** possui validações flexíveis. Campos ausentes (null) são interpretados pelo Service como "manter valor original". É mais restritivo que o CreateDTO em termos de escopo (ex: não permite alterar chaves estrangeiras ou datas de auditoria).
- **ResponseDTO (output universal)** 
   - Usado para retorno de requisições ``GET``, ``POST`` e ``PUT``. Não é necessário para ``DELETE``, pois o mesmo não retorna corpo de requisição.
   - **Responsabilidade:** projetar os dados para o cliente de forma segura e formatada.
   - Filtra dados sensíveis presentes na Entidade (ex: remove ``passwordHash`` do Usuário).
   - Mesmo em operações de escrita (``POST/PUT``), o Backend retorna o ResponseDTO para confirmar ao Frontend como o dados foram persistidos no banco (incluindo dados gerados pelo servidor, como ``createdAt`` ou ``id``), permitindo que a interface se atualize instantaneamente sem precisar fazer um novo ``GET``.

## 2. Tratamento de erros (*Global Exception Handler*)
Erros genéricos foram substituídos por respostas HTTP semânticas e informativas:
- **``GlobalExceptionHandler``:** criado com ``@RestControllerAdvice``, intercepta exceções em toda a aplicação e padroniza o JSON de resposta.
- **Validação de campos (``400 Bad Request``):** captura ``MethodArgumentNotValidException`` e retorna um mapa detalhado de qual campo falhou e porquê.
- **Conflitos de dados (``409 Conflict``):** implementação da classe ``DataConflictException``. Tentativas de cadastro de emails ou usernames duplicados retornam status ``409`` com mensagem clara.

## 3. Atualização da lógica de negócio (``service`` & ``controller``)
- Controllers agora atuam como conversores, recebendo DTOs, chamando o Service, e convertendo o resultado para ResponseDTOs.
- Services agora possui verificação preventiva adicional para lançar exceções de negócio antes de tentar o save no banco. Foi também implementada a lógica de *Partial Update*: o Service verifica campo a campo; se o DTO trouxe um valor (não nulo), ele atualiza a entidade. Se trouxe nulo, mantém o valor antigo.

# <br>11/02 - Segurança, atomicidade e padrões REST ([09ae776](https://github.com/Julia-Amadio/JADE/commit/09ae776c0d9f2f1b0ab0749c3f690d95bf827d26))
A principal atualização envolvida é a eliminação do armazenamento de senhas em texto plano. Foi adotado o padrão de indústria BCrypt para hashing.

## Implementação do PasswordEncoder
### 1. **Configuração da infraestrutura** 
Criação do pacote ``config`` e definição da classe ``SecurityConfig``. Por enquanto, o objetivo dela é expor o ``PasswordEncoder`` como um Bean do Spring, permitindo que ele seja injetado em qualquer lugar da aplicação.
### 2. **Integração no serviço (``UserService``)**
No método ``registerUser`` e ``updateUser``, a senha recebida é interceptada antes que a entidade seja persistida. O fluxo agora é:
1. Recebe o DTO com a senha "crua";
2. O ``UserService`` usa o ``passwordEncoder`` injetado;
3. A senha é transformada em um hash irreversível;
4. O hash é salvo no banco.

## Melhorias técnicas
Além da segurança, foi realizado um "pente fino" no código para elevar o nível de maturidade do backend:
- A anotação ``@Transactional`` foi aplicada em métodos de escrita do pacote Service. Isso garante que operações de banco de dados sejam atômicas, com rollback automático em caso de erro. O ``MonitorScheduler`` (robô) foi deixado propositalmente sem transação no nível do método pai para evitar travar conexões de banco durante chamadas de rede (HTTP Requests).
- Foram removidos os amadores ``System.out.println``, adotando o Lombok ``@Slf4j``. Agora temos logs estruturados com níveis (``INFO``, ``ERROR``, ``WARN``) e timestamp automático.
- Foi removida a regex restritiva (``^[a-zA-Z0-9...]``) que impedia caracteres especiais. A validação estava bloqueando senhas fortes geradas por gerenciadores de senha e frustrando usuários. Foram mantidas apenas as regras de complexidade mínima e tamanho.
- O endpoint de criação (``POST /users``) agora retorna explicitamente o status ``201 Created`` ao invés de ``200 OK``. Isso segue a semântica correta do protocolo HTTP para criação de recursos.

# <br>22/02 - Autenticação JWT e controle de acesso ([7941ebb](https://github.com/Julia-Amadio/JADE/commit/7941ebb8e7b7566602d83c78610bec1f3456681b))
Esta atualização representa um marco na maturidade da API. O sistema deixou de ser um ambiente aberto e passou a contar com um controle de acesso robusto baseado em perfis (RBAC - *Role-Based Access Control*), utilizando tokens JWT e o framework Spring Security.

A implementação seguiu um fluxo lógico, desde a alteração estrutural no banco de dados até a blindagem das rotas nos controllers:

## 1. Estrutura de dados (BD e ```Model```)
Para que o sistema saiba diferenciar um usuário comum de um administrador, foi necessária a criação de uma hierarquia de privilégios.
- SQL executado: adição da coluna de papéis na tabela principal.
```SQL
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER';
```
- ```User.java```: a entidade foi atualizada para refletir a nova coluna, garantindo que novos registros nasçam com o privilégio padrão, a menos que especificado o contrário.
```java
@Column(nullable = false, length = 20)
private String role = "ROLE_USER";
```

## 2. Adaptação do Spring Security (autenticação por email)
O Spring Security espera, por padrão, que o login seja feito via "Username". Porém, era desejado que o modelo de negócio do JADE utilizasse o e-mail como credencial principal. Duas classes foram implementadas para traduzir essa regra:
- ```UserDetailsImpl```: implementa a interface ```UserDetails``` do Spring. O método ```getUsername()``` foi sobrescrito para retornar o e-mail do usuário. Esta classe atua como o "crachá" interno do Spring, carregando as permissões e dados do usuário em memória;
- ```AuthorizationService```: implementa ```UserDetailsService```. O método ```loadUserByUsername``` foi adaptado para realizar a busca no banco de dados através do e-mail, empacotando o usuário encontrado no ```UserDetailsImpl```.

## 3. Gerenciamento de tokens e filtros (pacote ```security```)
A arquitetura do projeto foi refatorada, movendo o ```SecurityConfig``` para o novo pacote dedicado ```security```, que agora abriga toda a inteligência de validação:
- ```TokenService```: responsável por gerar e validar os tokens JWT usando a biblioteca do Auth0. O token é assinado com o e-mail (subject) e o papel (role) do usuário, possuindo uma vida útil de 2 horas;
- ```SecurityFilter```: um filtro que intercepta todas as requisições HTTP antes de chegarem aos controllers. Ele extrai o token do cabeçalho ```Authorization```, valida a assinatura no ```TokenService``` e, se válido, monta o contexto de segurança (```SecurityContextHolder```), avisando ao Spring que aquele usuário está autenticado e possui determinadas permissões;
- ```SecurityConfig```: além de abrigar o ```PasswordEncoder```, passou a definir o ```SecurityFilterChain```. Aqui as regras globais de rotas foram estabelecidas (ex: apenas ROLE_ADMIN pode deletar usuários, enquanto endpoints de login e cadastro permanecem públicos).

## 4. Endpoints de login (```AuthController```)
Com a infraestrutura de segurança pronta, foi criado o mecanismo para os usuários obterem suas credenciais:
- Implementação do ```LoginRequestDTO``` (recebe e-mail e senha) e ```LoginResponseDTO``` (devolve os dados do usuário junto com o token JWT gerado);
- Criação do ```AuthController```, expondo a rota ```POST /auth/login```, que realiza a autenticação e devolve o token de acesso.

## 5. Blindagem dos controllers e lógica de *ownership*
O controle de acesso global (URLs) configurado no ```SecurityConfig``` não é suficiente para cenários onde "o Usuário A não pode editar o Monitor do Usuário B". Para isso, verificações granulares foram adicionadas diretamente nos Controllers:
- Implementação de métodos de segurança (ex: ```checkMonitorOwner```) que extraem o usuário autenticado diretamente do contexto do Spring (```authentication.getPrincipal()```);
- O sistema agora verifica ativamente a integridade dos dados e cruza os IDs: se o recurso não pertencer ao usuário logado, e ele não possuir a permissão de ```ROLE_ADMIN```, a requisição é interceptada e o Controller lança imediatamente um erro ```403 Forbidden```.

## 6. Novas funcionalidades administrativas
Aproveitando o novo escopo de privilégios (ROLE_ADMIN), a API foi expandida com recursos de gestão global que usuários comuns não podem acessar:
- Adição de endpoints para busca inteligente de usuários por *query parameters* (podendo buscar dinamicamente por email ou username);
- Criação de uma rota ```GET /monitors``` global, permitindo que a administração visualize a lista completa de todos os monitores cadastrados no BD, ignorando os filtros de dono.

# <br> 03/03 - Novas funcionalidades e melhorias de qualidade (QoL)

## 1. Tratamento global de exceções (*Global Exception Handling* - [64fe222](https://github.com/Julia-Amadio/JADE/commit/64fe22283f33025b4c0740d9479ccf2b79491795))
A API agora possui sistema centralizado para captura e formatação de erros, garantindo que o Frontend (ou o cliente da API) sempre receba respostas padronizadas e previsíveis, independentemente de onde o erro ocorra.
- Implementação do ```StandardErrorDTO```: criado um DTO base para estabelecer estrutura padrão para todos os erros da aplicação (contendo timestamp, status, error, message e path).
- Refatoração do ```GlobalExceptionHandler```: os métodos de tratamento existentes foram atualizados para retornar o novo formato padrão.
- Novos handlers:
  - Adicionado suporte para ```ResponseStatusException```, capturando erros ```404 Not Found``` e ```403 Forbidden``` lançados dinamicamente nos controllers.
  - Implementado um "pega-tudo" (Catch-All) para a classe genérica ```Exception```. Isso blinda a API contra erros inesperados (como falhas de banco de dados ou ```NullPointers```), retornando um ```500 Internal Server Error``` limpo e seguro, sem vazar o stack trace (rastro) do código Java para o usuário.

## 2. Controle de execução do ```MonitorScheduler``` ([61c7d6f](https://github.com/Julia-Amadio/JADE/commit/61c7d6fe7631d5d9fcd4568324bb431f687a8e5a))
Implementação de uma "chave de liga/desliga" para o robô de monitoramento, visando melhorar a experiência de desenvolvimento e testes.

Foi utilizada a anotação ```@ConditionalOnProperty``` na classe ```MonitorScheduler```. Agora, a classe só é instanciada e executada se a propriedade ```jade.scheduler.enabled=true``` estiver definida no arquivo ```application.properties```.

Este controle permite o teste de rotas no Postman sem que o terminal seja poluído com logs contínuos de verificação e evita o inchaço desnecessário do BD de testes.

## 3. Paginação real para o histórico de monitores ([b1f3a1e](https://github.com/Julia-Amadio/JADE/commit/b1f3a1ee57033639ec67a2a555c14f020615ae1c))

A rota que buscava o histórico completo de logs de um monitor foi refatorada para utilizar a paginação nativa do Spring Data JPA, prevenindo futuros problemas com o servidor por falta de memória (```OutOfMemoryError```).
- A consulta no banco de dados passou de ```List<>``` para ```Page<>```, utilizando a interface ```Pageable```.
- A API não devolve mais um array infinito de objetos. Ao invés disso, a resposta agora inclui os dados fatiados (```content```) e metadados vitais para o Frontend, como a página atual, total de elementos guardados (```totalElements```) e total de páginas disponíveis (```totalPages```).
- Queries agora podem ser feitas utilizando parâmetros de URL (ex: ```?page=0&size=20```).


# <br> 08/03 - Controle de versão de BD ([01bcdb2](https://github.com/Julia-Amadio/JADE/commit/01bcdb222c63a658884f0f5881d0974f0a7a9a30))
A gestão do BD agora evolui de uma modelagem manual para migrações automatizadas. Isso garante que o esquema do BD seja versionado junto com o código-fonte, padronizando o ambiente.
- Desativação do DDL-Auto: a propriedade ```spring.jpa.hibernate.ddl-auto``` foi alterada para ```validate```. O Hibernate não tentará mais criar ou alterar tabelas de forma autônoma, limitando-se apenas a validar se as entidades Java correspondem ao banco real.
- Integração do Flyway: adicionadas as dependências necessárias no ```pom.xml```.
- Configuração de baseline: ativada a propriedade ```baseline-on-migrate=true```. Isso permite que o Flyway assumisse o banco de dados já existente no Neon de forma suave, marcando a estrutura atual como a Versão 1, sem tentar recriar tabelas que já estavam lá.
- Migração inicial (V1): criado o arquivo ```V1__Create_initial_schema.sql``` contendo os comandos (DDL) exatos para a criação das tabelas users, monitors, monitor_history e incidents, consolidando o estado atual da arquitetura.


# <br> 10/03 - Configuração de CORS e setup do Frontend com React + Vite ([4e4d94c](https://github.com/Julia-Amadio/JADE/commit/4e4d94c51cf110858fd08fcb14c9ca4b09522315))

## 1. Resolução de bloqueio de comunicação (CORS)
Para permitir que a futura interface web comunique com a API REST sem ser bloqueada pela política de segurança dos navegadores (*Same-Origin Policy*), foi implementada a configuração global de CORS no Spring Boot.
- **Criação da classe `WebConfig.java`:** adicionada ao pacote `config`, implementa a interface `WebMvcConfigurer`.
- **Mapeamento de origens permitidas:** o método `addCorsMappings` foi sobrescrito para permitir o acesso a todos os *endpoints* da API (`/**`) a partir dos endereços de desenvolvimento locais (`http://localhost:5173` para o Vite e `http://localhost:3000` como redundância para padrões Node).
- **Métodos e cabeçalhos:** foram explicitamente permitidos os métodos HTTP essenciais (`GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`), o envio de qualquer cabeçalho e a partilha de credenciais/cookies (`allowCredentials(true)`).

## 2. Setup inicial do Frontend (React)
A fundação da interface de utilizador foi inicializada utilizando a ferramenta **Vite** com o *template* do **React**. A arquitetura foi estruturada no terminal para que a nova pasta `frontend` e a pasta `backend` coexistam como diretórios "irmãos" na raiz do repositório, mantendo a separação clara de responsabilidades.
- **Dependências base:** instalação do `react-router-dom` para a futura gestão das rotas da aplicação (transição entre o ecrã de Monitores, Relatórios e Configurações) e do `lucide-react` para a iconografia.
- **Identidade visual e tipografia:**
    - O CSS *boilerplate* gerado nativamente pelo Vite foi totalmente removido para não interferir no layout.
    - A fonte **JetBrains Mono** foi importada via Google Fonts e definida como tipografia global.
- **Variáveis de estilo:** o ficheiro `index.css` foi reestruturado com variáveis nativas do CSS (`:root`) para centralizar a paleta de cores.


# <br> 14/03 - Configuração de ambiente local (Docker) e Fallback de Banco de Dados ([823595c](https://github.com/Julia-Amadio/JADE/commit/823595c03e66db4d903cdbbdbd3da972baac59d5))

## 1. Containerização do Banco de Dados para testes
Para facilitar o processo de *onboarding* de outros desenvolvedores e a avaliação do projeto por terceiros, foi implementada uma infraestrutura local de banco de dados.
- **Criação do `docker-compose.yml`:** adicionado um serviço encapsulado do PostgreSQL 15 (`jade-postgres-test`) mapeado para a porta `5433`.
- **Independência da Nuvem:** elimina a obrigatoriedade de configurar uma instância remota no Neon DB apenas para clonar e testar o funcionamento do JADE localmente, isolando o ambiente de testes dos dados de produção.

## 2. Estratégia de Fallback inteligente
O arquivo `application.properties` foi refatorado para suportar múltiplos ambientes de forma transparente, utilizando o recurso de *fallback* nativo do Spring Boot.
- **Variáveis dinâmicas:** as credenciais agora seguem o padrão `${VARIAVEL_DE_AMBIENTE:valor_padrao}`.
- **Transição suave:** se as variáveis da nuvem (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) forem injetadas, o Spring conecta ao Neon. Se o projeto for clonado cru, o Spring assume automaticamente os valores padrão apontando para o contêiner Docker (`jdbc:postgresql://localhost:5433/jade_local_db`, usuário `tester_jade` e senha `tester_password`).


# <br> 20/03 - Documentação da API com Swagger/OpenAPI e Melhorias de Observabilidade

Para facilitar o consumo da API por futuros clientes (como o Frontend em React) e agilizar os testes manuais, foi implementada a documentação interativa automatizada utilizando o padrão OpenAPI (Swagger).

## 1. Setup e configuração do SpringDoc
A infraestrutura de documentação foi adicionada e adaptada para suportar as características arquiteturais do JADE:
- **Dependência:** inclusão da biblioteca `springdoc-openapi-starter-webmvc-ui` no `pom.xml`, que escaneia o código e gera a interface gráfica automaticamente no endpoint `/swagger-ui/index.html`.
- **Suporte a paginação:** como a API utiliza paginação nativa, foi adicionada a propriedade `springdoc.model-converters.pageable-converter.enabled=true` no `application.properties`. Isso garante que o Swagger traduza corretamente a interface `Pageable` do Spring, renderizando os parâmetros de URL (`page`, `size`, `sort`) de forma amigável na tela.

## 2. Configuração de segurança (JWT / Bearer Auth)
Como o sistema é protegido por RBAC através de JWT, é necessário "ensinar" o Swagger a lidar com a autenticação:
- **Classes de configuração:** criação dos arquivos `SwaggerConfig` (e `OpenApiConfig`) dentro do pacote `config`. Estas classes utilizam anotações como `@OpenAPIDefinition` e `@SecurityScheme` para definir os metadados globais da API (título, versão) e estabelecer o esquema de segurança do tipo HTTP Bearer.
- **Blindagem visual das rotas:** adição da anotação `@SecurityRequirement(name = "bearerAuth")` em todos os Controllers e rotas específicas que exigem autenticação. Isso ativa o ícone de "cadeado" (🔒) na interface do Swagger, forçando o envio do token JWT no cabeçalho `Authorization` durante as requisições de teste, respeitando a lógica de *ownership* e privilégios de administrador.

## 3. Aprimoramento no Tratamento Global de Exceções
O método que atua como *Catch-All* para erros 500 agora utiliza `log.error()`, registrando a rota exata onde a falha ocorreu e a *stack trace* padronizada no console. Isso previne a perda de logs em ambientes de produção e prepara o terreno para ferramentas futuras de monitoramento.
