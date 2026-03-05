package cookbooks.clinicamedica;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paragon.agents.Interactable;
import com.paragon.agents.InteractableBlueprint;
import com.paragon.agents.RouterAgent;
import com.paragon.agents.StructuredAgentResult;
import cookbooks.clinicamedica.models.ResultadoConsulta;
import cookbooks.clinicamedica.usecase.AtenderPacienteInput;
import cookbooks.clinicamedica.usecase.AtenderPacienteUseCase;
// import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Cookbook: Clínica Médica de Agentes
 *
 * <p>Este exemplo ensina os seguintes conceitos em um único fluxo coeso:
 *
 * <ol>
 *   <li><b>RouterAgent.Structured</b> — roteamento LLM-classificado com saída tipada
 *   <li><b>Agentes especializados</b> — carregados de blueprints YAML com ferramentas
 *   <li><b>Blueprints YAML</b> — {@link InteractableBlueprint#fromYaml(String)} e {@code toYaml()}
 *   <li><b>Persistência de agentes</b> — novo agente criado e salvo em disco
 *   <li><b>RouterAgent.Structured.of()</b> — factory pública para wrapping tipado
 *   <li><b>Padrão meta-agente</b> — agente cujo output type é {@code AgentDefinition}
 *   <li><b>Use case com Structured</b> — {@code AtenderPacienteUseCase<T>}
 *   <li><b>Fallback com capacidades especiais</b> — Administração com ferramenta de contratar
 * </ol>
 *
 * <p>Requisito: variável {@code OPENROUTER_API_KEY} no arquivo {@code .env} na raiz do projeto.
 *
 * <p>Para executar no IntelliJ, marque o diretório {@code cookbooks/clinica-medica} como
 * <em>Source Root</em> (ou ajuste o build-helper-maven-plugin) e execute este {@code main}.
 */
public class Main {

  public static void main(String[] args) throws IOException {
    // =========================================================================
    // 1. Credenciais
    // =========================================================================
    // Dotenv removido devido a erro nativo no JDK 25 neste ambiente local
    // O script de execução já faz `export OPENROUTER_API_KEY=...`
    String apiKey = System.getenv("OPENROUTER_API_KEY");

    if (apiKey == null || apiKey.isBlank()) {
      System.out.println("Erro: OPENROUTER_API_KEY nao encontrada no arquivo .env");
      return;
    }

    // =========================================================================
    // 2. Carregar o router completo de um único YAML
    //
    //    Conceito: triagem-clinica.yaml é auto-suficiente — contém o router,
    //    todos os especialistas e o fallback de Administração embutidos inline.
    //    Ferramentas são reconstruídas por reflexão via toolClassNames.
    // =========================================================================
    Path agentsDir = Paths.get("cookbooks/clinica-medica/agents");

    System.out.println("\n=== Carregando router de triagem-clinica.yaml ===");

    String routerYaml = Files.readString(agentsDir.resolve("triagem-clinica.yaml"));
    InteractableBlueprint blueprint = InteractableBlueprint.fromYaml(routerYaml);
    RouterAgent router = (RouterAgent) blueprint.toInteractable();

    System.out.println("  Router carregado: " + router.name());

    // =========================================================================
    // 3. Envolver como RouterAgent.Structured<ResultadoConsulta>
    //
    //    Conceito: RouterAgent.Structured.of() é a factory pública que permite
    //    wrapping tipado sem precisar construir o router via builder.
    // =========================================================================
    RouterAgent.Structured<ResultadoConsulta> roteador =
        RouterAgent.Structured.of(router, ResultadoConsulta.class, new ObjectMapper());

    System.out.println("  Wrapped como Structured<ResultadoConsulta>");

    // =========================================================================
    // 4. Criar o use case que encapsula o fluxo de atendimento
    // =========================================================================
    AtenderPacienteUseCase<ResultadoConsulta> useCase = new AtenderPacienteUseCase<>(roteador);

    // =========================================================================
    // DEMO 1: Paciente com dor no peito → deve ir ao Cardiologista
    // =========================================================================
    System.out.println("\n" + "=".repeat(60));
    System.out.println("DEMO 1: Paciente com dor no peito");
    System.out.println("=".repeat(60));

    AtenderPacienteInput paciente1 =
        new AtenderPacienteInput(
            "Joao Silva",
            58,
            "Sinto dor no peito ao subir escadas, com irradiacao para o braco esquerdo. "
                + "Ja faz 3 dias. Tenho historico de pressao alta.");

    System.out.println("Paciente: " + paciente1.nomePaciente() + " (" + paciente1.idadePaciente() + " anos)");
    System.out.println("Queixa: " + paciente1.queixa());
    System.out.println("\nProcessando...");

    StructuredAgentResult<ResultadoConsulta> resultado1 = useCase.executa(paciente1);
    exibirResultado(resultado1);

    // =========================================================================
    // DEMO 2: Paciente com fratura → deve ir ao Ortopedista
    // =========================================================================
    System.out.println("\n" + "=".repeat(60));
    System.out.println("DEMO 2: Paciente com suspeita de fratura");
    System.out.println("=".repeat(60));

    AtenderPacienteInput paciente2 =
        new AtenderPacienteInput(
            "Maria Oliveira",
            34,
            "Cai da bicicleta ha 2 horas e meu tornozelo incheu muito, esta roxo "
                + "e eu nao consigo apoiar o pe no chao. A dor e intensa.");

    System.out.println("Paciente: " + paciente2.nomePaciente() + " (" + paciente2.idadePaciente() + " anos)");
    System.out.println("Queixa: " + paciente2.queixa());
    System.out.println("\nProcessando...");

    StructuredAgentResult<ResultadoConsulta> resultado2 = useCase.executa(paciente2);
    exibirResultado(resultado2);

    // =========================================================================
    // DEMO 3: Paciente com queixa dermatológica → fallback Administracao
    //         → ContratarMedicoTool → salva novo YAML
    //
    //    Conceito: padrão meta-agente — o agente de Administracao usa um agente
    //    interno cujo output type é AgentDefinition para criar um novo especialista.
    // =========================================================================
    System.out.println("\n" + "=".repeat(60));
    System.out.println("DEMO 3: Queixa dermatologica → meta-agente cria novo especialista");
    System.out.println("=".repeat(60));

    AtenderPacienteInput paciente3 =
        new AtenderPacienteInput(
            "Pedro Santos",
            27,
            "Tenho manchas vermelhas espalhando pelo corpo ha uma semana, "
                + "com coceira intensa. As manchas pioram a noite.");

    System.out.println("Paciente: " + paciente3.nomePaciente() + " (" + paciente3.idadePaciente() + " anos)");
    System.out.println("Queixa: " + paciente3.queixa());
    System.out.println("\nProcessando (o agente de Administracao ira contratar um dermatologista)...");

    StructuredAgentResult<ResultadoConsulta> resultado3 = useCase.executa(paciente3);
    exibirResultado(resultado3);

    // =========================================================================
    // 5. Verificar e carregar o novo agente criado pelo meta-agente
    // =========================================================================
    System.out.println("\n=== Verificando blueprints criados pelo meta-agente ===");

    try (var stream = Files.list(agentsDir)) {
      stream
          .filter(p -> {
            String fn = p.getFileName().toString();
            return !fn.startsWith("clinico")
                && !fn.startsWith("cardiologista")
                && !fn.startsWith("ortopedista")
                && !fn.startsWith("administracao")
                && !fn.startsWith("triagem-clinica");
          })
          .forEach(
              yamlPath -> {
                System.out.println("\nNovo blueprint encontrado: " + yamlPath.getFileName());
                try {
                  String conteudo = Files.readString(yamlPath);
                  String[] linhas = conteudo.split("\n");
                  int preview = Math.min(12, linhas.length);
                  System.out.println("--- Inicio do YAML gerado ---");
                  for (int i = 0; i < preview; i++) {
                    System.out.println(linhas[i]);
                  }
                  if (linhas.length > preview) {
                    System.out.println("... (" + (linhas.length - preview) + " linhas restantes)");
                  }
                  System.out.println("--- Fim do preview ---");

                  System.out.println("\nCarregando o novo especialista...");
                  Interactable novoEspecialista =
                      InteractableBlueprint.fromYaml(conteudo).toInteractable();
                  System.out.println("Especialista carregado: " + novoEspecialista.name());

                  System.out.println("Testando o novo especialista com queixa dermatologica...");
                  var resultadoTeste =
                      novoEspecialista.interact(
                          "Paciente com manchas vermelhas e coceira. Preciso de diagnostico.");
                  System.out.println("Resposta do novo especialista:");
                  System.out.println(resultadoTeste.output());

                } catch (IOException e) {
                  System.out.println("Erro ao ler " + yamlPath + ": " + e.getMessage());
                }
              });
    }

    System.out.println("\n" + "=".repeat(60));
    System.out.println("Cookbook Clinica Medica concluido com sucesso!");
    System.out.println("Conceitos demonstrados:");
    System.out.println("  [x] RouterAgent.Structured<ResultadoConsulta>");
    System.out.println("  [x] Router e agentes carregados de um unico YAML");
    System.out.println("  [x] RouterAgent.Structured.of() — factory publica");
    System.out.println("  [x] Tool carregada por reflexao via toolClassNames");
    System.out.println("  [x] Padrao meta-agente (AgentDefinition como output type)");
    System.out.println("  [x] Persistencia de blueprint em YAML");
    System.out.println("  [x] Agente fallback com capacidades especiais");
    System.out.println("  [x] Use case com Interactable.Structured<T>");
    System.out.println("=".repeat(60));
  }

  // =========================================================================
  // Helpers
  // =========================================================================

  private static void exibirResultado(StructuredAgentResult<ResultadoConsulta> resultado) {
    ResultadoConsulta consulta = resultado.output();
    if (consulta == null) {
      System.out.println("  [Sem resultado estruturado]");
      if (resultado.error() != null) {
        System.out.println("  Erro: " + resultado.error().getMessage());
        if (resultado.error().getCause() != null) {
          System.out.println("  Causa: " + resultado.error().getCause().getMessage());
        }
      }
      System.out.println("  Resposta bruta: [" + resultado.rawOutput() + "]");
      return;
    }

    System.out.println("\n  [Resultado da Consulta]");
    System.out.println("  Medico:       " + consulta.medicoResponsavel());
    System.out.println("  Diagnostico:  " + consulta.diagnostico());
    System.out.println("  Tratamento:   " + consulta.tratamentoRecomendado());

    if (consulta.examesSolicitados() != null && !consulta.examesSolicitados().isEmpty()) {
      System.out.println("  Exames:       " + String.join(", ", consulta.examesSolicitados()));
    }

    System.out.println("  Retorno:      " + (consulta.precisaRetorno() ? "Sim" : "Nao"));

    if (consulta.observacoes() != null && !consulta.observacoes().isBlank()) {
      System.out.println("  Observacoes:  " + consulta.observacoes());
    }
  }
}
