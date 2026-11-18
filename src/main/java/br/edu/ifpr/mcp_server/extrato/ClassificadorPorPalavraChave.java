package br.edu.ifpr.mcp_server.extrato;

/**
 * Classifica uma transação procurando palavras conhecidas na descrição 
 * EX: se a descrição tiver "ifood", a categoria é DELIVERY
 * Se nenhuma palavra for encontrada ele é colocado na categoria OUTROS
 * esta é a forma simples, usada como reserva quando a IA não está disponivel
 */

public class ClassificadorPorPalavraChave  implements ClassificadorDeCategoria{

	@Override
	public Categoria classificar(Transacao transacao) {
		// TODO Auto-generated method stub
		return null;
	}

}
