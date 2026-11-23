package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CalculadoraResumoDoMesTest {

	private final CalculadoraResumoDoMes calculadora = new CalculadoraResumoDoMes();

	private Transacao t(String data, String valor, String descricao) {
		return new Transacao(LocalDate.parse(data), new BigDecimal(valor), descricao);
	}

	@Test
	@DisplayName("Lista vazia: sem resumo")
	void listaVazia() {
		assertTrue(calculadora.calcular(List.of()).isEmpty());
	}

	@Test
	@DisplayName("Só estornos e pagamentos: sem resumo")
	void soEstornos() {
		assertTrue(calculadora.calcular(List.of(t("2026-11-01", "-50.00", "Pagamento"))).isEmpty());
	}

	@Test
	@DisplayName("Valor zero não é compra")
	void valorZero() {
		assertTrue(calculadora.calcular(List.of(t("2026-11-01", "0.00", "Ajuste"))).isEmpty());
	}

	@Test
	@DisplayName("Calcula total, quantidade, maior gasto, média e período")
	void calculaResumoCompleto() {
		ResumoDoMes resumo = calculadora.calcular(List.of(t("2026-11-01", "45.90", "Sushi"),
				t("2026-11-02", "18.50", "Uber"), t("2026-11-04", "39.90", "Netflix"))).orElseThrow();

		assertEquals(new BigDecimal("104.30"), resumo.total());
		assertEquals(3, resumo.quantidadeDeCompras());
		assertEquals("Sushi", resumo.maiorGasto().descricao());
		assertEquals(new BigDecimal("26.08"), resumo.mediaPorDia());
		assertEquals(LocalDate.of(2026, 11, 1), resumo.primeiraData());
		assertEquals(LocalDate.of(2026, 11, 4), resumo.ultimaData());
	}

	@Test
	@DisplayName("Estorno amplia o período, mas não entra na soma")
	void estornoEntraNoPeriodoMasNaoNaSoma() {
		ResumoDoMes resumo = calculadora
				.calcular(List.of(t("2026-11-01", "10.00", "Compra"), t("2026-11-10", "-5.00", "Estorno")))
				.orElseThrow();

		assertEquals(new BigDecimal("10.00"), resumo.total());
		assertEquals(1, resumo.quantidadeDeCompras());
		assertEquals(new BigDecimal("1.00"), resumo.mediaPorDia()); // 10,00 / 10 dias
	}

	@Test
	@DisplayName("Empate no maior gasto: fica o primeiro do extrato")
	void empateNoMaiorGasto() {
		ResumoDoMes resumo = calculadora
				.calcular(List.of(t("2026-11-01", "20.00", "Primeira"), t("2026-11-02", "20.00", "Segunda")))
				.orElseThrow();

		assertEquals("Primeira", resumo.maiorGasto().descricao());
	}

	@Test
	@DisplayName("Um dia só: a média é o próprio total")
	void umDiaSo() {
		ResumoDoMes resumo = calculadora.calcular(List.of(t("2026-11-01", "10.00", "Compra"))).orElseThrow();

		assertEquals(new BigDecimal("10.00"), resumo.mediaPorDia());
	}

	@Test
	@DisplayName("Média arredonda para cima na metade (HALF_UP)")
	void arredondamento() {
		ResumoDoMes resumo = calculadora
				.calcular(List.of(t("2026-11-01", "0.05", "Compra"), t("2026-11-02", "-1.00", "Estorno")))
				.orElseThrow();

		assertEquals(new BigDecimal("0.03"), resumo.mediaPorDia()); // 0,05 / 2 = 0,025 -> 0,03
	}
}