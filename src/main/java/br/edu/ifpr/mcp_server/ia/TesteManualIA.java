package br.edu.ifpr.mcp_server.ia;

import java.math.BigDecimal;
import java.time.LocalDate;

import br.edu.ifpr.mcp_server.extrato.ClassificadorPorIA;
import br.edu.ifpr.mcp_server.extrato.Transacao;

/**
 * Programa para testar a IA na mão: classifica algumas descrições
 * e mostra o resultado no console.
 * Para rodar: botão direito no arquivo > Run As > Java Application.
 */
public class TesteManualIA {
	
	public static void main(String[] args) {
		//le a chave da variavel de ambiente
		String chave = System.getenv("GEMINI_API_KEY");
		if (chave == null || chave.isBlank()) {
			System.out.println("Configure a variável de ambiente OPENAI_API_KEY antes de rodar.");
            return;
		}
		
		//monta o classificador: cria o cliente do gpt com a chave e entrega p/ classificador
		ClassificadorPorIA classificador = new ClassificadorPorIA(new ClienteGemini(chave));
		
		//descriçoes de exemplo
		String[] descricoes = {"Ifd*Saiko Sushi Comerc", "Uber *Trip", "Netflix.Com", "Ifd*Drogaria Sao Paulo", "Petz Curitiba"};
		
		//para cada descricao, cria uma transacao, classifica e mostra o resultado
		for (String descricao : descricoes) {
            Transacao transacao = new Transacao(LocalDate.now(), BigDecimal.TEN, descricao);
            System.out.println(descricao + " -> " + classificador.classificar(transacao));
        }
	}

}
