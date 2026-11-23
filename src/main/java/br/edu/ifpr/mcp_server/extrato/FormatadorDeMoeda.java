package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Transforma um valor em texto no formato brasileiro: R$ 1.234,50. Fica numa
 * classe só para todas as respostas mostrarem o dinheiro do mesmo jeito (DRY).
 */
public class FormatadorDeMoeda {

	private static final Locale BRASIL = Locale.forLanguageTag("pt-BR");

	// não usa NumberFormat.getCurrencyInstance porque o Java coloca um espaço
	// "invisível" (não separável) depois do R$, o que atrapalha a comparação de
	// texto
	public String formatar(BigDecimal valor) {
		// DecimalFormat não é thread-safe, por isso é criado a cada chamada
		DecimalFormat formato = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(BRASIL));
		formato.setRoundingMode(RoundingMode.HALF_UP);
		return "R$ " + formato.format(valor);
	}
}