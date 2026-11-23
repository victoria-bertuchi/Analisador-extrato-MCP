# 💳 Analisador de Extrato Bancário: Servidor MCP

Servidor MCP (Model Context Protocol) desenvolvido em Java que permite a assistentes de IA, como o Claude Desktop e o Gemini CLI, analisar extratos bancários, categorizando gastos com IA (Google Gemini) e gerando um resumo mensal para auxiliar no controle financeiro pessoal.

O projeto contém duas aplicações:
- **Servidor MCP:** disponibiliza as ferramentas de análise de extrato.
- **Cliente MCP:** aplicação de terminal capaz de listar e executar todas as ferramentas do servidor.

Trabalho final da disciplina de Serviços Web (IFPR).

## 👩‍💻 Autoras
- Fernanda Yumi Tomoi
- Victoria Camile Campos Bertuchi

## 🧰 Tecnologias
- Java 25
- Apache Maven
- MCP Java SDK
- Google Gemini (classificação dos gastos)
- JUnit (testes unitários e de integração)

## 🛠️ Ferramentas

| Ferramenta | O que faz |
|---|---|
| `gastos_por_categoria` | Lê o extrato, classifica cada compra (Delivery, Transporte, Mercado...) e devolve o total gasto em cada categoria. Usa o Gemini; sem chave, usa um classificador local por palavras-chave. |
| `resumo_do_mes` | Lê o extrato e devolve o total gasto, o maior gasto e a média por dia. Não usa IA. |

As duas ferramentas aceitam os mesmos parâmetros e devolvem os mesmos erros de leitura de arquivo.

### Parâmetros (das duas ferramentas)

Informe **um** dos dois:

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `arquivo` | texto | Caminho completo do arquivo CSV no computador. Ex.: `C:\Users\Usuario\Downloads\extrato.csv` (Windows) ou `/home/usuario/extrato.csv` (Linux/Mac) |
| `conteudo` | texto | O texto completo do CSV. Usado pelo Claude Desktop quando o arquivo é anexado no chat. |

Se os dois forem informados, `conteudo` é usado. Os dois são opcionais no esquema, mas um deles é necessário para a ferramenta funcionar.

### Ferramenta `gastos_por_categoria`

Resultado (sucesso):

```
Gastos por categoria:
- DELIVERY: R$ 45,90
- TRANSPORTE: R$ 18,50
- STREAMING: R$ 39,90
Total: R$ 104,30
```

Só entram as compras (valores que saíram da conta). Entradas e linhas de saldo são ignoradas.
Se o extrato não tiver nenhuma compra, a resposta é `Nenhum gasto encontrado no extrato.` (sem erro).

**Possíveis erros** (a ferramenta responde com `isError = true` e uma mensagem):

| Mensagem | Quando acontece |
|---|---|
| `Informe o caminho do arquivo CSV ou anexe o arquivo no chat.` | Nenhum parâmetro foi enviado. |
| `Arquivo não encontrado: ...` | O caminho informado não existe. |
| `O arquivo precisa ser .csv: ...` | O arquivo não termina em `.csv`. |
| `Não foi possível ler o arquivo: ...` | Erro ao abrir o arquivo (permissão, arquivo em uso...). |
| `O arquivo não está num formato de extrato reconhecido: ...` | Falta coluna de data, descrição ou valor; ou data/valor em formato desconhecido. |
| `A IA não conseguiu classificar as compras: ...` | Com a chave configurada: sem internet, chave inválida ou Gemini fora do ar (o servidor tenta até 3 vezes quando o Gemini está ocupado). |

### Ferramenta `resumo_do_mes`

Resultado (sucesso), para o arquivo `src/test/resources/extratos/fatura_nubank.csv`:

```
Resumo do extrato (01/11/2026 a 04/11/2026)
Total gasto: R$ 104,30 em 3 compras
Maior gasto: R$ 45,90 em 01/11/2026 (Ifd*Saiko Sushi Comerc)
Média por dia: R$ 26,08
```

