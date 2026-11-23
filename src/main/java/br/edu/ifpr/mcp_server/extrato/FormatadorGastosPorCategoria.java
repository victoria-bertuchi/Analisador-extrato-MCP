package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Monta o texto da resposta de gastos por categoria: uma linha por categoria e
 * o total no final. Só formata: não faz a conta de gastos nem conhece o MCP.
 */
public class FormatadorGastosPorCategoria {

	private static final String SEM_GASTOS = "Nenhum gasto encontrado no extrato.";

	private final FormatadorDeMoeda moeda = new FormatadorDeMoeda();

	public String formatar(Map<Categoria, BigDecimal> totais) {
		if (totais.isEmpty()) {
			return SEM_GASTOS;
		}
		StringBuilder texto = new StringBuilder("Gastos por categoria:\n");
		BigDecimal totalGeral = BigDecimal.ZERO;
		for (Map.Entry<Categoria, BigDecimal> item : totais.entrySet()) {
			texto.append("- ").append(item.getKey()).append(": ").append(moeda.formatar(item.getValue())).append("\n");
			totalGeral = totalGeral.add(item.getValue());
		}
		return texto.append("Total: ").append(moeda.formatar(totalGeral)).toString();
	}
}