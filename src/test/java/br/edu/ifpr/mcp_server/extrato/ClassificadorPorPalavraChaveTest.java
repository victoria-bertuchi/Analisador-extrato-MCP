package br.edu.ifpr.mcp_server.extrato;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ClassificadorPorPalavrasChaveTest {

	private final ClassificadorPorPalavrasChave classificador = new ClassificadorPorPalavrasChave();

	@ParameterizedTest(name = "\"{0}\" vira {1}")
	@DisplayName("Classifica pela palavra-chave da descrição")
	@CsvSource({ "Ifd*Saiko Sushi Comerc, DELIVERY", "Uber *Trip, TRANSPORTE", "Netflix.Com, STREAMING",
			"Drogaria Sao Paulo, FARMACIA", "Padaria Pão Quente, ALIMENTACAO", "Petz Curitiba, PET",
			"Pix - Enviado - Fulano, OUTROS", "Loja Qualquer, OUTROS" })
	void classificaPelaDescricao(String descricao, Categoria esperada) {
		Transacao transacao = new Transacao(LocalDate.of(2026, 11, 1), BigDecimal.TEN, descricao);

		assertEquals(esperada, classificador.classificar(transacao));
	}
}