- O período vai da primeira à última data do extrato (inclusive estornos) e a média é o total dividido pelos dias do período.
- Se o extrato não tiver nenhuma compra, a resposta é `Nenhum gasto encontrado no extrato.` (sem erro).

**Possíveis erros:** os mesmos de `gastos_por_categoria`, exceto os da IA (`isError = true`).

## Requisitos

- **Java 25 (JDK)**. Confira com `java -version`.
- **Maven** (ou o Maven que já vem no Eclipse). Confira com `mvn -v`: deve mostrar Java 25.
- **Eclipse** com suporte ao Java 25 (2025-12 ou mais novo). Em **Window → Preferences → Java → Installed JREs**, deixe o JDK 25 como padrão. Para importar: **File → Import → Maven → Existing Maven Projects**.
- **Node.js** (para o MCP Inspector e o Gemini CLI).
- **Claude Desktop** (teste com o assistente).
- Opcional: uma **chave da API do Gemini** (gratuita). Sem ela, `gastos_por_categoria` usa o classificador local.

### Como conseguir a chave do Gemini

1. Acesse https://aistudio.google.com e entre com uma conta Google.
2. Clique em **Get API key** → **Create API key**.
3. Copie a chave. Ela **nunca** vai no código: o servidor lê a chave da variável de ambiente `GEMINI_API_KEY` (em `McpServer01SyncStdio`). Sem a variável, o servidor usa o classificador local e avisa no log.

Variáveis opcionais: `GEMINI_MODEL` (padrão `gemini-flash-lite-latest`) e `GEMINI_URL` (endereço da API).

### Onde colocar a chave

A chave precisa estar na variável `GEMINI_API_KEY` do lugar onde o servidor vai rodar:

| Onde o servidor roda | Onde colocar a chave |
|---|---|
| Claude Desktop | No `claude_desktop_config.json`, dentro de `"env"` (ver **Claude Desktop**). |
| Gemini CLI | No `settings.json`, dentro de `"env"` (ver **Gemini CLI**). |
| Cliente MCP ou MCP Inspector (terminal) | No mesmo terminal, antes do comando: Windows `set GEMINI_API_KEY=sua_chave`; Linux/Mac `export GEMINI_API_KEY=sua_chave` (vale só para aquele terminal). |
| Eclipse (Run As → Java Application) | **Run → Run Configurations...** → **Java Application** → a classe que você executa (ex.: `McpClient01SyncStdio`) → aba **Environment** → **Add...** → Name: `GEMINI_API_KEY`, Value: sua chave → **Apply**. |
| Eclipse (testes JUnit com a IA) | **Run → Run Configurations...** → **JUnit** → o teste (ex.: `GastosPorCategoriaToolIT`) → aba **Environment** → **Add...** → `GEMINI_API_KEY` e sua chave → **Apply**. Sem a chave, os 2 testes com a IA aparecem como ignorados. |
| Eclipse (Maven build) | Na janela do **Maven build...** → aba **Environment** → **Add...** → `GEMINI_API_KEY` e sua chave. |

Essas configurações ficam só no seu computador e não vão para o GitHub.

## Builds

Todos os builds podem ser feitos no Eclipse (botão direito no projeto → **Run As → Maven build...**, campo **Goals**, e na aba **JRE**, em **VM arguments**: `--enable-native-access=ALL-UNNAMED`) ou no terminal, na pasta do projeto (onde está o `pom.xml`).

| Objetivo | Terminal / Goals no Eclipse | Resultado |
|---|---|---|
| Testes unitários | `mvn clean test` | Roda as classes `*Test` |
| Gerar o servidor | `mvn clean package -Pserver -DskipTests` | `server/mcp_server01_server.jar` (+ `server/libs/`) |
| Testes de integração (e unitários) | `mvn clean verify -Pserver` | Roda as classes `*Test`, gera o servidor e roda as classes `*IT` contra ele |
| Gerar o cliente | `mvn clean package -Pclient -DskipTests` | `target/dist-client/mcp_server01_client.jar` (+ `libs/`) |

