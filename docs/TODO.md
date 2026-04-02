# To-Do: melhorias de Segurança e Robustez para a API

Este arquivo lista os próximos passos sugeridos para elevar o nível de segurança e profissionalismo da API do JADE antes da construção intensiva do frontend.

## Legenda
🔴 Alta prioridade · 🟡 Média · 🟢 Baixa

---

### 🔴 Implementar *rate limiting*

-   **Problema:** a API ainda não possui proteção contra ataques de força bruta ou de negação de serviço (DDoS) em endpoints críticos, como o de login. Um atacante pode fazer milhares de requisições por segundo, sobrecarregando o sistema.
-   **Solução Proposta:** implementar um limite de requisições (ex: 10 tentativas de login por minuto por endereço IP). Usar `Bucket4j`, uma biblioteca popular e eficiente para este fim, que se integra bem com o Spring.
-   **Plano de ação:**
    1.  Adicionar a dependência do `bucket4j-core` e `bucket4j-jcache` ao `pom.xml`.
    2.  Criar um filtro (`RateLimitingFilter`) que intercepta requisições para o endpoint `/auth/login`.
    3.  Configurar um "balde de tokens" (bucket) para cada endereço IP que tenta acessar a rota.
    4.  Se um IP exceder o limite, a requisição deve ser bloqueada com um status `429 Too Many Requests`.

---

### 🟡 Adicionar Spring Boot Actuator para observabilidade

-   **Problema:** atualmente, não há uma maneira padronizada de verificar a "saúde" da aplicação em produção ou de coletar métricas de desempenho (uso de memória, CPU, etc.) sem acesso direto ao servidor.
-   **Solução proposta:** integrar o Spring Boot Actuator para expor endpoints de gerenciamento.
-   **Plano de ação:**
    1.  Adicionar a dependência `spring-boot-starter-actuator` ao `pom.xml`.
    2.  Configurar o `application.properties` para expor os endpoints desejados (ex: `health`, `metrics`, `info`).
    3.  **Crucial:** proteger os endpoints sensíveis no `SecurityConfig`. Apenas `ROLE_ADMIN` deve ter acesso a `metrics` e outros detalhes internos, enquanto `/actuator/health` pode permanecer mais acessível para verificações de status por outras ferramentas (como um Load Balancer).

---

### Outras pendências

- [ ] 🔴 Criar `enum IncidentStatus` e `enum IncidentSeverity`
- [ ] 🔴 Configurar `api.security.token.secret` forte via variável de ambiente
- [ ] 🟡 Fallback de HEAD para GET no `pingUrl` (405)
- [ ] 🟡 Extrair `checkMonitorOwner` para `OwnershipValidator` (`@Component`)
- [ ] 🟢 Padronizar ResponseDTOs restantes com `@Builder`
- [ ] 🟢 Centralizar mappers `toResponseDTO`
- [ ] 🟢 Scheduler assíncrono para múltiplos monitores

---
