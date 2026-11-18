package br.edu.ifpr.mcp_server.extrato;

import java.util.List;
import java.util.Map;

/**
 * Classifica uma transação procurando palavras conhecidas na descrição 
 * EX: se a descrição tiver "ifood", a categoria é DELIVERY
 * Se nenhuma palavra for encontrada ele é colocado na categoria OUTROS
 * esta é a forma simples, usada como reserva quando a IA não está disponivel
 */

public class ClassificadorPorPalavraChave  implements ClassificadorDeCategoria{

	private final Map<Categoria, List<String>> palavras;
	
	public  ClassificadorPorPalavraChave(Map<Categoria, List<String>> palavras) {
		this.palavras = palavras;
	}
	
	@Override
	public Categoria classificar(Transacao transacao) {
		// TODO Auto-generated method stub
		return null;
	}

}
