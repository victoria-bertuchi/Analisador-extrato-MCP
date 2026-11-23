package br.edu.ifpr.mcp_server.ia;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.ifpr.mcp_server.extrato.ClassificadorPorIA;
import br.edu.ifpr.mcp_server.extrato.Transacao;

/**
 * Programa para testar a IA na mão: classifica algumas descrições e mostra o
 * resultado no console. Não é um teste automático (por isso o nome não termina
 * em Test). Para rodar: botão direito no arquivo > Run As > Java Application
 * (com a variável GEMINI_API_KEY na aba Environment da configuração de
 * execução).
 */
public class TesteManualIA {

	public static void main(String[] args) {
		// lê a chave da variável de ambiente
		String chave = System.getenv("GEMINI_API_KEY");
		if (chave == null || chave.isBlank()) {
			System.out.println("Configure a variável de ambiente GEMINI_API_KEY antes de rodar.");
			return;
		}

		// monta o classificador: cria o cliente do Gemini com a chave e entrega para o
		// classificador
		ClassificadorPorIA classificador = new ClassificadorPorIA(new ClienteGemini(chave));

		// descrições de exemplo
		String[] descricoes = { "Ifd*Saiko Sushi Comerc", "Uber *Trip", "Netflix.Com", "Ifd*Drogaria Sao Paulo",
				"Petz Curitiba" };

		// para cada descrição, cria uma transação, classifica e mostra o resultado
		for (String descricao : descricoes) {
			Transacao transacao = new Transacao(LocalDate.now(), BigDecimal.TEN, descricao);
			System.out.println(descricao + " -> " + classificador.classificar(transacao));
		}
	}
}