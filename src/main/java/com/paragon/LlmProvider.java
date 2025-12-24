package com.paragon;

import org.jspecify.annotations.NonNull;

public enum LlmProvider {
  AI21("AI21"),
  AION_LABS("Aion Labs"),
  AMAZON_BEDROCK("Amazon Bedrock"),
  AMAZON_NOVA("Amazon Nova"),
  ANTHROPIC("Anthropic"),
  ARCEE_AI("Arcee AI"),
  ATLAS_CLOUD("AtlasCloud"),
  AVIAN("Avian"),
  AZURE("Azure"),
  BASE_TEN("BaseTen"),
  BYTE_PLUS("BytePlus"),
  BLACK_FOREST_LABS("Black Forest Labs"),
  CEREBRAS("Cerebras"),
  CHUTES("Chutes"),
  CIRRASCALE("Cirrascale"),
  CLARIFAI("Clarifai"),
  CLOUDFLARE("Cloudflare"),
  COHERE("Cohere"),
  CRUSOE("Crusoe"),
  DEEPINFRA("DeepInfra"),
  DEEPSEEK("Deepseek"),
  FEATHERLESS("Featherless"),
  FIREWORKS("Fireworks"),
  FRIENDLI("Friendli"),
  GMICLOUD("GMICloud"),
  GOPOMELO("GoPomelo"),
  GOOGLE("Google"),
  GOOGLE_AI_STUDIO("Google AI Studio"),
  GROQ("Groq"),
  HYPERBOLIC("Hyperbolic"),
  INCEPTION("Inception"),
  INFERENCENET("InferenceNet"),
  INFERMATIC("Infermatic"),
  INFLECTION("Inflection"),
  LIQUID("Liquid"),
  MARA("Mara"),
  MANCER2("Mancer 2"),
  MINIMAX("Minimax"),
  MODELRUN("ModelRun"),
  MISTRAL("Mistral"),
  MODULAR("Modular"),
  MOONSHOT_AI("Moonshot AI"),
  MORPH("Morph"),
  NCOMPASS("NCompass"),
  NEBIUS("Nebius"),
  NEXTBIT("NextBit"),
  NOVITA("Novita"),
  NVIDIA("Nvidia"),
  OPENAI("OpenAI"),
  OPEN_INFERENCE("OpenInference"),
  PARASAIL("Parasail"),
  PERPLEXITY("Perplexity"),
  PHALA("Phala"),
  RELACE("Relace"),
  SAMBANOVA("SambaNova"),
  SILICONFLOW("SiliconFlow"),
  SOURCEFUL("Sourceful"),
  STEALTH("Stealth"),
  STREAMLAKE("StreamLake"),
  SWICHPOINT("Swichpoint"),
  TARGON("Targon"),
  TOGETHER("Together"),
  VENICE("Venice"),
  WANDB("WandB"),
  XIAOMI("Xiaomi"),
  XAI("xAI"),
  ZAI("Z.AI"),
  FAKE_PROVIDER("FakeProvider");

  private final @NonNull String value;

  LlmProvider(@NonNull String value) {
    this.value = value;
  }

  @Override
  public @NonNull String toString() {
    return value;
  }
}
