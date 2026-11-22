package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Calcula o resumo de um extrato: total gasto, maior gasto e média por dia.
 * Só faz contas: não sabe ler arquivo, não conhece o MCP e não formata texto.
 *
 * Regras:
 * - só valores positivos são compras. Valores negativos (estorno, pagamento da fatura) e zero ficam de fora, a mesma regra de CalculadoraGastosPorCategoria;
 * - o período vai da primeira à última data do extrato (todas as transações, inclusive estornos), e a média é o total dividido pelos dias desse período.
 */
public class CalculadoraResumoDoMes {

   private static final int CASAS_DECIMAIS = 2;

   /**
    * @param transacoes linhas do extrato, em qualquer ordem
    * @return o resumo, ou vazio se o extrato não tem nenhuma compra
    */
   public Optional<ResumoDoMes> calcular(List<Transacao> transacoes) {
      List<Transacao> compras = filtrarCompras(transacoes);
      if (compras.isEmpty()) {
         return Optional.empty();
      }

      LocalDate primeiraData = primeiraData(transacoes);
      LocalDate ultimaData = ultimaData(transacoes);
      BigDecimal total = somar(compras);

      return Optional.of(new ResumoDoMes(total,
                                         compras.size(),
                                         maiorGasto(compras),
                                         mediaPorDia(total, primeiraData, ultimaData),
                                         primeiraData,
                                         ultimaData));
   }

   private List<Transacao> filtrarCompras(List<Transacao> transacoes) {
      return transacoes.stream()
                       .filter(transacao -> transacao.valor().signum() > 0)
                       .toList();
   }

   private BigDecimal somar(List<Transacao> compras) {
      return compras.stream()
                    .map(Transacao::valor)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
   }

   //em caso de empate, fica a compra que aparece primeiro no extrato
   private Transacao maiorGasto(List<Transacao> compras) {
      return compras.stream()
                    .max(Comparator.comparing(Transacao::valor))
                    .orElseThrow();
   }

   private LocalDate primeiraData(List<Transacao> transacoes) {
      return transacoes.stream()
                       .map(Transacao::data)
                       .min(Comparator.naturalOrder())
                       .orElseThrow();
   }

   private LocalDate ultimaData(List<Transacao> transacoes) {
      return transacoes.stream()
                       .map(Transacao::data)
                       .max(Comparator.naturalOrder())
                       .orElseThrow();
   }

   //conta os dois extremos: de 01/10 a 31/10 são 31 dias
   private BigDecimal mediaPorDia(BigDecimal total, LocalDate primeiraData, LocalDate ultimaData) {
      long dias = ChronoUnit.DAYS.between(primeiraData, ultimaData) + 1;
      return total.divide(BigDecimal.valueOf(dias), CASAS_DECIMAIS, RoundingMode.HALF_UP);
   }
}
