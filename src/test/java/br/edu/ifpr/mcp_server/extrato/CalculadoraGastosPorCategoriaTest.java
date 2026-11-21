package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Testes da calculadora de gastos por categoria.
 * Usa um classificador falso para testar só a soma, sem depender da IA.
 */

public class CalculadoraGastosPorCategoriaTest {
	//um classificardor falso, devolve a categoria já combinada para cada descrição.
    //o teste confere só a soma, e não se a classificação está certa.
    private final ClassificadorDeCategoria classificadorFalso = new ClassificadorDeCategoria() {
        @Override
        public Categoria classificar(Transacao transacao) {
            return switch (transacao.descricao()) {
                case "Uber *Trip", "99 *Corrida" -> Categoria.TRANSPORTE;
                case "Ifd*Saiko Sushi" -> Categoria.DELIVERY;
                default -> Categoria.OUTROS;
            };
        }
    };

    private final CalculadoraGastosPorCategoria calculadora = new CalculadoraGastosPorCategoria(classificadorFalso);

    //data fixa porque a calculadora não usa a data
    private Transacao transacao(String descricao, String valor) {
        return new Transacao(LocalDate.now(), new BigDecimal(valor), descricao);
    }

    //conferir que duas compras da mesma categoria são somadas num total só
    @Test
    @DisplayName("Soma as compras da mesma categoria")
    void somaMesmaCategoria() {
        List<Transacao> transacoes = List.of(
                transacao("Uber *Trip", "10.00"),
                transacao("99 *Corrida", "20.50"));

        Map<Categoria, BigDecimal> totais = calculadora.calcular(transacoes);

        assertEquals(Map.of(Categoria.TRANSPORTE, new BigDecimal("30.50")), totais);
    }

    //conferir que compras de categorias diferentes ficam em totais separados
    @Test
    @DisplayName("Separa o total de cada categoria")
    void separaPorCategoria() {
        List<Transacao> transacoes = List.of(
                transacao("Uber *Trip", "10.00"),
                transacao("Ifd*Saiko Sushi", "45.90"));

        Map<Categoria, BigDecimal> totais = calculadora.calcular(transacoes);

        assertEquals(Map.of(Categoria.TRANSPORTE, new BigDecimal("10.00"),
                            Categoria.DELIVERY, new BigDecimal("45.90")), totais);
    }

    // conferir que valores negativos (pagamento da fatura, estorno) não entram na soma
    @Test
    @DisplayName("Ignora valores negativos")
    void ignoraNegativos() {
        List<Transacao> transacoes = List.of(
                transacao("Uber *Trip", "15.00"),
                transacao("Pagamento recebido", "-100.00"));

        Map<Categoria, BigDecimal> totais = calculadora.calcular(transacoes);

        assertEquals(Map.of(Categoria.TRANSPORTE, new BigDecimal("15.00")), totais);
    }

}
