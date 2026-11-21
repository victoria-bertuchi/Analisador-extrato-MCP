package br.edu.ifpr.mcp_server.mcp.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import br.edu.ifpr.mcp_server.extrato.CalculadoraResumoDoMes;
import br.edu.ifpr.mcp_server.extrato.FormatadorResumoDoMes;
import br.edu.ifpr.mcp_server.extrato.LeitorExtratoCsv;
import br.edu.ifpr.mcp_server.extrato.Transacao;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
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
 * Recebe o caminho de um extrato em CSV e devolve o total gasto, o maior gasto e a média por dia. É só a "cola" entre as peças: o leitor lê o arquivo, a calculadora faz as contas e o formatador monta o texto.
 */
public class ResumoDoMesTool implements McpToolDefinition {

   private static final String NOME_FERRAMENTA = "resumo_do_mes";
   //mesmo nome de parâmetro da ferramenta gastos_por_categoria
   private static final String ATRIBUTO_CAMINHO = "caminho_arquivo";
   private static final String SEM_GASTOS = "Nenhum gasto encontrado no extrato.";

   private final LeitorExtratoCsv leitor;
   private final CalculadoraResumoDoMes calculadora;
   private final FormatadorResumoDoMes formatador;

   //usado pelo servidor
   public ResumoDoMesTool() {
      this(new LeitorExtratoCsv(), new CalculadoraResumoDoMes(), new FormatadorResumoDoMes());
   }

   //as peças chegam prontas pelo construtor: a ferramenta não decide como elas são criadas
   public ResumoDoMesTool(LeitorExtratoCsv leitor,
                          CalculadoraResumoDoMes calculadora,
                          FormatadorResumoDoMes formatador) {
      this.leitor = leitor;
      this.calculadora = calculadora;
      this.formatador = formatador;
   }

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
      //diz ao cliente que a ferramenta recebe um texto obrigatório chamado "caminho_arquivo"
      Map<String, Object> inputSchema =
            Map.of("type", "object",
                   "properties", Map.of(ATRIBUTO_CAMINHO,
                                        Map.of("type", "string",
                                               "description", "Caminho completo do arquivo CSV do extrato")),
                   "required", List.of(ATRIBUTO_CAMINHO),
                   "additionalProperties", false);

      Tool tool = Tool.builder(NOME_FERRAMENTA, inputSchema)
                      .title("Resumo do mês")
                      .description("Lê um extrato bancário em CSV e devolve o total gasto, "
                                 + "o maior gasto e a média de gasto por dia")
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
      String caminho = lerCaminho(request);

      //confere o arquivo antes de começar, para devolver uma mensagem clara em vez de quebrar
      Optional<String> problema = validarCaminho(caminho);
      if (problema.isPresent()) {
         return resultado(problema.get(), true);
      }

      try {
         List<Transacao> transacoes = leitor.ler(Path.of(caminho));

         String texto = calculadora.calcular(transacoes)
                                   .map(formatador::formatar)
                                   .orElse(SEM_GASTOS);
         return resultado(texto, false);
      }
      catch (IOException e) {
         return resultado("Não foi possível ler o arquivo: " + e.getMessage(), true);
      }
      catch (DateTimeParseException | NumberFormatException | ArrayIndexOutOfBoundsException e) {
         //data ou valor que não dá para converter, ou coluna faltando: a linha não está em date,title,amount
         return resultado("O arquivo não está no formato esperado (date,title,amount): " + e.getMessage(), true);
      }
   }

   //devolve null se o parâmetro não veio ou não é texto
   private String lerCaminho(CallToolRequest request) {
      Map<String, Object> argumentos = request.arguments();
      if (argumentos == null) {
         return null;
      }
      return argumentos.get(ATRIBUTO_CAMINHO) instanceof String texto ? texto : null;
   }

   //devolve a mensagem de erro, ou vazio se o caminho está bom
   private Optional<String> validarCaminho(String caminho) {
      if (caminho == null || caminho.isBlank()) {
         return Optional.of("Informe o caminho do arquivo CSV do extrato.");
      }
      try {
         if (!Files.exists(Path.of(caminho))) {
            return Optional.of("Arquivo não encontrado: " + caminho);
         }
      }
      catch (InvalidPathException e) {
         return Optional.of("Caminho inválido: " + caminho);
      }
      if (!caminho.toLowerCase().endsWith(".csv")) {
         return Optional.of("O arquivo precisa ser .csv: " + caminho);
      }
      return Optional.empty();
   }

   //isError(true) avisa o cliente que deu problema, com o motivo em texto
   private CallToolResult resultado(String mensagem, boolean erro) {
      return CallToolResult.builder()
                           .content(List.of(TextContent.builder(mensagem).build()))
                           .isError(erro)
                           .build();
   }
}
