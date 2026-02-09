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
Com o registro de logs pronto, o próximo passo lógico é dar inteligência ao monitoramento. 

Se um monitor onde um ping é lançado a cada 10 segundos possui 600 logs de DOWN, não temos 600 problemas, mas sim um problema que durou 1 hora -- assim, temos um INCIDENTE.

Foi implementada a seguinte lógica no scheduler:
1. UP --> DOWN:
   - A URL monitorada caiu. 
   - Verificação: já existe um incidente aberto para esse monitor? 
   - Não? ABRIR INCIDENTE (Status: OPEN).
2. DOWN --> DOWN:
   - A URL continua sem fornecer resposta. 
   - Fazer nada (ou apenas atualizar o log). O incidente continua OPEN.
3. DOWN --> UP:
   - Voltou!
   - Verificar: Existe um incidente aberto? 
   - Se sim, FECHAR INCIDENTE (Status: RESOLVED).

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
Anteriormente, os Controllers estavam expondo diretamente as Entidades JPA (``User`` e ``Monitor``). Isso trazia riscos de segurança (exposição do hash da senha) e problemas técnicos (referência circular/StackOverflow ao serializar JSON). A solução foi dividir cada modelo em três representações:
- **CreateDTO:** focado na entrada, contém validações rígidas (``@NotBlank``, ``@Email``, regex de complexidade de senha) para garantir a integridade dos dados antes de tocarem a regra de negócio;
- **ResponseDTO:** focado na saída, sanitiza os dados (remove senhas e dados sensíveis) e achata relacionamentos (retorna ``userId`` ao invés do objeto ``User`` completo) para evitar loops infinitos;
- **UpdateDTO:** focado em atualização, permite campos nulos, possibilitando que o usuário atualize apenas a senha ou apenas o email, sem precisar reenviar o objeto inteiro (comportamento similar ao PATCH).

## 2. Tratamento de erros (*Global Exception Handler*)
Erros genéricos foram substituídos por respostas HTTP semânticas e informativas:
- **``GlobalExceptionHandler``:** criado com ``@RestControllerAdvice``, intercepta exceções em toda a aplicação e padroniza o JSON de resposta.
- **Validação de campos (``400 Bad Request``):** captura ``MethodArgumentNotValidException`` e retorna um mapa detalhado de qual campo falhou e porquê.
- **Conflitos de dados (``409 Conflict``):** implementação da classe ``DataConflictException``. Tentativas de cadastro de emails ou usernames duplicados retornam status ``409`` com mensagem clara.

## 3. Atualização da lógica de negócio (``service`` & ``controller``)
- Controllers agora atuam como conversores, recebendo DTOs, chamando o Service, e convertendo o resultado para ResponseDTOs.
- Services agora possui verificação preventiva adicional para lançar exceções de negócio antes de tentar o save no banco. Foi também implementada a lógica de *Partial Update*: o Service verifica campo a campo; se o DTO trouxe um valor (não nulo), ele atualiza a entidade. Se trouxe nulo, mantém o valor antigo.