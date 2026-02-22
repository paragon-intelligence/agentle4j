package com.paragon.tts;

import com.paragon.messaging.whatsapp.config.TTSConfig;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Provider genérico de Text-to-Speech.
 *
 * <p>Implementações suportam diferentes providers:
 *
 * <ul>
 *   <li>ElevenLabs
 *   <li>OpenAI TTS
 *   <li>Google Cloud TTS
 *   <li>Azure Speech
 *   <li>Amazon Polly
 * </ul>
 *
 * <p><b>Exemplo:</b>
 *
 * <pre>{@code
 * TTSProvider tts = ElevenLabsTTSProvider.create(apiKey);
 * TTSConfig config = TTSConfig.ptBR("voiceId");
 * byte[] audio = tts.synthesize("Olá, como posso ajudar?", config);
 * }</pre>
 *
 * @author Agentle Team
 * @since 1.0
 */
public interface TTSProvider {

  /**
   * Provider no-op para testes.
   *
   * @return provider vazio
   */
  static TTSProvider noOp() {
    return (text, config) -> new Byte[0];
  }

  /**
   * Sintetiza texto em áudio.
   *
   * @param text texto a converter
   * @param config configuração de voz, idioma, velocidade, pitch
   * @return bytes do áudio (formato depende do provider)
   * @throws TTSException se síntese falhar
   */
  @NonNull Byte[] synthesize(@NonNull String text, @Nullable TTSConfig config) throws TTSException;

  /**
   * Verifica se provider está disponível.
   *
   * @return true se está configurado e pronto
   */
  default boolean isAvailable() {
    return true;
  }

  /**
   * Retorna formato de áudio retornado.
   *
   * @return formato (ex: "mp3", "opus", "aac")
   */
  default String audioFormat() {
    return "mp3";
  }
}
