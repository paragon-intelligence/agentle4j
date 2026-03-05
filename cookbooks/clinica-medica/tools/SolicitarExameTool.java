package cookbooks.clinicamedica.tools;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Ferramenta para solicitar exames médicos.
 *
 * <p>Usa construtor sem argumentos, podendo ser carregada via YAML com {@code toolClassNames}.
 * Em produção, esta ferramenta criaria a solicitação no sistema hospitalar.
 */
@FunctionMetadata(
    name = "solicitar_exame",
    description = "Solicita um exame médico para o paciente atual.")
public class SolicitarExameTool extends FunctionTool<SolicitarExameTool.Params> {

  /** Parâmetros para solicitação de exame. */
  public record Params(@NonNull String tipoExame, @NonNull String justificativa) {}

  public SolicitarExameTool() {
    super();
  }

  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable Params params) {
    if (params == null) {
      return FunctionToolCallOutput.error("Parâmetros de exame não informados");
    }

    // Simulação de protocolo de exame — em produção integraria com o LIS/HIS
    String protocolo = "EX-%d".formatted(System.currentTimeMillis() % 100000);
    String confirmacao =
        "Exame solicitado com sucesso!\n"
            + "Tipo: %s\n"
            + "Justificativa: %s\n"
            + "Protocolo: %s\n"
            + "Prazo estimado para resultado: 2-3 dias úteis"
                .formatted(params.tipoExame(), params.justificativa(), protocolo);

    return FunctionToolCallOutput.success(confirmacao);
  }
}
