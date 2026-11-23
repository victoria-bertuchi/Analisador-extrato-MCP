package br.edu.ifpr.mcp_server.main;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.edu.ifpr.mcp_server.extrato.CalculadoraGastosPorCategoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorDeCategoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorPorIA;
import br.edu.ifpr.mcp_server.extrato.ClassificadorPorPalavrasChave;
import br.edu.ifpr.mcp_server.extrato.FormatadorGastosPorCategoria;
import br.edu.ifpr.mcp_server.ia.ClienteGemini;
import br.edu.ifpr.mcp_server.mcp.tools.GastosPorCategoriaTool;
import br.edu.ifpr.mcp_server.mcp.tools.ResumoDoMesTool;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import br.edu.ifpr.mcp_server.mcp.utils.ResolvedorDeArquivoDoExtrato;
import io.modelcontextprotocol.json.jackson3.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema.Implementation;
import io.modelcontextprotocol.spec.McpSchema.ServerCapabilities;
import tools.jackson.databind.json.JsonMapper;

/**
 * Servidor MCP do analisador de extrato (transporte STDIO). Aqui é o único
 * lugar que decide quais peças são usadas e como elas são ligadas (a "raiz de
 * composição"): as ferramentas só recebem o que precisam.
 */
public class McpServer01SyncStdio {

	private static final Logger LOG = LoggerFactory.getLogger(McpServer01SyncStdio.class);

	private static final String VARIAVEL_DA_CHAVE = "GEMINI_API_KEY";

	private McpSyncServer server;

	// escolhe o classificador: a IA, se houver chave; senão, o local (por
	// palavras-chave)
	private ClassificadorDeCategoria criarClassificador() {
		String chave = System.getenv(VARIAVEL_DA_CHAVE);
		if (chave == null || chave.isBlank()) {
			LOG.warn("{} não configurada: usando o classificador local por palavras-chave.", VARIAVEL_DA_CHAVE);
			return new ClassificadorPorPalavrasChave();
		}
		LOG.info("Classificação das compras feita pela IA (Gemini).");
		return new ClassificadorPorIA(new ClienteGemini(chave));
	}

	private void registerTools(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
		// cria a lista de ferramentas disponíveis no servidor...
		List<McpToolDefinition> tools = List.of(
				new GastosPorCategoriaTool(new ResolvedorDeArquivoDoExtrato(),
						new CalculadoraGastosPorCategoria(criarClassificador()), new FormatadorGastosPorCategoria()),
				new ResumoDoMesTool());
		// ...para cada ferramenta, executa seu registro para posterior execução
		for (McpToolDefinition tool : tools) {
			tool.register(mcp);
		}
	}

	private SyncSpecification<SingleSessionSyncSpecification> configMcpServer() {
		// cria o objeto para comunicação entre servidor e cliente
		JacksonMcpJsonMapper jsonMapper = new JacksonMcpJsonMapper(new JsonMapper());
		// estabelece que a comunicação usará o transporte STDIO
		StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(jsonMapper);
		// identifica quais "capacidades" o servidor disponibiliza, neste caso,
		// ferramentas e 'logging'
		ServerCapabilities capabilities = ServerCapabilities.builder().tools(true).logging().build();
		// identificação do servidor
		Implementation implementation = Implementation.builder("analisador-extrato", "1.0.0").build();

		// configura o servidor com transporte STDIO
		return McpServer.sync(transportProvider).serverInfo(implementation).capabilities(capabilities);
	}

	public McpServer01SyncStdio() throws Exception {
		// configura/inicializa o servidor
		SyncSpecification<SingleSessionSyncSpecification> srv = configMcpServer();

		// registra as ferramentas disponíveis neste servidor
		registerTools(srv);
		// cria o servidor
		this.server = srv.build();

		LOG.info("Servidor MCP analisador-extrato iniciado via STDIO...");
	}

	public static void main(String[] args) throws Exception {
		new McpServer01SyncStdio();
	}
}