No Eclipse, os goals são os mesmos, sem a palavra `mvn` (ex.: `clean verify -Pserver`).

Os testes de integração também podem ser rodados no Eclipse, depois de gerar o servidor: botão direito na classe `McpServer01IT` (ou `GastosPorCategoriaToolIT`) → **Run As → JUnit Test**.

Gere o servidor **antes** do cliente: o `clean` apaga a pasta `target`, onde fica o cliente.

**Antes de gerar o servidor, feche o Claude Desktop e o MCP Inspector**: se o servidor antigo estiver em uso, o Windows não deixa o Maven apagar a pasta `server`.

## Cliente MCP

O cliente fica em `br.edu.ifpr.mcp_server.main.McpClient01SyncStdio`. Ele sobe o servidor, mostra um menu com as ferramentas disponíveis, pede os parâmetros de cada uma (lidos do esquema JSON da ferramenta) e executa a escolhida. Por isso ele executa qualquer ferramenta do servidor. Ele repassa a variável `GEMINI_API_KEY` ao servidor, se ela existir.

O cliente procura o JAR do servidor, nesta ordem: variável `MCP_SERVER_JAR`; `server/mcp_server01_server.jar` (pasta do projeto); `../../server/mcp_server01_server.jar` (quando executado dentro de `target/dist-client`).

Depois de gerar o servidor e o cliente (ver **Builds**), no terminal, a partir da pasta do projeto:

```
set GEMINI_API_KEY=sua_chave          (opcional; Linux/Mac: export GEMINI_API_KEY=sua_chave)
cd target\dist-client                 (Linux/Mac: cd target/dist-client)
java -jar mcp_server01_client.jar
```

O cliente mostra as ferramentas numeradas. Digite o número (ou o nome) da ferramenta. Quando pedir `conteudo`, só aperte ENTER (é opcional); quando pedir `arquivo`, informe o caminho completo do CSV. Os comandos `listar` (atualiza a lista) e `sair` (encerra) também estão disponíveis. O cliente espera até 5 minutos pela resposta de cada ferramenta.

## MCP Inspector

Requer Node.js. Na pasta do projeto, depois de gerar o servidor:

```
set GEMINI_API_KEY=sua_chave
npx @modelcontextprotocol/inspector -e GEMINI_API_KEY=%GEMINI_API_KEY% java -jar server\mcp_server01_server.jar
```

(Linux/Mac: `export GEMINI_API_KEY=sua_chave` e `npx @modelcontextprotocol/inspector -e GEMINI_API_KEY=$GEMINI_API_KEY java -jar server/mcp_server01_server.jar`. Sem chave, tire o `-e ...`.)

O `-e` repassa a chave para o servidor aberto pelo Inspector. O Inspector abre no navegador: **Connect** → aba **Tools** → **List Tools** → escolha a ferramenta → preencha `arquivo` com o caminho do CSV → **Run Tool**.

Também dá para usar o Docker (`docker compose up --build`): o Inspector abre em http://127.0.0.1:6274 e os CSVs de exemplo ficam em `/data` dentro do contêiner (ex.: `arquivo` = `/data/fatura_nubank.csv`). A variável `GEMINI_API_KEY` do seu computador é repassada ao contêiner.

## Claude Desktop

1. Abra o Claude Desktop → **Configurações → Aplicativo para desktop → Extensões → Gerenciar → Servidores MCP locais → Editar configuração**.
   (Em outras versões: Configurações → Desenvolvedor → Editar configuração.)
2. Isso abre o arquivo `claude_desktop_config.json`. Acrescente o bloco `mcpServers` (troque os caminhos e a chave pelos seus; sem chave, apague o bloco `env`):

