package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Transforma um ResumoDoMes no texto que o usuário lê, no formato brasileiro
 * (R$ 1.234,50 e dd/MM/aaaa). Só formata: não faz conta nem conhece o MCP.
 */
public class FormatadorResumoDoMes {

   private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");
   private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

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

   //não usa NumberFormat.getCurrencyInstance porque o Java coloca um espaço
   //"invisível" (não separável) depois do R$, o que atrapalha a comparação de texto
   private String moeda(BigDecimal valor) {
      //DecimalFormat não é thread-safe, por isso é criado a cada chamada
      DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BRASIL));
      formato.setRoundingMode(RoundingMode.HALF_UP);
      return "R$ " + formato.format(valor);
   }
}
