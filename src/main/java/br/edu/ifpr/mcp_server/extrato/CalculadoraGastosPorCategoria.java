package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Soma quanto foi gasto em cada categoria.
 * Não sabe classificar sozinha: recebe um classificador pelo construtor,
 * seguindo o padrão Strategy.
 */
public class CalculadoraGastosPorCategoria {
	private final ClassificadorDeCategoria classificador;

	public CalculadoraGastosPorCategoria(ClassificadorDeCategoria classificador) {
		this.classificador = classificador;
	}

	public Map<Categoria, BigDecimal> calcular(List<Transacao> transacoes) {
		// só entram valores positivos (gastos); negativos são entradas, pagamentos ou estornos
		List<Transacao> gastos = transacoes.stream()
				.filter(t -> t.valor().compareTo(BigDecimal.ZERO) > 0)
				.toList();

		// classifica todos os gastos de uma vez (a IA recebe tudo numa única pergunta)
		List<Categoria> categorias = classificador.classificarTodas(gastos);

		// EnumMap é um Map feito para chaves que são enum: guarda as categorias na ordem do enum
		Map<Categoria, BigDecimal> totais = new EnumMap<>(Categoria.class);
		for (int i = 0; i < gastos.size(); i++) {
			// pega o total que a categoria já tem (ou zero, se for a primeira vez) e soma o valor
			totais.merge(categorias.get(i), gastos.get(i).valor(), BigDecimal::add);
		}
		return totais;
	}
}