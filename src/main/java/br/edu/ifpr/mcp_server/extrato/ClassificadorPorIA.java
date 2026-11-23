package br.edu.ifpr.mcp_server.extrato;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import br.edu.ifpr.mcp_server.ia.ClienteIA;

/**
 * Classifica as compras perguntando para uma IA. Manda os lançamentos em lotes
 * e transforma a resposta em categorias.
 */
public class ClassificadorPorIA implements ClassificadorDeCategoria {

	// quantos lançamentos vão em cada pergunta para a IA (evita respostas grandes
	// demais)
	private static final int TAMANHO_DO_LOTE = 50;

	// a lista vem do enum Categoria, então nunca fica diferente das categorias que
	// existem (DRY)
	private static final String LISTA_DE_CATEGORIAS = Arrays.stream(Categoria.values()).map(Enum::name)
			.collect(Collectors.joining(", "));

	// regras fixas encaminhadas para a IA
	private static final String INSTRUCOES = """
			Você é um assistente financeiro especialista em extratos bancários brasileiros.
			Você vai receber uma lista numerada de lançamentos. Classifique cada um em uma categoria.
			Use apenas: %s.
			Use o nome do estabelecimento para decidir (ex.: Ifood = DELIVERY, Uber = TRANSPORTE,
			Netflix = STREAMING, supermercado = MERCADO, restaurante, lanchonete ou padaria = ALIMENTACAO,
			drogaria = FARMACIA, cinema = LAZER).
			Pix ou transferência para uma pessoa, sem nome de estabelecimento, é OUTROS.
			Se não tiver certeza, use OUTROS.
			Responda uma linha por lançamento, no formato NUMERO=CATEGORIA, sem mais nenhum texto.
			Exemplo:
			1=DELIVERY
			2=OUTROS
			""".formatted(LISTA_DE_CATEGORIAS);

	// quem vai conversar com a IA
	private final ClienteIA cliente;

	// recebe o cliente de IA de fora e guarda
	public ClassificadorPorIA(ClienteIA cliente) {
		this.cliente = cliente;
	}

	@Override
	public Categoria classificar(Transacao transacao) {
		return classificarTodas(List.of(transacao)).get(0);
	}

	@Override
	public List<Categoria> classificarTodas(List<Transacao> transacoes) {
		List<Categoria> categorias = new ArrayList<>();
		// manda em lotes: um extrato grande vira poucas perguntas, em vez de uma por
		// compra
		for (int inicio = 0; inicio < transacoes.size(); inicio += TAMANHO_DO_LOTE) {
			int fim = Math.min(inicio + TAMANHO_DO_LOTE, transacoes.size());
			categorias.addAll(classificarLote(transacoes.subList(inicio, fim)));
		}
		return categorias;
	}

	private List<Categoria> classificarLote(List<Transacao> lote) {
		// monta a lista numerada: "1. Ifood", "2. Uber"...
		StringBuilder lista = new StringBuilder();
		for (int i = 0; i < lote.size(); i++) {
			lista.append(i + 1).append(". ").append(lote.get(i).descricao()).append("\n");
		}

		String resposta;
		try {
			resposta = cliente.perguntar(INSTRUCOES, lista.toString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("A comunicação com a IA foi interrompida.", e);
		} catch (Exception e) {
			// erro de internet, chave errada, sem crédito para uso etc.
			throw new IllegalStateException("Não foi possível falar com a IA: " + e.getMessage(), e);
		}

		// começa tudo como OUTROS; cada linha "N=CATEGORIA" válida substitui a posição
		// N
		Categoria[] categorias = new Categoria[lote.size()];
		Arrays.fill(categorias, Categoria.OUTROS);
		for (String linha : resposta.split("\\R")) {
			String[] partes = linha.split("=");
			if (partes.length == 1 && lote.size() == 1) {
				// com um lançamento só, a IA pode responder apenas a categoria (ex.:
				// "DELIVERY")
				partes = new String[] { "1", partes[0] };
			}
			if (partes.length != 2) {
				continue; // linha fora do formato: ignora
			}
			try {
				int posicao = Integer.parseInt(partes[0].replaceAll("\\D", "")) - 1;
				Categoria categoria = Categoria.valueOf(partes[1].trim().toUpperCase());
				if (posicao >= 0 && posicao < lote.size()) {
					categorias[posicao] = categoria;
				}
			} catch (IllegalArgumentException e) {
				// número ou categoria inválidos: aquela posição continua OUTROS
			}
		}
		return List.of(categorias);
	}
}