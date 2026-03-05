package cookbooks.clinicamedica.usecase;

/**
 * Input para o caso de uso de atendimento de paciente.
 *
 * @param nomePaciente nome completo do paciente
 * @param idadePaciente idade do paciente em anos
 * @param queixa queixa principal relatada pelo paciente
 */
public record AtenderPacienteInput(String nomePaciente, int idadePaciente, String queixa) {}
