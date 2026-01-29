# Do build ao runtime
Uma visão detalhada de como a stack do JADE interage, desde a compilação até a execução.

## Compilação (*build time*)
O processo de construção do JADE aproveita o processamento de anotações para reduzir verbosidade:
1. *Resolução de dependências:* o Maven lê o ``pom.xml``, baixa bibliotecas essenciais (Spring Boot Starter, Postgres Driver, Flyway) e, em seguida, chama o compilador do java (``javac``).
2. *Annotation processing (Lombok):* 
    - Diferente de uma biblioteca comum, o Lombok atua durante a compilação. 
    - Intercepta a AST (*Abstract Syntax Tree*) do compilador ``javac``. 
    - Métodos como getters, setters e constructors são injetados diretamente na árvore de sintaxe antes da geração do bytecode final (``.class``).

Resultado: o binário final contém métodos que não existem visualmente no código fonte (``.java``).

![Diagrama exemplificando o fluxo de compilação do JADE.](https://i.postimg.cc/s2wj6XYj/JADE-(build-time).png)

## Execução (*runtime*)
Ao iniciar a aplicação (java -jar), o Spring Boot orquestra a inicialização em ordem de dependência:
1. **Configuração:** leitura do ``application.properties`` e injeção de variáveis de ambiente (Senhas/URLs).
2. **Database migration (Flyway):** antes de qualquer lógica de negócio, o Flyway verifica a versão do banco. Se necessário, executa scripts SQL pendentes para alinhar o Schema.
3. **Validação JPA (Hibernate):** com o banco pronto, o Hibernate inicia. Graças à configuração ``ddl-auto=validate``, ele compara as classes ``@Entity`` com as tabelas reais. Se houver divergência, a aplicação aborta a inicialização (Fail Fast).
4. **Web Server:** apenas após todas as validações de dados, o Tomcat embutido abre a porta 8080 para receber requisições.

![Diagrama exemplificando o fluxo de execução do JADE.](https://i.postimg.cc/wB0qfvXd/JADE-(runtime).png)