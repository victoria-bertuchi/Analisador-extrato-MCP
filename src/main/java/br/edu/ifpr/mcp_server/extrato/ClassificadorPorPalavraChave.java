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

	private final Map<Categoria, List<String>> palavrasPorCategoria;
	
	public  ClassificadorPorPalavraChave(Map<Categoria, List<String>> palavrasPorCategoria) {
		this.palavrasPorCategoria = palavrasPorCategoria;
	}
	
	@Override
	public Categoria classificar(Transacao transacao) {
		// TODO Auto-generated method stub
		String descricao = transacao.descricao().toLowerCase();
		for (Categoria categoria: palavrasPorCategoria.keySet()) {
			for(String palavra: palavrasPorCategoria.get(categoria)) {
				if(descricao.contains(palavra)) {
					return categoria;
				}
			}
			
		}
		return Categoria.OUTROS;
	
	}

}
