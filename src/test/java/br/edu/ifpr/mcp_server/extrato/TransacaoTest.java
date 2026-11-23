package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TransacaoTest {

	private Transacao transacao(String valor) {
		return new Transacao(LocalDate.of(2026, 11, 1), new BigDecimal(valor), "Loja");
	}

	@Test
	@DisplayName("Valor positivo é compra")
	void positivoEhCompra() {
		assertTrue(transacao("10.00").ehCompra());
	}

	@Test
	@DisplayName("Valor zero não é compra")
	void zeroNaoEhCompra() {
		assertFalse(transacao("0.00").ehCompra());
	}

	@Test
	@DisplayName("Valor negativo (estorno, pagamento) não é compra")
	void negativoNaoEhCompra() {
		assertFalse(transacao("-10.00").ehCompra());
	}

	@Test
	@DisplayName("Campos nulos são rejeitados")
	void rejeitaNulos() {
		assertThrows(NullPointerException.class, () -> new Transacao(null, BigDecimal.ONE, "Loja"));
		assertThrows(NullPointerException.class, () -> new Transacao(LocalDate.now(), null, "Loja"));
		assertThrows(NullPointerException.class, () -> new Transacao(LocalDate.now(), BigDecimal.ONE, null));
	}
}