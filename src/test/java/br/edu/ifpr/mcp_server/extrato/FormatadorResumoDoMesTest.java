package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FormatadorResumoDoMesTest {

	private final FormatadorResumoDoMes formatador = new FormatadorResumoDoMes();

	private ResumoDoMes resumo(int quantidade, String maior) {
		Transacao compra = new Transacao(LocalDate.of(2026, 11, 1), new BigDecimal(maior), "Notebook");
		return new ResumoDoMes(new BigDecimal("1300.00"), quantidade, compra, new BigDecimal("43.33"),
				LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30));
	}

	@Test
	@DisplayName("Formata valores e datas no padrão brasileiro")
	void formataNoPadraoBrasileiro() {
		String esperado = String.join("\n", "Resumo do extrato (01/11/2026 a 30/11/2026)",
				"Total gasto: R$ 1.300,00 em 2 compras", "Maior gasto: R$ 1.234,50 em 01/11/2026 (Notebook)",
				"Média por dia: R$ 43,33");

		assertEquals(esperado, formatador.formatar(resumo(2, "1234.50")));
	}

	@Test
	@DisplayName("Uma compra fica no singular")
	void singular() {
		String texto = formatador.formatar(resumo(1, "10.00"));

		assertTrue(texto.contains("em 1 compra\n"));
	}
}