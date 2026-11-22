package br.edu.ifpr.mcp_server.extrato;

import java.util.List;

public interface ClassificadorDeCategoria {
	Categoria classificar(Transacao transacao);

	// classifica várias transações; a resposta vem na mesma ordem da lista recebida.
	// por padrão classifica uma por vez, mas quem puder fazer tudo de uma vez (como a IA) sobrescreve
	default List<Categoria> classificarTodas(List<Transacao> transacoes) {
		return transacoes.stream().map(this::classificar).toList();
	}
}