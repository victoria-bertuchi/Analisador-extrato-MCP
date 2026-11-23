package br.edu.ifpr.mcp_server.extrato;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Classificador simples, sem internet e sem IA: procura palavras-chave na
 * descrição. É usado quando o servidor é iniciado sem a chave do Gemini. A
 * ordem das categorias importa: a primeira que casar vence.
 */
public class ClassificadorPorPalavrasChave implements ClassificadorDeCategoria {

	private static final Map<Categoria, List<String>> PALAVRAS = new LinkedHashMap<>();

	static {
		PALAVRAS.put(Categoria.DELIVERY, List.of("ifood", "ifd*", "rappi", "uber eats"));
		PALAVRAS.put(Categoria.TRANSPORTE, List.of("uber", "99 *", "99app", "cabify", "posto ", "combustivel"));
		PALAVRAS.put(Categoria.STREAMING, List.of("netflix", "spotify", "disney", "hbo", "prime video", "youtube"));
		PALAVRAS.put(Categoria.MERCADO, List.of("supermercado", "mercado", "assai", "atacad", "carrefour"));
		PALAVRAS.put(Categoria.FARMACIA, List.of("drog", "farmacia", "panvel"));
		PALAVRAS.put(Categoria.SAUDE, List.of("clinica", "hospital", "laboratorio", "unimed"));
		PALAVRAS.put(Categoria.ALIMENTACAO, List.of("restaurante", "lanchonete", "padaria", "sushi", "pizzaria"));
		PALAVRAS.put(Categoria.EDUCACAO, List.of("escola", "faculdade", "curso", "udemy"));
		PALAVRAS.put(Categoria.LAZER, List.of("cinema", "ingresso", "steam"));
		PALAVRAS.put(Categoria.VESTUARIO, List.of("renner", "riachuelo", "zara", "c&a"));
		PALAVRAS.put(Categoria.CONTAS_DA_CASA, List.of("energia", "copel", "sanepar", "aluguel", "condominio"));
		PALAVRAS.put(Categoria.PET, List.of("petz", "cobasi", "petshop"));
	}

	@Override
	public Categoria classificar(Transacao transacao) {
		String descricao = normalizar(transacao.descricao());
		for (Map.Entry<Categoria, List<String>> entrada : PALAVRAS.entrySet()) {
			for (String palavra : entrada.getValue()) {
				if (descricao.contains(palavra)) {
					return entrada.getKey();
				}
			}
		}
		return Categoria.OUTROS;
	}

	// deixa em minúsculo e sem acento, para "Farmácia" casar com "farmacia"
	private String normalizar(String texto) {
		return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase();
	}
}