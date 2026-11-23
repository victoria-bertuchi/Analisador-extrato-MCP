package br.edu.ifpr.mcp_server.extrato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Representa um lançamento do extrato bancário (uma linha do CSV). É um record:
 * só guarda dados e não muda depois de criado.
 */
public record Transacao(LocalDate data, BigDecimal valor, String descricao) {

	public Transacao {
		Objects.requireNonNull(data, "data é obrigatória");
		Objects.requireNonNull(valor, "valor é obrigatório");
		Objects.requireNonNull(descricao, "descrição é obrigatória");
	}

	/**
	 * Só valores positivos são compras. Valores negativos (estorno, pagamento da
	 * fatura, entrada de dinheiro) e zero ficam de fora.
	 */
	public boolean ehCompra() {
		return valor.signum() > 0;
	}
}