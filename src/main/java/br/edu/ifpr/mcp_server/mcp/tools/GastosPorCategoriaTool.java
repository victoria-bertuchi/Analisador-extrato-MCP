package br.edu.ifpr.mcp_server.mcp.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.extrato.CalculadoraGastosPorCategoria;
import br.edu.ifpr.mcp_server.extrato.Categoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorDeCategoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorPorIA;
import br.edu.ifpr.mcp_server.extrato.LeitorExtratoCsv;
import br.edu.ifpr.mcp_server.extrato.Transacao;
import br.edu.ifpr.mcp_server.ia.ClienteGemini;
import br.edu.ifpr.mcp_server.mcp.utils.McpToolDefinition;
import io.modelcontextprotocol.server.McpServer.SingleSessionSyncSpecification;
import io.modelcontextprotocol.server.McpServer.SyncSpecification;
import io.modelcontextprotocol.server.McpServerFeatures.SyncToolSpecification;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.Tool;

/**
 * Ferramenta MCP "gastos_por_categoria".
 * Recebe o caminho de um extrato em CSV, classifica cada compra com a IA (Gemini)
 * e devolve quanto foi gasto em cada categoria.
 */
public class GastosPorCategoriaTool implements McpToolDefinition {

   private static final String NOME_FERRAMENTA = "gastos_por_categoria";
   private static final String ATRIBUTO_ARQUIVO = "arquivo";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
      //diz ao Claude que a ferramenta recebe um texto obrigatório chamado "arquivo"
      Map<String, Object> inputSchema =
            Map.of("type", "object",
                   "properties", Map.of(ATRIBUTO_ARQUIVO,
                                        Map.of("type", "string",
                                               "description", "Caminho completo do arquivo CSV do extrato")),
                   "required", List.of(ATRIBUTO_ARQUIVO),
                   "additionalProperties", false);

      Tool tool = Tool.builder(NOME_FERRAMENTA, inputSchema)
                      .title("Gastos por categoria")
                      .description("Lê um extrato bancário em CSV, classifica cada compra com IA "
                                 + "e devolve o total gasto em cada categoria")
                      .build();

      //quando o Claude chamar a ferramenta, o método run (abaixo) é executado
      SyncToolSpecification toolSpec = SyncToolSpecification.builder()
                                                             .tool(tool)
                                                             .callHandler(this::run)
                                                             .build();
      mcp.tools(toolSpec);
   }

   @Override
   public CallToolResult run(McpSyncServerExchange exchange, CallToolRequest request) {
      //confere o arquivo antes de começar, para devolver uma mensagem clara em vez de quebrar
      Object valorRecebido = request.arguments().get(ATRIBUTO_ARQUIVO);
      if (!(valorRecebido instanceof String caminho) || caminho.isBlank()) {
         return erro("Informe o caminho do arquivo CSV do extrato.");
      }
      Path arquivo = Path.of(caminho);
      if (!Files.exists(arquivo)) {
         return erro("Arquivo não encontrado: " + caminho);
      }
      if (!caminho.toLowerCase().endsWith(".csv")) {
         return erro("O arquivo precisa ser .csv: " + caminho);
      }

      //a chave da IA vem da variável de ambiente, nunca do código
      String chave = System.getenv("GEMINI_API_KEY");
      if (chave == null || chave.isBlank()) {
         return erro("A variável de ambiente GEMINI_API_KEY não está configurada. Veja o README.");
      }

      try {
         //lê o CSV e transforma cada linha numa Transacao
         List<Transacao> transacoes = new LeitorExtratoCsv().ler(arquivo);

         ClassificadorDeCategoria classificador = new ClassificadorPorIA(new ClienteGemini(chave));
         CalculadoraGastosPorCategoria calculadora = new CalculadoraGastosPorCategoria(classificador);
         Map<Categoria, BigDecimal> totais = calculadora.calcular(transacoes);

         return CallToolResult.builder()
                              .addTextContent(formatar(totais))
                              .build();
      }
      catch (IOException e) {
         return erro("Não foi possível ler o arquivo: " + e.getMessage());
      }
      catch (IllegalStateException e) {
         //o clienteGemini lança este erro quando a IA não responde (sem internet, chave errada, servidor fora do ar)
         return erro("A IA não conseguiu classificar as compras: " + e.getMessage());
      }
      catch (RuntimeException e) {
         //data ou valor que não dá para converter: a linha não está no formato date,title,amount
         return erro("O arquivo não está num formato de extrato reconhecido: " + e.getMessage());
      }
   }

   //monta o texto de resposta, uma linha por categoria, e o total no final
   private String formatar(Map<Categoria, BigDecimal> totais) {
      if (totais.isEmpty()) {
         return "Nenhum gasto encontrado no extrato.";
      }
      StringBuilder texto = new StringBuilder("Gastos por categoria:\n");
      BigDecimal totalGeral = BigDecimal.ZERO;
      for (Map.Entry<Categoria, BigDecimal> item : totais.entrySet()) {
         texto.append("- ").append(item.getKey()).append(": R$ ").append(item.getValue()).append("\n");
         totalGeral = totalGeral.add(item.getValue());
      }
      texto.append("Total: R$ ").append(totalGeral);
      return texto.toString();
   }

   //resposta de erro: isError(true) avisa o Claude que deu problema, com o motivo em texto
   private CallToolResult erro(String mensagem) {
      return CallToolResult.builder()
                           .addTextContent(mensagem)
                           .isError(true)
                           .build();
   }
}