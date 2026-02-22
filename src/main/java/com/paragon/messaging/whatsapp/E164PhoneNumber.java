package com.paragon.messaging.whatsapp;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Anotação de validação para números de telefone no formato E.164.
 *
 * <p>Formato E.164: [+][código do país][número]
 *
 * <p>Exemplo: +5511999999999, +14155552671
 *
 * <p>Regras:
 *
 * <ul>
 *   <li>Pode iniciar com +
 *   <li>Primeiro dígito não pode ser 0
 *   <li>Total de 1 a 15 dígitos (sem contar o +)
 *   <li>Apenas números (sem espaços, parênteses, hífens)
 * </ul>
 *
 * @author Your Name
 * @since 2.0
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = E164PhoneNumber.E164Validator.class)
@Documented
public @interface E164PhoneNumber {

  String message() default "Phone number must be in E.164 format (e.g., +5511999999999)";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  /** Implementação do validador para formato E.164. */
  class E164Validator implements ConstraintValidator<E164PhoneNumber, String> {

    /**
     * Regex para validação de número E.164.
     *
     * <p>Padrão: ^\\+?[1-9]\\d{1,14}$
     *
     * <ul>
     *   <li>^ - início da string
     *   <li>\\+? - + opcional
     *   <li>[1-9] - primeiro dígito de 1-9 (não pode ser 0)
     *   <li>\\d{1,14} - de 1 a 14 dígitos adicionais
     *   <li>$ - fim da string
     * </ul>
     */
    private static final String E164_PATTERN = "^\\+?[1-9]\\d{1,14}$";

    @Override
    public void initialize(E164PhoneNumber constraintAnnotation) {
      // Nenhuma inicialização necessária
    }

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
      if (phoneNumber == null) {
        return true; // Usar @NotNull separadamente para validar nulidade
      }

      if (phoneNumber.isBlank()) {
        return false;
      }

      // Remove o + para contagem de dígitos
      String digits = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;

      // Valida contra o padrão E.164
      if (!phoneNumber.matches(E164_PATTERN)) {
        // Mensagem de erro customizada baseada no problema
        context.disableDefaultConstraintViolation();

        if (!digits.matches("\\d+")) {
          context
              .buildConstraintViolationWithTemplate(
                  "Phone number must contain only digits (found non-digit characters)")
              .addConstraintViolation();
        } else if (digits.length() < 2 || digits.length() > 15) {
          context
              .buildConstraintViolationWithTemplate(
                  "Phone number must have between 2 and 15 digits (found "
                      + digits.length()
                      + " digits)")
              .addConstraintViolation();
        } else if (digits.startsWith("0")) {
          context
              .buildConstraintViolationWithTemplate(
                  "Phone number cannot start with 0 in E.164 format")
              .addConstraintViolation();
        } else {
          context
              .buildConstraintViolationWithTemplate(
                  "Phone number is not in valid E.164 format (e.g., +5511999999999)")
              .addConstraintViolation();
        }

        return false;
      }

      return true;
    }
  }

  /** Métodos utilitários para trabalhar com números E.164. */
  class Utils {

    /**
     * Normaliza um número de telefone para o formato E.164.
     *
     * <p>Remove espaços, parênteses, hífens e adiciona + se necessário.
     *
     * @param phoneNumber número a ser normalizado
     * @return número normalizado ou null se inválido
     */
    public static String normalize(String phoneNumber) {
      if (phoneNumber == null || phoneNumber.isBlank()) {
        return null;
      }

      // Remove todos os caracteres não-numéricos exceto +
      String cleaned = phoneNumber.replaceAll("[^+\\d]", "");

      // Collapse multiple + signs into one
      cleaned = cleaned.replaceAll("\\++", "+");

      // Adiciona + se não tiver
      if (!cleaned.startsWith("+")) {
        cleaned = "+" + cleaned;
      }

      // Valida se está no formato correto
      if (cleaned.matches("^\\+?[1-9]\\d{1,14}$")) {
        return cleaned;
      }

      return null;
    }

    /**
     * Verifica se um número está no formato E.164 válido.
     *
     * @param phoneNumber número a verificar
     * @return true se válido
     */
    public static boolean isValid(String phoneNumber) {
      return phoneNumber != null
          && !phoneNumber.isBlank()
          && phoneNumber.matches("^\\+?[1-9]\\d{1,14}$");
    }

    /**
     * Extrai o código do país de um número E.164.
     *
     * @param phoneNumber número em formato E.164
     * @return código do país ou null se não puder extrair
     */
    public static String extractCountryCode(String phoneNumber) {
      if (!isValid(phoneNumber)) {
        return null;
      }

      String digits = phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;

      // Códigos de país podem ter 1-3 dígitos
      // Tenta extrair baseado em padrões conhecidos
      if (digits.startsWith("1")) {
        return "1"; // USA, Canadá
      } else if (digits.startsWith("55")) {
        return "55"; // Brasil
      } else if (digits.length() >= 2) {
        return digits.substring(0, 2); // Padrão: 2 dígitos
      }

      return null;
    }
  }
}
