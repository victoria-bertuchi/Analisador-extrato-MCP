package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Soma quanto foi gasto em cada categoria.
 * Não sabe classificar sozinha: recebe um classificador pelo construtor
 * (pode ser o da IA ou o de palavra-chave), seguindo o padrão Strategy.
 */

public class CalculadoraGastosPorCategoria {
	private final ClassificadorDeCategoria classificador;

    public CalculadoraGastosPorCategoria(ClassificadorDeCategoria classificador) {
        this.classificador = classificador;
    }

    public Map<Categoria, BigDecimal> calcular(List<Transacao> transacoes) {
        //EnumMap é um Map feito para chaves que são enum: guarda as categorias na ordem do enum
        Map<Categoria, BigDecimal> totais = new EnumMap<>(Categoria.class);

        for (Transacao transacao : transacoes) {
            //só soma valores positivos (compras). Valores negativos no extrato
            //são pagamentos da fatura ou estornos, e não são gastos
            if (transacao.valor().compareTo(BigDecimal.ZERO) > 0) {
                Categoria categoria = classificador.classificar(transacao);

                //pega o total que a categoria já tem (ou zero, se for a primeira vez) e soma o valor
                BigDecimal totalAtual = totais.getOrDefault(categoria, BigDecimal.ZERO);
                totais.put(categoria, totalAtual.add(transacao.valor()));
            }
        }
        return totais;
    }

}
