package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Soma quanto foi gasto em cada categoria. Não sabe classificar sozinha: recebe
 * um classificador pelo construtor, seguindo o padrão Strategy.
 */
public class CalculadoraGastosPorCategoria {

	private final ClassificadorDeCategoria classificador;

	public CalculadoraGastosPorCategoria(ClassificadorDeCategoria classificador) {
		this.classificador = classificador;
	}

	public Map<Categoria, BigDecimal> calcular(List<Transacao> transacoes) {
		// só entram as compras; negativos são entradas, pagamentos ou estornos
		List<Transacao> gastos = transacoes.stream().filter(Transacao::ehCompra).toList();

		// classifica todos os gastos de uma vez (a IA recebe os gastos em lotes)
		List<Categoria> categorias = classificador.classificarTodas(gastos);
		if (categorias.size() != gastos.size()) {
			throw new IllegalStateException("O classificador devolveu " + categorias.size() + " categorias para "
					+ gastos.size() + " compras.");
		}

		// EnumMap é um Map feito para chaves que são enum: guarda as categorias na
		// ordem do enum
		Map<Categoria, BigDecimal> totais = new EnumMap<>(Categoria.class);
		for (int i = 0; i < gastos.size(); i++) {
			// pega o total que a categoria já tem (ou zero, se for a primeira vez) e soma o
			// valor
			totais.merge(categorias.get(i), gastos.get(i).valor(), BigDecimal::add);
		}
		return totais;
	}
}