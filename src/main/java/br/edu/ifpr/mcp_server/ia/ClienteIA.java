package br.edu.ifpr.mcp_server.ia;

/**
 * Contrato de quem conversa com uma IA: recebe as regras e a pergunta e devolve
 * o texto da resposta. Cada IA (Gemini, outra) tem a sua implementação (padrão
 * Strategy).
 */
public interface ClienteIA {

	String perguntar(String instrucoesDoSistema, String mensagemDoUsuario) throws Exception;
}