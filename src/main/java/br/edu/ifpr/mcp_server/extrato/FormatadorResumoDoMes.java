package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Transforma um ResumoDoMes no texto que o usuário lê, no formato brasileiro
 * (R$ 1.234,50 e dd/MM/aaaa). Só formata: não faz conta nem conhece o MCP.
 */
public class FormatadorResumoDoMes {

   private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

   private final FormatadorDeMoeda formatadorDeMoeda = new FormatadorDeMoeda();

   public String formatar(ResumoDoMes resumo) {
      Transacao maior = resumo.maiorGasto();

      return String.join("\n",
            "Resumo do extrato (" + data(resumo.primeiraData()) + " a " + data(resumo.ultimaData()) + ")",
            "Total gasto: " + moeda(resumo.total()) + " em " + compras(resumo.quantidadeDeCompras()),
            "Maior gasto: " + moeda(maior.valor()) + " em " + data(maior.data()) + " (" + maior.descricao() + ")",
            "Média por dia: " + moeda(resumo.mediaPorDia()));
   }

   private String compras(int quantidade) {
      return quantidade + (quantidade == 1 ? " compra" : " compras");
   }

   private String data(LocalDate data) {
      return FORMATO_DATA.format(data);
   }

   private String moeda(BigDecimal valor) {
      return formatadorDeMoeda.formatar(valor);
   }
}
