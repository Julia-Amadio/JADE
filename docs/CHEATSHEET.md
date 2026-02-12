Para uso no escopo deste projeto.

# Git
## 1. Fluxo comum
| **COMANDO**                    | **O QUE FAZ**                                                         |
|--------------------------------|-----------------------------------------------------------------------|
| ``git status``                 | Mostra o que mudou e o que está pendente.                             |
| ``git add .``                  | Adiciona todos os arquivos modificados ao commit.                     |
| ``git commit -m "message"``    | Salva o pacote com uma mensagem.                                      |
| ``git push origin main``       | Envia alterações para o GitHub (web).                                 |
| ``git pull``                   | Fetch + merge. Baixa atualizações.                                    |
| ``git fetch origin``           | Busca diferenças em relação ao que existe na máquina local.           |
| ``git branch``                 | Mostra em qual branch está.                                           |
| ``git checkout -b branchname`` | Cria e muda para uma nova branch (ex: ``feature/monitor-scheduler``). |
## 2. Desfazer erros
| **COMANDO**                                 | **O QUE FAZ**                                                                                   |
|---------------------------------------------|-------------------------------------------------------------------------------------------------|
| ``git commit --amend -m "updated message"`` | Permite reescrever a mensagem do último commit.                                                 |
| ``git reset --soft HEAD~1``                 | Desfaz o último commit, mas mantém os arquivos alterados prontos para serem commitados de novo. |
| ``git reset --hard HEAD~1``                 | **APAGA O ÚLTIMO COMMIT** e **DESTRÓI TODAS AS ALTERAÇÕES** nos arquivos. Volta no tempo real.  |
| ``git restore filename``                    | Descarta as mudanças em um arquivo específico (ANTES do ``add``).                               |
- ``HEAD``: estado atual.
- ``~1``: volta um passo para trás (pai do commit atual).

# IntelliJ IDEA
## 1. Live templates
| **Atalho + TAB** | **O QUE ESCREVE**                                                                              |
|------------------|------------------------------------------------------------------------------------------------|
| psvm             | ``public static void main(String[] args) { }``                                                 |
| sout             | ``System.out.println();``                                                                      |
| soutv            | Imprime variável com o nome dela. Útil para debug. <br>Ex: ``System.out.println("x = " + x);`` |
| soutp            | Imprime os parâmetros do método.                                                               |
| fori             | Cria um loop ``for`` simples (``for (int i = 0; i < ...``).                                    |
| iter             | Cria um loop ``if (variavel == null) { ... }`` (listas).                                       |
| ifn              | ``if (variavel == null) { ... }``                                                              |
| inn              | ``if (variavel != null) { ... }``                                                              |
| prsf             | ``private static final`` (constantes).                                                         |
## 2. Navegação e refatoração (atalhos)
| **ATALHO**     | **O QUE FAZ**                                                                                  |
|----------------|------------------------------------------------------------------------------------------------|
| Shift + Shift  | **Search everywhere:** procura qualquer classe, arquivo ou configuração.                       |
| Ctrl + Alt + L | **Reformat code:** indenta e arruma o código bagunçado automaticamente.                        |
| Ctrl + Alt + O | **Optimize imports:** apaga imports inutilizados e organiza os restantes em ordem alfabética.  |
| Alt + Enter    | Resolve certos problemas (importar classe, criar método que não existe, corrigir erro).        |
| Alt + Insert   | **Gerar código:** cria Getters, Setters, Construtores, ``toString`` (se não usar Lombok).      |
| Shift + F6     | **Renomear:** renomeia uma variável/classe e atualiza todas as referências no projeto inteiro. |
| Ctrl + D       | Duplica a linha atual.                                                                         |
| Ctrl + Y       | Apaga a linha atual.                                                                           |
