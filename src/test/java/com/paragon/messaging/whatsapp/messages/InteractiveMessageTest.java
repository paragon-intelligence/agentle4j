package com.paragon.messaging.whatsapp.messages;

import static org.junit.jupiter.api.Assertions.*;

import com.paragon.messaging.core.OutboundMessage.OutboundMessageType;
import com.paragon.messaging.whatsapp.messages.InteractiveMessage.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link InteractiveMessage} and its implementations. */
@DisplayName("InteractiveMessage")
class InteractiveMessageTest {

  private Validator validator;

  @BeforeEach
  void setUp() {
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  @Nested
  @DisplayName("ButtonMessage")
  class ButtonMessageTests {

    @Test
    @DisplayName("creates button message with required fields")
    void createsWithRequiredFields() {
      ButtonMessage message =
          new ButtonMessage("Choose an option:", List.of(new ReplyButton("opt1", "Option 1")));

      assertEquals("Choose an option:", message.body());
      assertEquals(1, message.buttons().size());
      assertTrue(message.header().isEmpty());
      assertTrue(message.footer().isEmpty());
    }

    @Test
    @DisplayName("creates button message with header and footer")
    void createsWithHeaderAndFooter() {
      ButtonMessage message =
          new ButtonMessage(
              "Body text", List.of(new ReplyButton("btn1", "Button 1")), "Header", "Footer");

      assertTrue(message.header().isPresent());
      assertTrue(message.footer().isPresent());
      assertEquals("Header", message.header().get());
      assertEquals("Footer", message.footer().get());
    }

    @Test
    @DisplayName("builder creates button message")
    void builderCreates() {
      ButtonMessage message =
          ButtonMessage.builder()
              .body("Select one:")
              .addButton("yes", "Yes")
              .addButton("no", "No")
              .header("Confirm")
              .footer("Reply to continue")
              .build();

      assertEquals(2, message.buttons().size());
      assertEquals("yes", message.buttons().get(0).id());
      assertEquals("Yes", message.buttons().get(0).title());
    }

    @Test
    @DisplayName("validates body not blank")
    void validatesBodyNotBlank() {
      ButtonMessage message = new ButtonMessage("", List.of(new ReplyButton("btn1", "Button")));

      Set<ConstraintViolation<ButtonMessage>> violations = validator.validate(message);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates body length <= 1024")
    void validatesBodyLength() {
      String longBody = "A".repeat(1025);
      ButtonMessage message =
          new ButtonMessage(longBody, List.of(new ReplyButton("btn1", "Button")));

      Set<ConstraintViolation<ButtonMessage>> violations = validator.validate(message);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates button count 1-3")
    void validatesButtonCount() {
      // Too few (0)
      assertThrows(IllegalArgumentException.class, () -> new ButtonMessage("Body", List.of()));

      // Too many (4) - should throw IllegalArgumentException
      assertThrows(
          IllegalArgumentException.class,
          () ->
              new ButtonMessage(
                  "Body",
                  List.of(
                      new ReplyButton("btn1", "B1"),
                      new ReplyButton("btn2", "B2"),
                      new ReplyButton("btn3", "B3"),
                      new ReplyButton("btn4", "B4"))));
    }

    @Test
    @DisplayName("validates header length <= 60")
    void validatesHeaderLength() {
      String longHeader = "A".repeat(61);
      ButtonMessage message =
          new ButtonMessage("Body", List.of(new ReplyButton("btn1", "Button")), longHeader, null);

      Set<ConstraintViolation<ButtonMessage>> violations = validator.validate(message);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("type() returns INTERACTIVE_BUTTON")
    void typeReturnsInteractiveButton() {
      ButtonMessage message = new ButtonMessage("Body", List.of(new ReplyButton("btn1", "Button")));

      assertEquals(OutboundMessageType.INTERACTIVE_BUTTON, message.type());
    }

    @Test
    @DisplayName("creates immutable button list")
    void createsImmutableList() {
      ButtonMessage message = new ButtonMessage("Body", List.of(new ReplyButton("btn1", "Button")));

      assertThrows(
          UnsupportedOperationException.class,
          () -> message.buttons().add(new ReplyButton("btn2", "Button 2")));
    }
  }

  @Nested
  @DisplayName("ListMessage")
  class ListMessageTests {

    @Test
    @DisplayName("creates list message with sections")
    void createsWithSections() {
      ListSection section =
          new ListSection("Main", List.of(new ListRow("item1", "Item 1", "Description 1")));

      ListMessage message = new ListMessage("Choose from menu:", "View Options", List.of(section));

      assertEquals("Choose from menu:", message.body());
      assertEquals("View Options", message.buttonText());
      assertEquals(1, message.sections().size());
    }

    @Test
    @DisplayName("builder creates list message")
    void builderCreates() {
      ListMessage message =
          ListMessage.builder()
              .body("Select a dish:")
              .buttonText("View Menu")
              .addSection(
                  "Appetizers", List.of(new ListRow("app1", "Salad"), new ListRow("app2", "Soup")))
              .addSection(
                  "Main Courses",
                  List.of(new ListRow("main1", "Pasta"), new ListRow("main2", "Pizza")))
              .build();

      assertEquals(2, message.sections().size());
      assertEquals(2, message.sections().get(0).rows().size());
    }

    @Test
    @DisplayName("validates total rows <= 10")
    void validatesTotalRows() {
      List<ListRow> manyRows =
          List.of(
              new ListRow("r1", "R1"),
              new ListRow("r2", "R2"),
              new ListRow("r3", "R3"),
              new ListRow("r4", "R4"),
              new ListRow("r5", "R5"),
              new ListRow("r6", "R6"),
              new ListRow("r7", "R7"),
              new ListRow("r8", "R8"),
              new ListRow("r9", "R9"),
              new ListRow("r10", "R10"),
              new ListRow("r11", "R11")); // 11 rows - too many

      ListSection section = new ListSection(manyRows);

      assertThrows(
          IllegalArgumentException.class,
          () -> new ListMessage("Body", "Button", List.of(section)));
    }

    @Test
    @DisplayName("validates button text length <= 20")
    void validatesButtonTextLength() {
      String longButtonText = "A".repeat(21);
      ListMessage message =
          new ListMessage(
              "Body",
              longButtonText,
              List.of(new ListSection(List.of(new ListRow("r1", "Row 1")))));

      Set<ConstraintViolation<ListMessage>> violations = validator.validate(message);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("type() returns INTERACTIVE_LIST")
    void typeReturnsInteractiveList() {
      ListMessage message =
          new ListMessage(
              "Body", "Button", List.of(new ListSection(List.of(new ListRow("r1", "Row")))));

      assertEquals(OutboundMessageType.INTERACTIVE_LIST, message.type());
    }
  }

  @Nested
  @DisplayName("CtaUrlMessage")
  class CtaUrlMessageTests {

    @Test
    @DisplayName("creates CTA URL message")
    void createsCta() {
      CtaUrlMessage message =
          new CtaUrlMessage(
              "Visit our website for more information", "Visit Now", "https://example.com");

      assertEquals("Visit our website for more information", message.body());
      assertEquals("Visit Now", message.displayText());
      assertEquals("https://example.com", message.url());
    }

    @Test
    @DisplayName("validates URL format")
    void validatesUrlFormat() {
      // Valid HTTPS
      CtaUrlMessage https = new CtaUrlMessage("Body", "Click", "https://example.com");
      assertTrue(validator.validate(https).isEmpty());

      // Valid HTTP
      CtaUrlMessage http = new CtaUrlMessage("Body", "Click", "http://example.com");
      assertTrue(validator.validate(http).isEmpty());

      // Invalid (no protocol)
      CtaUrlMessage invalid = new CtaUrlMessage("Body", "Click", "example.com");
      assertFalse(validator.validate(invalid).isEmpty());
    }

    @Test
    @DisplayName("validates display text length <= 20")
    void validatesDisplayTextLength() {
      String longText = "A".repeat(21);
      CtaUrlMessage message = new CtaUrlMessage("Body", longText, "https://example.com");

      Set<ConstraintViolation<CtaUrlMessage>> violations = validator.validate(message);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("header() and footer() return empty")
    void headerFooterEmpty() {
      CtaUrlMessage message = new CtaUrlMessage("Body", "Click", "https://example.com");

      assertTrue(message.header().isEmpty());
      assertTrue(message.footer().isEmpty());
    }

    @Test
    @DisplayName("type() returns INTERACTIVE_CTA_URL")
    void typeReturnsCtaUrl() {
      CtaUrlMessage message = new CtaUrlMessage("Body", "Click", "https://example.com");

      assertEquals(OutboundMessageType.INTERACTIVE_CTA_URL, message.type());
    }
  }

  @Nested
  @DisplayName("ReplyButton")
  class ReplyButtonTests {

    @Test
    @DisplayName("creates reply button")
    void createsButton() {
      ReplyButton button = new ReplyButton("yes", "Yes");

      assertEquals("yes", button.id());
      assertEquals("Yes", button.title());
    }

    @Test
    @DisplayName("validates ID not blank")
    void validatesIdNotBlank() {
      ReplyButton button = new ReplyButton("", "Title");

      Set<ConstraintViolation<ReplyButton>> violations = validator.validate(button);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates title length <= 20")
    void validatesTitleLength() {
      String longTitle = "A".repeat(21);
      ReplyButton button = new ReplyButton("id", longTitle);

      Set<ConstraintViolation<ReplyButton>> violations = validator.validate(button);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("MAX_TITLE_LENGTH constant is 20")
    void maxTitleLengthIs20() {
      assertEquals(20, ReplyButton.MAX_TITLE_LENGTH);
    }
  }

  @Nested
  @DisplayName("ListSection")
  class ListSectionTests {

    @Test
    @DisplayName("creates section with title")
    void createsWithTitle() {
      ListSection section = new ListSection("Category", List.of(new ListRow("item1", "Item 1")));

      assertTrue(section.title().isPresent());
      assertEquals("Category", section.title().get());
    }

    @Test
    @DisplayName("creates section without title")
    void createsWithoutTitle() {
      ListSection section = new ListSection(List.of(new ListRow("item1", "Item 1")));

      assertTrue(section.title().isEmpty());
    }

    @Test
    @DisplayName("validates title length <= 24")
    void validatesTitleLength() {
      String longTitle = "A".repeat(25);
      ListSection section = new ListSection(longTitle, List.of(new ListRow("item1", "Item")));

      Set<ConstraintViolation<ListSection>> violations = validator.validate(section);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates at least one row")
    void validatesMinRows() {
      assertThrows(Exception.class, () -> new ListSection(List.of()));
    }

    @Test
    @DisplayName("creates immutable row list")
    void createsImmutableRows() {
      ListSection section = new ListSection(List.of(new ListRow("r1", "Row 1")));

      assertThrows(
          UnsupportedOperationException.class,
          () -> section.rows().add(new ListRow("r2", "Row 2")));
    }
  }

  @Nested
  @DisplayName("ListRow")
  class ListRowTests {

    @Test
    @DisplayName("creates row with description")
    void createsWithDescription() {
      ListRow row = new ListRow("item1", "Item Name", "Item description");

      assertEquals("item1", row.id());
      assertEquals("Item Name", row.title());
      assertTrue(row.description().isPresent());
      assertEquals("Item description", row.description().get());
    }

    @Test
    @DisplayName("creates row without description")
    void createsWithoutDescription() {
      ListRow row = new ListRow("item1", "Item Name");

      assertTrue(row.description().isEmpty());
    }

    @Test
    @DisplayName("validates title length <= 24")
    void validatesTitleLength() {
      String longTitle = "A".repeat(25);
      ListRow row = new ListRow("id", longTitle);

      Set<ConstraintViolation<ListRow>> violations = validator.validate(row);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates description length <= 72")
    void validatesDescriptionLength() {
      String longDesc = "A".repeat(73);
      ListRow row = new ListRow("id", "Title", longDesc);

      Set<ConstraintViolation<ListRow>> violations = validator.validate(row);
      assertFalse(violations.isEmpty());
    }

    @Test
    @DisplayName("validates ID not blank")
    void validatesIdNotBlank() {
      ListRow row = new ListRow("", "Title");

      Set<ConstraintViolation<ListRow>> violations = validator.validate(row);
      assertFalse(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Real-World Scenarios")
  class RealWorldScenariosTests {

    @Test
    @DisplayName("yes/no confirmation buttons")
    void yesNoButtons() {
      ButtonMessage message =
          ButtonMessage.builder()
              .body("Do you want to proceed with the order?")
              .addButton("yes", "Yes")
              .addButton("no", "No")
              .build();

      Set<ConstraintViolation<ButtonMessage>> violations = validator.validate(message);
      assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("restaurant menu list")
    void restaurantMenu() {
      ListMessage message =
          ListMessage.builder()
              .body("Choose your meal:")
              .buttonText("View Menu")
              .addSection(
                  "Appetizers",
                  List.of(
                      new ListRow("salad", "Caesar Salad", "Fresh romaine lettuce"),
                      new ListRow("soup", "Tomato Soup", "Homemade daily")))
              .addSection(
                  "Main Courses",
                  List.of(
                      new ListRow("pasta", "Spaghetti", "Al dente pasta"),
                      new ListRow("steak", "Ribeye Steak", "Premium cut")))
              .build();

      Set<ConstraintViolation<ListMessage>> violations = validator.validate(message);
      assertTrue(violations.isEmpty());
      assertEquals(4, message.sections().stream().mapToInt(s -> s.rows().size()).sum());
    }

    @Test
    @DisplayName("customer support options")
    void customerSupport() {
      ButtonMessage message =
          ButtonMessage.builder()
              .body("How can we help you today?")
              .addButton("track", "Track Order")
              .addButton("return", "Return Item")
              .addButton("support", "Contact Support")
              .header("Customer Service")
              .footer("We're here to help!")
              .build();

      Set<ConstraintViolation<ButtonMessage>> violations = validator.validate(message);
      assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("website visit CTA")
    void websiteVisit() {
      CtaUrlMessage message =
          new CtaUrlMessage(
              "Check out our latest products and special offers on our website!",
              "Visit Store",
              "https://store.example.com");

      Set<ConstraintViolation<CtaUrlMessage>> violations = validator.validate(message);
      assertTrue(violations.isEmpty());
    }
  }

  @Nested
  @DisplayName("Edge Cases")
  class EdgeCasesTests {

    @Test
    @DisplayName("button with exact max title length")
    void buttonMaxTitleLength() {
      String maxTitle = "A".repeat(20);
      ReplyButton button = new ReplyButton("id", maxTitle);

      Set<ConstraintViolation<ReplyButton>> violations = validator.validate(button);
      assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("list with exactly 10 rows")
    void listWith10Rows() {
      List<ListRow> rows =
          List.of(
              new ListRow("r1", "R1"),
              new ListRow("r2", "R2"),
              new ListRow("r3", "R3"),
              new ListRow("r4", "R4"),
              new ListRow("r5", "R5"),
              new ListRow("r6", "R6"),
              new ListRow("r7", "R7"),
              new ListRow("r8", "R8"),
              new ListRow("r9", "R9"),
              new ListRow("r10", "R10"));

      ListMessage message = new ListMessage("Body", "Button", List.of(new ListSection(rows)));

      // Should succeed with exactly 10 rows
      assertNotNull(message);
    }

    @Test
    @DisplayName("handles Unicode in button titles")
    void handlesUnicodeInButtons() {
      ButtonMessage message =
          ButtonMessage.builder()
              .body("Escolha:")
              .addButton("sim", "Sim ✓")
              .addButton("nao", "Não ✗")
              .build();

      Set<ConstraintViolation<ButtonMessage>> violations = validator.validate(message);
      assertTrue(violations.isEmpty());
    }
  }
}
