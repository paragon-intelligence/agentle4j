package cookbooks.clinicamedica.tools;

import com.paragon.agents.Agent;
import com.paragon.agents.AgentDefinition;
import com.paragon.agents.InteractableBlueprint;
import com.paragon.agents.InteractableBlueprint.ResponderBlueprint;
import com.paragon.responses.Responder;
import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Ferramenta de administração que contrata (cria) um novo médico especialista em runtime.
 *
 * <p>Demonstra o padrão <b>meta-agente</b>: esta ferramenta usa um agente interno cujo tipo de
 * saída é {@link AgentDefinition}. O meta-agente recebe uma descrição de especialidade e gera
 * a definição comportamental do novo agente, que é então convertida em blueprint YAML e salva
 * em disco para uso futuro.
 *
 * <p><b>Por que injetar dependências aqui?</b><br>
 * Diferente das ferramentas sem argumento (carregáveis por YAML via reflexão), esta ferramenta
 * precisa de infraestrutura em tempo de execução: o {@link Responder} para fazer chamadas LLM,
 * o diretório onde salvar o YAML, e o modelo a usar. Por isso é construída manualmente no
 * {@code Main.java} e adicionada ao agente de Administração.
 *
 * <p><b>Limitação pedagógica</b>: O meta-agente pode escolher ferramentas de uma lista
 * pré-definida (toolNames), pois não consegue inventar código Java novo. Neste exemplo,
 * o novo especialista criado não terá ferramentas, mas demonstra o fluxo completo de
 * criação → serialização → persistência de um blueprint de agente.
 */
@FunctionMetadata(
    name = "contratar_medico",
    description =
        "Contrata um novo médico especialista criando seu blueprint de agente e salvando em disco.")
public class ContratarMedicoTool extends FunctionTool<ContratarMedicoTool.Params> {

  /** Parâmetros para contratação de novo especialista. */
  public record Params(
      @NonNull String especialidade,
      @NonNull String descricaoNecessidade) {}

  private final Responder responder;
  private final Path agentsDir;
  private final String model;

  /**
   * Construtor sem argumentos para carregamento via YAML/toolClassNames.
   *
   * <p>Lê {@code OPENROUTER_API_KEY} e {@code AGENT_MODEL} do ambiente.
   */
  public ContratarMedicoTool() {
    super();
    String apiKey = Objects.requireNonNullElse(System.getenv("OPENROUTER_API_KEY"), "");
    this.responder = Responder.builder().openRouter().apiKey(apiKey).build();
    this.agentsDir = Paths.get("cookbooks/clinica-medica/agents");
    this.model = Objects.requireNonNullElse(System.getenv("AGENT_MODEL"), "openai/gpt-4o-mini");
  }

  /**
   * Cria a ferramenta com as dependências necessárias para o padrão meta-agente.
   *
   * @param responder o responder para chamadas LLM do meta-agente
   * @param agentsDir diretório onde os blueprints YAML serão salvos
   * @param model modelo LLM a ser usado pelo meta-agente e pelo novo especialista
   */
  public ContratarMedicoTool(Responder responder, Path agentsDir, String model) {
    super();
    this.responder = responder;
    this.agentsDir = agentsDir;
    this.model = model;
  }

  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable Params params) {
    if (params == null) {
      return FunctionToolCallOutput.error("Parâmetros de contratação não informados");
    }

    try {
      // 1. Criar o meta-agente que gera definições de outros agentes
      // Nota: Agent.Structured<T> tem interact() (não interactStructured()) —
      // é um wrapper tipado específico de Agent, diferente de Interactable.Structured<T>.
      Agent.Structured<AgentDefinition> metaAgente =
          Agent.builder()
              .name("MetaAgenteMedico")
              .model(model)
              .instructions(
                  """
                  Você é um arquiteto de agentes de IA para uma clínica médica.
                  Sua tarefa é criar a definição comportamental de um agente médico especialista.

                  O agente que você criar deve:
                  - Ter um nome descritivo (ex: "Dermatologista", "Neurologista")
                  - Ter instruções detalhadas sobre sua especialidade
                  - Ter maxTurns entre 5 e 10
                  - Ter temperatura entre 0.1 e 0.4 (especialistas médicos são precisos)
                  - Não incluir toolNames (lista vazia ou null)
                  - Incluir outputType: "cookbooks.clinicamedica.models.ResultadoConsulta"

                  Gere a definição completa do agente especialista solicitado.
                  """)
              .structured(AgentDefinition.class)
              .responder(responder)
              .build();

      // 2. Chamar o meta-agente com a descrição da necessidade
      // Agent.Structured usa interact() para retornar StructuredAgentResult<T>
      String prompt =
          "Crie um agente especialista em %s para uma clínica médica. Necessidade: %s"
              .formatted(params.especialidade(), params.descricaoNecessidade());

      var metaResult = metaAgente.interact(prompt);
      if (metaResult.isError()) {
        return FunctionToolCallOutput.error("Meta-agente não conseguiu criar a definição");
      }
      AgentDefinition definicao = metaResult.parsed();

      // 3. Converter a definição para blueprint YAML
      ResponderBlueprint responderBlueprint = ResponderBlueprint.from(responder);
      InteractableBlueprint.AgentBlueprint blueprint =
          definicao.toBlueprint(responderBlueprint, model, List.of());

      String yaml = blueprint.toYaml();

      // 4. Salvar o blueprint em disco
      String nomeArquivo = definicao.name().toLowerCase().replaceAll("[^a-z0-9]", "-");
      Path destino = agentsDir.resolve(nomeArquivo + ".yaml");
      Files.writeString(destino, yaml);

      return FunctionToolCallOutput.success(
          "Novo especialista contratado com sucesso!\n"
              + "Nome: %s\n"
              + "Blueprint salvo em: %s\n"
              + "O agente pode ser carregado via InteractableBlueprint.fromYaml()"
                  .formatted(definicao.name(), destino.toAbsolutePath()));

    } catch (IOException e) {
      return FunctionToolCallOutput.error("Erro ao salvar blueprint: " + e.getMessage());
    } catch (Exception e) {
      return FunctionToolCallOutput.error("Erro ao criar especialista: " + e.getMessage());
    }
  }
}
