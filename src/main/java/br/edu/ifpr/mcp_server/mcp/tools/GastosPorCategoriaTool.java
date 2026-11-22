package br.edu.ifpr.mcp_server.mcp.tools;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import br.edu.ifpr.mcp_server.extrato.CalculadoraGastosPorCategoria;
import br.edu.ifpr.mcp_server.extrato.Categoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorDeCategoria;
import br.edu.ifpr.mcp_server.extrato.ClassificadorPorIA;
import br.edu.ifpr.mcp_server.extrato.LeitorDeExtratoFactory;
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
 * Recebe um extrato em CSV (pelo caminho ou anexado no chat), classifica cada compra com a IA (Gemini)
 * e devolve quanto foi gasto em cada categoria.
 */
public class GastosPorCategoriaTool implements McpToolDefinition {

   private static final String NOME_FERRAMENTA = "gastos_por_categoria";
   private static final String ATRIBUTO_ARQUIVO = "arquivo";
   private static final String ATRIBUTO_CONTEUDO = "conteudo";

   @Override
   public void register(SyncSpecification<SingleSessionSyncSpecification> mcp) throws Exception {
      //a ferramenta aceita o caminho do arquivo OU o texto do CSV (quando o arquivo é anexado no chat)
      Map<String, Object> inputSchema =
            Map.of("type", "object",
                   "properties", Map.of(
                         ATRIBUTO_ARQUIVO, Map.of("type", "string",
                               "description", "Caminho completo do arquivo CSV do extrato no computador"),
                         ATRIBUTO_CONTEUDO, Map.of("type", "string",
                               "description", "Texto completo do CSV, linha por linha, quando o usuário anexar o arquivo no chat")),
                   "additionalProperties", false);

      Tool tool = Tool.builder(NOME_FERRAMENTA, inputSchema)
                      .title("Gastos por categoria")
                      .description("Lê um extrato bancário em CSV, classifica cada compra com IA "
                                 + "e devolve o total gasto em cada categoria. "
                                 + "Se o usuário anexar o CSV no chat, passe o texto completo do arquivo em 'conteudo', "
                                 + "sem alterar nenhuma linha. Se ele informar um caminho, use 'arquivo'.")
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
      Path arquivo;
      try {
         arquivo = arquivoRecebido(request.arguments());
      }
      catch (IllegalArgumentException | IOException e) {
         return erro(e.getMessage());
      }

      //a chave da IA vem da variável de ambiente, nunca do código
      String chave = System.getenv("GEMINI_API_KEY");
      if (chave == null || chave.isBlank()) {
         return erro("A variável de ambiente GEMINI_API_KEY não está configurada. Veja o README.");
      }

      try {
         //a factory olha o cabeçalho do arquivo e escolhe o leitor certo
         List<Transacao> transacoes = LeitorDeExtratoFactory.para(arquivo).ler(arquivo);

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
         //o ClienteGemini lança este erro quando a IA não responde (sem internet, chave errada, servidor fora do ar)
         return erro("A IA não conseguiu classificar as compras: " + e.getMessage());
      }
      catch (RuntimeException e) {
         //data ou valor que não dá para converter, ou coluna que falta no cabeçalho
         return erro("O arquivo não está num formato de extrato reconhecido: " + e.getMessage());
      }
   }

   //devolve o arquivo a ser lido: o caminho informado ou, se veio o texto do CSV, um arquivo temporário com ele
   private Path arquivoRecebido(Map<String, Object> argumentos) throws IOException {
      if (argumentos.get(ATRIBUTO_CONTEUDO) instanceof String conteudo && !conteudo.isBlank()) {
         Path temporario = Files.createTempFile("extrato", ".csv");
         Files.writeString(temporario, conteudo, StandardCharsets.UTF_8);
         temporario.toFile().deleteOnExit();
         return temporario;
      }
      if (!(argumentos.get(ATRIBUTO_ARQUIVO) instanceof String caminho) || caminho.isBlank()) {
         throw new IllegalArgumentException("Informe o caminho do arquivo CSV ou anexe o arquivo no chat.");
      }
      Path arquivo = Path.of(caminho.replace("\"", "").trim());
      if (!Files.exists(arquivo)) {
         throw new IllegalArgumentException("Arquivo não encontrado: " + caminho);
      }
      if (!arquivo.toString().toLowerCase().endsWith(".csv")) {
         throw new IllegalArgumentException("O arquivo precisa ser .csv: " + caminho);
      }
      return arquivo;
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