package br.edu.ifpr.mcp_server.extrato;

import br.edu.ifpr.mcp_server.ia.ClienteIA;

public class ClassificadorPorIA implements ClassificadorDeCategoria{
	//regras fixas encaminhadas para IA
	private static final String INSTRUCOES = """
			Você é uma assistente financeiro especialista em extratos
			bancários brasileiros.
			Classifique o lançamento em uma categoria. 
			Use apenas: DELIVERY, TRANSPORTE, MERCADO, FARMACIA, SAUDE,
			STREAMING, ALIMENTACAO, EDUCACAO, LAZER, VESTUARIO, 
			CONTAS_DA_CASA, PET, OUTROS.
			Se não tiver certeza, responda OUTROS. 
			Responda somente com o nome da categoria.
			""";
   //quem vai conversar com a IA
	private final ClienteIA cliente;
	
   // recebe o cliente de IA de fora e guarda
	public ClassificadorPorIA(ClienteIA cliente) {
		this.cliente = cliente;
		
	}
	
	@Override
	public Categoria classificar(Transacao transacao) {
		try {
			//pergunta p/ IA e guarda oq ela responder
			String resposta = cliente.perguntar(INSTRUCOES, transacao.descricao());
			
			//IA responde um texto
			return Categoria.valueOf(resposta.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			//IA respondeu algo q não é uma categoria da lista
			return Categoria.OUTROS;
		} catch (Exception e) {
			//erro de internet , chave errada, sem crédito p/ uso e etc
			throw new IllegalStateException("Não foi possivel falar com a IA: " +e.getMessage(), e);
			
		}
	}

}
