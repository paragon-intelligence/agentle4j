package com.paragon.harness;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.harness.tools.ShellVerificationTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

@DisplayName("ShellVerificationTool")
class ShellVerificationToolTest {

  @Nested
  @DisplayName("runCommand")
  class RunCommand {

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("passes for zero-exit command")
    void passesForZeroExitCode() {
      ShellVerificationTool tool =
          ShellVerificationTool.builder().name("echo_test").command("echo", "hello").build();

      VerificationResult result = tool.runCommand();

      assertTrue(result.passed());
      assertEquals(0, result.exitCode());
      assertTrue(result.output().contains("hello"));
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("fails for non-zero-exit command")
    void failsForNonZeroExitCode() {
      ShellVerificationTool tool =
          ShellVerificationTool.builder()
              .name("false_cmd")
              .command("false") // always exits with code 1
              .build();

      VerificationResult result = tool.runCommand();

      assertFalse(result.passed());
      assertNotEquals(0, result.exitCode());
    }

    @Test
    @EnabledOnOs({OS.LINUX, OS.MAC})
    @DisplayName("captures output in result")
    void capturesOutput() {
      ShellVerificationTool tool =
          ShellVerificationTool.builder()
              .name("capture_test")
              .command("echo", "captured output")
              .build();

      VerificationResult result = tool.runCommand();
      assertTrue(result.output().contains("captured output"));
    }

    @Test
    @DisplayName("call() wraps runCommand output in FunctionToolCallOutput")
    @EnabledOnOs({OS.LINUX, OS.MAC})
    void callWrapsOutput() {
      ShellVerificationTool tool =
          ShellVerificationTool.builder().name("call_test").command("echo", "wrapped").build();

      var output = tool.call(new ShellVerificationTool.TriggerRequest());
      assertNotNull(output);
      assertTrue(
          output.output().toString().contains("wrapped")
              || output.output().toString().contains("PASSED"));
    }
  }

  @Nested
  @DisplayName("builder validation")
  class BuilderValidation {

    @Test
    @DisplayName("throws if name is not set")
    void throwsIfNameMissing() {
      assertThrows(
          NullPointerException.class,
          () -> ShellVerificationTool.builder().command("echo", "hi").build());
    }

    @Test
    @DisplayName("throws if command is empty")
    void throwsIfCommandEmpty() {
      assertThrows(
          IllegalArgumentException.class,
          () -> ShellVerificationTool.builder().name("test").command(new String[0]).build());
    }
  }
}
