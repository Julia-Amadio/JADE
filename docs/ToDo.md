# To-Do: Melhorias de Segurança e Robustez para a API

Este arquivo lista os próximos passos sugeridos para elevar o nível de segurança e profissionalismo da API do JADE antes da construção intensiva do frontend.

---

### 1. Implementar Rate Limiting (Limitação de Taxa)

-   **Prioridade:** Alta
-   **Problema:** A API atualmente não possui proteção contra ataques de força bruta ou de negação de serviço (DDoS) em endpoints críticos, como o de login. Um atacante pode fazer milhares de requisições por segundo, sobrecarregando o sistema.
-   **Solução Proposta:** Implementar um limite de requisições (ex: 10 tentativas de login por minuto por endereço IP).
-   **Tecnologia Sugerida:** `Bucket4j`. É uma biblioteca popular e eficiente para este fim, que se integra bem com o Spring.
-   **Plano de Ação:**
    1.  Adicionar a dependência do `bucket4j-core` e `bucket4j-jcache` ao `pom.xml`.
    2.  Criar um filtro (`RateLimitingFilter`) que intercepta requisições para o endpoint `/auth/login`.
    3.  Configurar um "balde de tokens" (bucket) para cada endereço IP que tenta acessar a rota.
    4.  Se um IP exceder o limite, a requisição deve ser bloqueada com um status `429 Too Many Requests`.

---

### 2. Adicionar Spring Boot Actuator para Observabilidade

-   **Prioridade:** Média
-   **Problema:** Atualmente, não há uma maneira padronizada de verificar a "saúde" da aplicação em produção ou de coletar métricas de desempenho (uso de memória, CPU, etc.) sem acesso direto ao servidor.
-   **Solução Proposta:** Integrar o Spring Boot Actuator para expor endpoints de gerenciamento.
-   **Plano de Ação:**
    1.  Adicionar a dependência `spring-boot-starter-actuator` ao `pom.xml`.
    2.  Configurar o `application.properties` para expor os endpoints desejados (ex: `health`, `metrics`, `info`).
    3.  **Crucial:** Proteger os endpoints sensíveis no `SecurityConfig`. Apenas `ROLE_ADMIN` deve ter acesso a `metrics` e outros detalhes internos, enquanto `/actuator/health` pode permanecer mais acessível para verificações de status por outras ferramentas (como um Load Balancer).

---

### 3. Gerar Documentação da API com OpenAPI (Swagger)

-   **Prioridade:** Baixa (Qualidade de Vida / Vitrine)
-   **Problema:** A API não possui uma documentação interativa e automática. Para um terceiro (ou para o futuro "eu"), entender todos os endpoints, seus parâmetros e os formatos de resposta requer a leitura do código-fonte.
-   **Solução Proposta:** Utilizar a especificação OpenAPI 3 para gerar uma documentação viva e uma interface de usuário (Swagger UI).
-   **Tecnologia Sugerida:** `springdoc-openapi`.
-   **Plano de Ação:**
    1.  Adicionar a dependência `springdoc-openapi-starter-webmvc-ui` ao `pom.xml`.
    2.  Acessar a URL `/swagger-ui.html` gerada automaticamente pela aplicação.
    3.  (Opcional) Anotar os controllers e DTOs com `@Operation` e `@Schema` para enriquecer a documentação com descrições mais detalhadas.
    4.  Configurar o `SecurityConfig` para que a UI do Swagger possa ser acessada e para que ela inclua um campo para inserir o token JWT, permitindo testar os endpoints protegidos diretamente pela interface.