```json
{
  "mcpServers": {
    "analisador_extrato": {
      "command": "C:\\Program Files\\Java\\jdk-25\\bin\\java.exe",
      "args": [
        "-jar",
        "C:\\caminho\\do\\projeto\\server\\mcp_server01_server.jar"
      ],
      "env": {
        "GEMINI_API_KEY": "COLE_SUA_CHAVE_AQUI"
      }
    }
  }
}
```

- No Windows, as barras do caminho são duplas (`\\`). No Linux/Mac, use `/`.
- `command` é o caminho do `java` do JDK 25 (`where java` no Windows, `which java` no Linux/Mac).
- Se o arquivo já tiver outras configurações, coloque o bloco `"mcpServers": {...},` logo depois da primeira `{`.

3. **Feche o Claude Desktop por completo** (inclusive o ícone perto do relógio) e abra de novo.
4. Na lista de Servidores MCP locais, o `analisador_extrato` deve aparecer como **Em execução**.

### Como usar

No chat do Claude Desktop, há dois jeitos:

1. **Anexar o CSV** no chat e perguntar:
   > Qual o resumo do mês deste extrato? E quais foram meus gastos por categoria?

   O Claude passa o texto do arquivo para a ferramenta (campo `conteudo`).

2. **Informar o caminho** do arquivo (melhor para extratos grandes):
   > Use a ferramenta gastos_por_categoria com o arquivo C:\Users\SeuUsuario\Downloads\extrato.csv

No Windows, para copiar o caminho: Shift + botão direito no arquivo → Copiar como caminho.

As compras são enviadas à IA em lotes de até 50 por pergunta, então mesmo extratos grandes são classificados com poucas chamadas.

## Gemini CLI (outro assistente de IA)

