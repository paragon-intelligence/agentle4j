package com.paragon.responses.spec;

/** Specify additional output data to include in the model response. */
public enum OutputDataInclude {
  /** Include the sources of the web search tool call. */
  WEB_SEARCH_CALL_CATION_SOURCES("web_search_call.action.sources"),

  /** Includes the outputs of python code execution in code interpreter tool call items. */
  CODE_INTERPRETER_CALL_OUTPUTS("code_interpreter_call.outputs"),

  /** Include image urls from the computer call output. */
  COMPUTER_CALL_OUTPUT_OUTPUT_IMAGE_URL("computer_call_output.output.image_url"),

  /** Include the search results of the file search tool call. */
  FILE_SEARCH_CALL_RESULTS("file_search_call.results"),

  /** Include image urls from the input message. */
  MESSAGE_INPUT_IMAGE_IMAGE_URL("message.input_image.image_url"),

  /** Include logprobs with assistant messages. */
  MESSAGE_OUTPUT_TEXT_LOGPROBS("message.output_text.logprobs"),

  /**
   * Includes an encrypted version of reasoning tokens in reasoning item outputs. This enables
   * reasoning items to be used in multi-turn conversations when using the Responses API statelessly
   * (like when the store parameter is set to false or when an organization is enrolled in the zero
   * data retention program).
   */
  REASONING_ENCRYPTED_CONTENT("reasoning.encrypted_content");

  private final String value;

  OutputDataInclude(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
