package br.edu.ifpr.mcp_server.mcp.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.extrato.CalculadoraGastosPorCategoria;
import br.edu.ifpr.mcp_server.extrato.FormatadorGastosPorCategoria;
import br.edu.ifpr.mcp_server.extrato.LeitorDeExtratoFactory;
import br.edu.ifpr.mcp_server.extrato.Transacao;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.mcp.utils.ResolvedorDeArquivoDoExtrato;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Ferramenta MCP "gastos_por_categoria". Recebe um extrato em CSV (pelo caminho
 * ou anexado no chat), classifica cada compra e devolve quanto foi gasto em
 * cada categoria. A ferramenta não sabe se a classificação é feita pela IA ou
 * por palavras-chave: o classificador chega pronto, dentro da calculadora
 * (injeção de dependência pelo construtor).
 */
public class GastosPorCategoriaTool implements McpToolDefinition {

	private static final String NOME_FERRAMENTA = "gastos_por_categoria";

	private final ResolvedorDeArquivoDoExtrato resolvedor;
	private final CalculadoraGastosPorCategoria calculadora;
	private final FormatadorGastosPorCategoria formatador;

	public GastosPorCategoriaTool(ResolvedorDeArquivoDoExtrato resolvedor, CalculadoraGastosPorCategoria calculadora,
			FormatadorGastosPorCategoria formatador) {
		this.resolvedor = resolvedor;
		this.calculadora = calculadora;
		this.formatador = formatador;
	}

	@Override
	public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
		// os parâmetros (arquivo e conteudo) são os mesmos da ferramenta resumo_do_mes
		Map<String, Object> inputSchema = Map.of("type", "object", "properties",
				ResolvedorDeArquivoDoExtrato.propriedadesDoEsquema(), "additionalProperties", false);

		Tool tool = Tool.builder(NOME_FERRAMENTA, inputSchema).title("Gastos por categoria")
				.description("Lê um extrato bancário em CSV, classifica cada compra "
						+ "e devolve o total gasto em cada categoria. "
						+ "Se o usuário anexar o CSV no chat, passe o texto completo do arquivo em 'conteudo', "
						+ "sem alterar nenhuma linha. Se ele informar um caminho, use 'arquivo'.")
				.build();

		// quando o cliente chamar a ferramenta, o método run (abaixo) é executado
		SyncToolSpecification toolSpec = SyncToolSpecification.builder().tool(tool).callHandler(this::run).build();
		mcp.tools(toolSpec);
	}

	@Override
	public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
		Path arquivo;
		try {
			arquivo = resolvedor.resolver(request.arguments());
		} catch (IllegalArgumentException | IOException e) {
			return resultado(e.getMessage(), true);
		}

		try {
			// a factory olha o cabeçalho do arquivo e escolhe o leitor certo
			List<Transacao> transacoes = LeitorDeExtratoFactory.para(arquivo).ler(arquivo);
			return resultado(formatador.formatar(calculadora.calcular(transacoes)), false);
		} catch (IOException e) {
			return resultado("Não foi possível ler o arquivo: " + e.getMessage(), true);
		} catch (IllegalStateException e) {
			// o ClassificadorPorIA lança este erro quando a IA não responde (sem internet,
			// chave errada, servidor fora do ar)
			return resultado("A IA não conseguiu classificar as compras: " + e.getMessage(), true);
		} catch (IllegalArgumentException e) {
			// data ou valor que não dá para converter, ou coluna que falta no cabeçalho
			return resultado("O arquivo não está num formato de extrato reconhecido: " + e.getMessage(), true);
		}
	}

	// isError(true) avisa o cliente que deu problema, com o motivo em texto
	private CallToolResult resultado(String mensagem, boolean erro) {
		return CallToolResult.builder().content(List.of(TextContent.builder(mensagem).build())).isError(erro).build();
	}
}