O servidor também funciona no [Gemini CLI](https://github.com/google-gemini/gemini-cli), o assistente de terminal do Google.

1. Instale (precisa do Node.js): `npm install -g @google/gemini-cli`. Rode `gemini` uma vez para autenticar (login Google ou chave da API).
2. Gere o servidor (ver **Builds**) e crie/edite o arquivo de configuração do Gemini CLI: `%USERPROFILE%\.gemini\settings.json` (Windows) ou `~/.gemini/settings.json` (Linux/Mac). Também vale um `.gemini/settings.json` dentro da pasta do projeto.
3. Acrescente o servidor (aqui as barras são normais, `/`, inclusive no Windows):

```json
{
  "mcpServers": {
    "analisador-extrato": {
      "command": "java",
      "args": ["-jar", "C:/caminho/do/projeto/server/mcp_server01_server.jar"],
      "env": { "GEMINI_API_KEY": "$GEMINI_API_KEY" },
      "timeout": 60000
    }
  }
}
```

O Gemini CLI remove variáveis sensíveis do ambiente ao iniciar servidores MCP, por isso a chave precisa estar listada em `env`; o valor `$GEMINI_API_KEY` é lido da variável do seu terminal. Sem chave, apague a linha `env`.

4. Execute `gemini` e digite `/mcp`: o servidor `analisador-extrato` deve aparecer conectado, com as ferramentas `gastos_por_categoria` e `resumo_do_mes`.
5. Peça em linguagem natural, informando o caminho do arquivo (o Gemini CLI não anexa o arquivo como o Claude Desktop):
   > Use a ferramenta resumo_do_mes com o arquivo C:/extratos/fatura_nubank.csv

O Gemini CLI pede confirmação antes de executar cada ferramenta.

## Formatos de extrato aceitos

O servidor descobre sozinho o formato olhando a primeira linha (cabeçalho) do arquivo. Arquivos salvos pelo Excel como "CSV UTF-8" (com BOM) também são aceitos.

**1. CSV do Nubank** (cabeçalho `date,title,amount`)

```
date,title,amount
2026-11-01,Ifd*Saiko Sushi Comerc,45.90
2026-11-02,Uber *Trip,18.50
```

Nesse formato, o gasto vem **positivo**.

**2. Extrato de conta (formato genérico)**

As colunas são encontradas **pelo nome**, em qualquer ordem:

- Data: `Data`, `Date`, `Data Lançamento`
- Descrição: `Descrição`, `Histórico`, `Lançamento`, `Detalhes`, `Title` (se houver mais de uma, elas são juntadas)
- Valor: `Valor`, `Amount`, `Valor (R$)`
- Tipo (opcional): `Tipo Lançamento` / `Tipo` com `Entrada` ou `Saída`

Também aceita:
- separador `,` ou `;`;
- valor `1.234,56` ou `1234.56`;
- datas `05/11/2026`, `5/11/2026`, `05-11-2026`, `2026-11-05`, `2026/11/05`;
- linhas antes do cabeçalho (nome, agência, conta);
- arquivo salvo em UTF-8 (com ou sem BOM) ou com acentuação do Windows;
- descrição entre aspas com vírgula ou quebra de linha dentro;
- linhas de rodapé, totais ou sem data (são ignoradas).

No extrato de conta, o gasto vem **negativo** (o dinheiro saiu da conta). Quando existe a coluna de tipo, ela é que decide: só as linhas de **Saída** contam como gasto. Linhas de saldo ("Saldo Anterior", "Saldo do dia") e entradas (Pix recebido, salário) são ignoradas.

Exemplo (Banco do Brasil):

```
Data,"Lançamento","Detalhes","N° documento","Valor","Tipo Lançamento"
10/11/2026,"Compra com Cartão","10/11 11:02 LOJA EXEMPLO","167349","-10,00","Saída"
10/11/2026,"Saldo do dia","","","924,33",""
```

## Categorias

`DELIVERY`, `TRANSPORTE`, `MERCADO`, `FARMACIA`, `SAUDE`, `STREAMING`, `ALIMENTACAO`, `EDUCACAO`, `LAZER`, `VESTUARIO`, `CONTAS_DA_CASA`, `PET`, `OUTROS`

## Arquitetura e padrões de projeto

Pacotes: `br.edu.ifpr.mcp_server.extrato` (regras do extrato), `...ia` (comunicação com a IA), `...mcp.tools` e `...mcp.utils` (ferramentas MCP e apoio) e `...main` (servidor e cliente).

| Classe | Papel |
|---|---|
| `Transacao` | Uma linha do extrato: data, valor e descrição. Sabe dizer se é uma compra (`ehCompra()`). |
| `ResumoDoMes` | Resultado do resumo: só guarda os números. |
| `Categoria` | As categorias possíveis. |
| `LeitorDeExtrato` | Interface: lê um arquivo e devolve a lista de transações. |
| `LeitorExtratoCsvNubank` | Lê o CSV do Nubank (`date,title,amount`). |
| `LeitorExtratoCsvGenerico` | Lê extratos de conta, achando as colunas pelo nome. |
| `LeitorDeExtratoFactory` | Olha o cabeçalho e escolhe o leitor certo. |
| `ClassificadorDeCategoria` | Interface: recebe uma transação (ou uma lista delas) e devolve a categoria. |
| `ClassificadorPorIA` | Classifica usando a IA, mandando até 50 lançamentos por pergunta. |
| `ClassificadorPorPalavrasChave` | Classifica por palavras na descrição, sem internet. Usado quando não há chave. |
| `ClienteIA` | Interface: manda uma pergunta para a IA e devolve a resposta. |
| `ClienteGemini` | Implementação com o Google Gemini (tenta de novo até 3 vezes se o servidor estiver ocupado). |
| `CalculadoraGastosPorCategoria` | Soma os gastos de cada categoria (ignora entradas). |
| `CalculadoraResumoDoMes` | Calcula total, maior gasto e média por dia. |
| `FormatadorDeMoeda`, `FormatadorResumoDoMes`, `FormatadorGastosPorCategoria` | Montam os textos das respostas (R$ 1.234,50 e dd/MM/aaaa). |
| `ResolvedorDeArquivoDoExtrato` | Descobre o arquivo a ler a partir dos parâmetros `arquivo` ou `conteudo` (usado pelas duas ferramentas). |
| `GastosPorCategoriaTool`, `ResumoDoMesTool` | As ferramentas MCP: juntam as peças e devolvem a resposta. |
| `McpServer01SyncStdio` | O servidor: escolhe o classificador, liga as peças e registra as ferramentas. |
| `McpClient01SyncStdio` | O cliente de terminal. |

**Strategy**
- `LeitorDeExtrato`: dois leitores fazem a mesma tarefa de jeitos diferentes. No CSV do Nubank o gasto é positivo; no extrato de conta, negativo. O genérico atende os bancos que seguem a mesma convenção, sem repetir código (DRY).
- `ClassificadorDeCategoria`: a calculadora recebe "um classificador" sem saber qual é. Há duas implementações reais (IA e palavras-chave) e, nos testes, um classificador falso.
- `ClienteIA`: dá para trocar o Gemini por outra IA criando uma nova classe, sem mexer no classificador.

**Factory**
- `LeitorDeExtratoFactory`: quem usa não precisa saber de qual banco é o extrato; a factory decide pelo cabeçalho.

**Injeção de dependência (pelo construtor)**
- `new CalculadoraGastosPorCategoria(classificador)`, `new ClassificadorPorIA(cliente)`, `new GastosPorCategoriaTool(resolvedor, calculadora, formatador)`: cada classe recebe pronto o que precisa, em vez de criar por dentro. Quem monta tudo é o `McpServer01SyncStdio`. É isso que permite testar com objetos falsos.

**SOLID**
- **S** (responsabilidade única): ler, classificar, falar com a IA, somar e formatar estão em classes separadas.
- **O** (aberto/fechado): para aceitar um formato novo de extrato, cria-se um novo `LeitorDeExtrato` e ensina-se a factory a reconhecê-lo, sem alterar as classes que já existem.
- **D** (inversão de dependência): as classes dependem das interfaces (`ClassificadorDeCategoria`, `ClienteIA`, `LeitorDeExtrato`), não das implementações.

## Testes

Os testes ficam em `src/test/java` e usam JUnit.

**Testes unitários** (`*Test`, rodam sem internet e sem chave):

| Classe | O que verifica |
|---|---|
| `TransacaoTest` | `ehCompra()` com valor positivo, zero e negativo; campos nulos rejeitados. |
| `LeitorExtratoCsvGenericoTest` | Formato do Banco do Brasil (Entrada/Saída), junção das colunas de descrição, linhas de saldo ignoradas, separador `;` e valor `1.234,56`, colunas em qualquer ordem, linhas antes do cabeçalho, os 4 formatos de data, arquivo com acentuação do Windows, arquivo vazio, coluna faltando e data inválida. |
| `LeitorExtratoCsvNubankTest` | Leitura do CSV do Nubank mantendo o sinal do valor. |
| `LeitorExtratoCsvComBomTest` | CSV com BOM (Excel) é lido, e a factory ainda reconhece o Nubank. |
| `LeitorDeExtratoFactoryTest` | A factory escolhe o leitor Nubank para `date,title,amount` e o genérico para os outros cabeçalhos (inclusive arquivo vazio). |
| `ClassificadorPorIATest` | A resposta da IA vira a categoria certa; resposta inválida e erro da IA. Usa uma IA falsa. |
| `ClassificadorPorPalavrasChaveTest` | Palavras-chave de cada tipo de gasto, com acento e sem acento, e `OUTROS`. |
| `ClienteGeminiTest` | Contra um servidor HTTP falso: resposta 200, chave enviada, nova tentativa em 429, desistência em 503 e erro imediato em 401. |
| `CalculadoraGastosPorCategoriaTest` | Soma por categoria, categorias separadas e entradas ignoradas. Usa um classificador falso. |
| `CalculadoraResumoDoMesTest` | Lista vazia, só estornos, valor zero, resumo completo, período com estorno, empate no maior gasto, um dia só e arredondamento. |
| `FormatadorResumoDoMesTest` | Formato brasileiro de valores e datas; singular e plural de "compra". |
| `ResolvedorDeArquivoDoExtratoTest` | Argumentos nulos/vazios, conteúdo vira arquivo temporário, prioridade do conteúdo, caminho entre aspas, arquivo inexistente e não `.csv`. |
| `ResumoDoMesToolTest` | A ferramenta pelo caminho e pelo conteúdo; sem gastos; sem parâmetros; arquivo inexistente; não `.csv`; formato inválido. |
| `GastosPorCategoriaToolTest` | A ferramenta com classificador falso: sucesso, sem gastos, erros de arquivo/formato e erro da IA. |

**Testes de integração** (`*IT`, sobem o servidor `server/mcp_server01_server.jar` e chamam as ferramentas pelo protocolo MCP):

| Classe | O que verifica |
|---|---|
| `McpServer01IT` | O servidor anuncia as ferramentas com descrição e esquema; `resumo_do_mes` pelo caminho e pelo conteúdo; erros sem parâmetros e com arquivo inexistente. |
| `GastosPorCategoriaToolIT` | A ferramenta é anunciada; erros sem parâmetros, arquivo inexistente e arquivo que não é CSV; sem chave, usa o classificador local; e, quando `GEMINI_API_KEY` existe no computador, a execução completa com a IA pelo caminho e pelo conteúdo. |

Os dois testes que usam a IA de verdade só rodam se a variável `GEMINI_API_KEY` existir no ambiente; sem ela, aparecem como ignorados. O arquivo de exemplo usado está em `src/test/resources/extratos/fatura_nubank.csv`.

Para testar a IA na mão: execute `ExecucaoManualIA` (em `src/test/java/.../ia`) com **Run As → Java Application**, com a variável `GEMINI_API_KEY` configurada.

## Segurança e privacidade

- A chave da API **nunca** fica no código nem no GitHub. Ela é lida da variável de ambiente `GEMINI_API_KEY`, configurada no `claude_desktop_config.json` ou no `settings.json` de cada computador.
- Com a chave configurada, as **descrições das transações** (ex.: nomes de estabelecimentos) são enviadas ao Google (Gemini) para classificação. Valores e datas não são enviados. Sem a chave, nada sai do computador.
- As ferramentas leem qualquer arquivo `.csv` cujo caminho for informado: use o servidor apenas com assistentes em que você confia.

## Limitações conhecidas

- A classificação pela IA pode variar entre execuções em lançamentos ambíguos (ex.: Pix para um nome que pode ser pessoa ou empresa). Os valores e o total não mudam.
- Um Pix enviado para uma pessoa ("Pix - Enviado - Fulano") não diz o que foi comprado, então tende a ficar em `OUTROS`.
- O classificador local (sem chave) só conhece algumas palavras-chave; o que ele não reconhece vira `OUTROS`.
- Testado com extratos reais do Banco do Brasil, do Santander e do Nubank. Outros bancos funcionam se seguirem a convenção mais comum (débito negativo ou coluna Entrada/Saída). Um banco que traga o gasto positivo sem coluna de tipo seria lido sem erro, mas com total errado; para ele, basta criar um novo leitor e registrá-lo na factory.
- Extratos com valor em duas colunas ("Crédito" e "Débito") não são suportados.

## Melhorias futuras

- Leitores específicos para faturas de cartão de outros bancos.
- Suporte a extratos com valor em duas colunas ("Crédito" e "Débito").
- Leitura de outros formatos além de CSV (XLSX, OFX).
- Resources e prompts MCP (por exemplo, a lista de categorias como resource).