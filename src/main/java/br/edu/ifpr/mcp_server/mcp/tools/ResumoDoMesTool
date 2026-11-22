package br.edu.ifpr.mcp_server.mcp.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.extrato.CalculadoraResumoDoMes;
import br.edu.ifpr.mcp_server.extrato.FormatadorResumoDoMes;
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
 * Ferramenta MCP "resumo_do_mes".
 * Recebe um extrato em CSV (pelo caminho ou anexado no chat) e devolve o total gasto,
 * o maior gasto e a média por dia. Não usa IA. É só a "cola" entre as peças: o resolvedor
 * descobre o arquivo, a factory escolhe o leitor, a calculadora faz as contas e o
 * formatador monta o texto.
 */
public class ResumoDoMesTool implements McpToolDefinition {

   private static final String NOME_FERRAMENTA = "resumo_do_mes";
   private static final String SEM_GASTOS = "Nenhum gasto encontrado no extrato.";

   private final ResolvedorDeArquivoDoExtrato resolvedor;
   private final CalculadoraResumoDoMes calculadora;
   private final FormatadorResumoDoMes formatador;

   //usado pelo servidor
   public ResumoDoMesTool() {
      this(new ResolvedorDeArquivoDoExtrato(), new CalculadoraResumoDoMes(), new FormatadorResumoDoMes());
   }

   //as peças chegam prontas pelo construtor: a ferramenta não decide como elas são criadas
   public ResumoDoMesTool(ResolvedorDeArquivoDoExtrato resolvedor,
                          CalculadoraResumoDoMes calculadora,
                          FormatadorResumoDoMes formatador) {
      this.resolvedor = resolvedor;
      this.calculadora = calculadora;
      this.formatador = formatador;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
      //os parâmetros (arquivo e conteudo) são os mesmos da ferramenta gastos_por_categoria
      Map<String, Object> inputSchema =
            Map.of("type", "object",
                   "properties", ResolvedorDeArquivoDoExtrato.propriedadesDoEsquema(),
                   "additionalProperties", false);

      Tool tool = Tool.builder(NOME_FERRAMENTA, inputSchema)
                      .title("Resumo do mês")
                      .description("Lê um extrato bancário em CSV e devolve o total gasto, o maior gasto "
                                 + "e a média de gasto por dia. "
                                 + "Se o usuário anexar o CSV no chat, passe o texto completo do arquivo em 'conteudo', "
                                 + "sem alterar nenhuma linha. Se ele informar um caminho, use 'arquivo'.")
                      .build();

      //quando o cliente chamar a ferramenta, o método run é executado
      SyncToolSpecification toolSpec = SyncToolSpecification.builder()
                                                             .tool(tool)
                                                             .callHandler(this::run)
                                                             .build();
      mcp.tools(toolSpec);
   }

   @Override
   public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
      Path arquivo;
      try {
         arquivo = resolvedor.resolver(request.arguments());
      }
      catch (IllegalArgumentException | IOException e) {
         return resultado(e.getMessage(), true);
      }

      try {
         //a factory olha o cabeçalho do arquivo e escolhe o leitor certo
         List<Transacao> transacoes = LeitorDeExtratoFactory.para(arquivo).ler(arquivo);

         String texto = calculadora.calcular(transacoes)
                                   .map(formatador::formatar)
                                   .orElse(SEM_GASTOS);
         return resultado(texto, false);
      }
      catch (IOException e) {
         return resultado("Não foi possível ler o arquivo: " + e.getMessage(), true);
      }
      catch (IllegalArgumentException e) {
         //data ou valor que não dá para converter, ou coluna que falta no cabeçalho
         return resultado("O arquivo não está num formato de extrato reconhecido: " + e.getMessage(), true);
      }
   }

   //isError(true) avisa o cliente que deu problema, com o motivo em texto
   private CallToolResult resultado(String mensagem, boolean erro) {
      return CallToolResult.builder()
                           .content(List.of(TextContent.builder(mensagem).build()))
                           .isError(erro)
                           .build();
   }
}
