package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Resultado do resumo de um extrato: só guarda os números, sem fazer conta.
 * É um record: não muda depois de criado.
 *
 * @param total               soma de todas as compras (valores positivos)
 * @param quantidadeDeCompras quantas compras entraram na soma
 * @param maiorGasto          a compra de maior valor (guarda data e descrição junto)
 * @param mediaPorDia         total dividido pelos dias do período, com 2 casas decimais
 * @param primeiraData        data da primeira transação do extrato
 * @param ultimaData          data da última transação do extrato
 */
public record ResumoDoMes(BigDecimal total,
                          int quantidadeDeCompras,
                          Transacao maiorGasto,
                          BigDecimal mediaPorDia,
                          LocalDate primeiraData,
                          LocalDate ultimaData) {

}
