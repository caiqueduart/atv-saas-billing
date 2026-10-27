# Relatório Técnico

**Dupla:** Caique Duarte, Gabriel Matos

## 1. Tabela de diagnóstico do débito técnico

| #   | Linhas                          | Problema                                                                                                                                               | Princípio / Smell               | Impacto prático                                                                                     |
| --- | ------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ | ------------------------------- | --------------------------------------------------------------------------------------------------- |
| 1   | L14–16                          | Credenciais do banco fixas no código                                                                                                                   | Segurança                       | Senha versionada no Git                                                                             |
| 2   | L18–105                         | Um único método faz cálculo, consulta, gravação de fatura e envio de e-mail                                                                            | Long Method                     | Qualquer mudança exige reler o método inteiro                                                       |
| 3   | L18–20                          | `planType` nunca é usado                                                                                                                               | código morto                    | Parâmetros `String` trocados de ordem não geram erro de compilação                                  |
| 4   | L18, L29–57, L85                | Valores monetários em `double`                                                                                                                         | Primitive Obsession             | Erros de ponto flutuante em cálculos de centavos                                                    |
| 5   | L59, L89–96                     | `DriverManager` e `SimpleEmail` chamados dentro da regra de negócio                                                                                    | Dependência direta de I/O       | Impossível testar o cálculo sem PostgreSQL e SMTP                                                   |
| 6   | L25                             | `LocalDate.now()` estático                                                                                                                             | Dependência de relógio global   | Resultado depende da data de execução; regra da Black Friday não é testável de forma determinística |
| 7   | L32, L34, L38, L43, L45, L50–51 | Números mágicos (`0.15`, `0.10`, `0.70`, `30.0`, `0.90`, `0.02`, `0.001`)                                                                              | Magic Number                    | Regras só são identificáveis lendo o cálculo inteiro                                                |
| 8   | L100–102                        | `catch (Exception e)` genérico que transforma qualquer falha em string de log                                                                          | Erro mascarado                  | Falhas de infraestrutura e erros de programação ficam silenciados                                   |
| 9   | L86, L94, L96, L98              | Fatura gravada como `FAILED` (L86), e-mail diz que a fatura "foi processada" (L94); log registra "E-mail disparado" (L98) com `send()` comentado (L96) | Inconsistência de comportamento | O log afirma um envio que não ocorre, e o texto do e-mail contradiz o status da fatura              |

## 2. Métricas antes vs. depois da refatoração

| Métrica                                                        | Antes (legado)                         | Depois                                                                 |
| -------------------------------------------------------------- | -------------------------------------- | ---------------------------------------------------------------------- |
| Linhas do método principal (`processMonthlyBilling`)           | 88 (L18–105)                           | 11                                                                     |
| Complexidade ciclomática do método principal                   | 18                                     | 3                                                                      |
| Maior complexidade ciclomática entre os métodos                | 18                                     | 6 (`PricingCalculator.applyPromoCode`)                                 |
| Parâmetros do método principal                                 | 8 (incluindo `planType`, não usado)    | 1 (`BillingCommand`)                                                   |
| Responsabilidades na classe `SubscriptionBillingService`       | 5 (preço, banco, pagamento, fatura, e-mail) | 1 (orquestração)                                                  |
| Classes / interfaces                                           | 1 classe                               | 11 classes/records/enums + 4 interfaces (portas)                       |
| Valores monetários                                             | `double`                               | `Money` (BigDecimal, 2 casas, HALF_EVEN)                               |
| Números mágicos na regra de preço                              | 7 (+ limites 12, 24 e 1 mês)           | 0 (constantes nomeadas em `PricingCalculator`)                         |
| Credenciais no código-fonte                                    | 3 (URL, usuário, senha)                | 0 (`DataSource` injetado)                                              |
| Dependências concretas de I/O na regra de negócio              | 3 (`DriverManager`, `SimpleEmail`, `LocalDate.now()`) | 0 (portas + `Clock` injetado)                           |
| Blocos `catch (Exception e)`                                   | 1                                      | 0 (falhas propagam como `InfrastructureException`)                     |
| Testes unitários                                               | 0                                      | 34, executados sem banco nem SMTP (`mvn test`)                         |

Complexidade ciclomática contada como 1 + número de pontos de decisão (`if`, `else if`, `&&`, `?:`, `catch`).

### Decisões que alteram o comportamento observável

- **Arredondamento:** o legado acumulava erro de `double` e gravava valores como `84.9915`; agora cada operação é arredondada para centavos (HALF_EVEN). Em casos com multa e juros a diferença pode ser de 1 centavo em relação ao `double` não arredondado.
- **E-mail (item 9):** o adaptador SMTP passa a chamar `send()`, o texto do e-mail reflete o status da fatura (paga ou falha) e o log só é registrado após o envio.
- **Erros (item 8):** falhas de banco ou SMTP deixam de virar uma linha de log e passam a ser lançadas para o chamador.
