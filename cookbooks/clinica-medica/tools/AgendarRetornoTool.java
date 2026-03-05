package cookbooks.clinicamedica.tools;

import com.paragon.responses.annotations.FunctionMetadata;
import com.paragon.responses.spec.FunctionTool;
import com.paragon.responses.spec.FunctionToolCallOutput;
import java.time.LocalDate;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Ferramenta para agendar retorno do paciente.
 *
 * <p>Usa construtor sem argumentos, podendo ser carregada via YAML com {@code toolClassNames}.
 * Em produção, esta ferramenta integraria com o sistema de agendamento da clínica.
 */
@FunctionMetadata(
    name = "agendar_retorno",
    description = "Agenda retorno do paciente para a especialidade informada.")
public class AgendarRetornoTool extends FunctionTool<AgendarRetornoTool.Params> {

  /** Parâmetros para agendamento de retorno. */
  public record Params(@NonNull String especialidade, int diasPrazo) {}

  public AgendarRetornoTool() {
    super();
  }

  @Override
  public @Nullable FunctionToolCallOutput call(@Nullable Params params) {
    if (params == null) {
      return FunctionToolCallOutput.error("Parâmetros de agendamento não informados");
    }

    LocalDate dataRetorno = LocalDate.now().plusDays(params.diasPrazo());
    String confirmacao =
        "Retorno agendado com sucesso!\n"
            + "Especialidade: %s\n"
            + "Data prevista: %s\n"
            + "Confirme presença com 24h de antecedência pelo telefone (11) 3000-0000"
                .formatted(params.especialidade(), dataRetorno);

    return FunctionToolCallOutput.success(confirmacao);
  }
}
