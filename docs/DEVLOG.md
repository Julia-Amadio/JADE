# 06/01 - Camada de Modelo (Package com.jadeproject.backend.model)
O pacote ``model`` constitui a camada de domínio da aplicação. Contém as entidades JPA (Java Persistence API), que são classes Java responsáveis por representar a estrutura de dados do sistema e suas regras de integridade. Estas classes desempenham dois papéis fundamentais na arquitetura:
- **Mapeamento Objeto-Relacional (ORM):** através de anotações (como ``@Entity``, ``@Table``, ``@Column``), estas classes definem como o banco de dados deve ser estruturado. O framework utiliza esses "modelos" para criar e validar automaticamente as tabelas, colunas e chaves estrangeiras no PostgreSQL.
- **Representação de estado:** durante a execução da aplicação (runtime), estas classes são instanciadas para manipular os dados na memória RAM, transportando informações entre o BD e as camadas de regra de negócio.

Em resumo, o diretório model é a fonte da verdade sobre a estrutura dos dados, servindo tanto como espelho para a criação das tabelas no banco quanto como molde para os objetos manipulados pela aplicação.

# <br>08/01 - Camada Repository (Package com.jadeproject.backend.repository)
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
## SMOKE TEST (“teste de fumaça”)
Simplesmente rodar a aplicação (``public class JadeprojectBackendApplication``). Isso testa:
- Se o Spring consegue se conectar ao banco de dados.
- Se as anotações (``@Entity``, ``@Column``) estão corretas.
- Se o Hibernate consegue criar as tabelas no banco.

**Resultado esperado:** console deve terminar com uma mensagem parecida com ``Started JadeProjectApplication in X seconds``, não exibindo Exception ou quaisquer outros avisos de erro.

## Alterações no banco
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

## TESTE DE CARGA INICIAL (*data loader*)
Envolve a criação da classe temporária chamada ``DataLoader.java``, que é inserida no pacote raiz e roda assim que o sistema liga. Ela vai tentar salvar um usuário e mostrar no console, provando que as camadas estão conversando perfeitamente. Inicialmente, ela está elaborada para funcionar sem a camada Service, vide [este commit](https://github.com/Julia-Amadio/JADE/commit/6398df3ff27e1797e3afc5599fcace5704d3b8b0).

# <br>10/01 - Camada Service (Package com.jadeproject.backend.repository) e alterações
A camada Service serve para garantir o princípio do SOC (Separation of Concerns, ou separação de responsabilidades). Suas funções principais incluem:
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

## Atualizações no ``DataLoader.java``
O teste de carga de dados foi reestruturado (vide [este commit](https://github.com/Julia-Amadio/JADE/commit/be8d96fcbcb9a45811a84267caba8650035a56a5)) para que fizesse uso da camada de Serviços. Em sequência, ele realiza:
- A criação de um Usuário;
- A criação de um Monitor, pertencente ao Usuário teste;
- A criação de quatro logs para o Monitor, com três sucessos e uma falha;
- Simulação da “queda” do monitor, emitindo um Incidente.
    - Inicialmente, o Incidente é registrado com status ``OPEN`` e, após isso, a thread de execução é pausada por trinta segundos (Thread.sleep(30000);) para verificação visual do status dentro do banco. 
    - Após o intervalo, o status do Incidente é alterado para ``RESOLVED``, simulando a retomada do funcionamento do monitor.

# <br>13/01 - Camada Controller (Package com.jadeproject.backend.controller)
A camada Controller é o componente responsável por expor as funcionalidades do sistema para o mundo externo. No contexto de uma API REST (como o JADE), ela atua como um intermediário que traduz as requisições HTTP (vindas da Web) para comandos Java que o sistema entende.

O funcionamento técnico segue este ciclo para cada ação (ex: criar monitor):
1. **Recepção (listening):** o Controller fica "ouvindo" endereços específicos (Endpoints), como POST /monitors.
2. **Deserialização:** quando uma requisição chega, o Controller pega os dados (geralmente em formato JSON) e os converte para Objetos Java (ex: transforma ``{ "name": "Google" }`` em ``new Monitor()``).
3. **Delegação:** o Controller não toma decisões complexas. Ele repassa o objeto para a camada Service, que contém a inteligência do negócio.
4. **Resposta (serialization):**
    - Se o Service processar com sucesso, o Controller recebe o resultado, empacota em um JSON e devolve com Status ``200 OK``.
    - Se o Service lançar um erro, o Controller captura esse erro e devolve uma mensagem apropriada com Status ``400 Bad Request`` ou ``500 Internal Error``.