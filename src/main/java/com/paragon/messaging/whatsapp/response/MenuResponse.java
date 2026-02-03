package com.paragon.messaging.whatsapp.response;

import com.paragon.messaging.whatsapp.messages.InteractiveMessage;
import com.paragon.messaging.whatsapp.messages.InteractiveMessage.ListRow;
import com.paragon.messaging.whatsapp.messages.InteractiveMessage.ListSection;
import com.paragon.messaging.core.OutboundMessage;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Menu/list-based response for structured AI output.
 *
 * <p>Represents an interactive list message with sections and selectable items.
 * Maximum 10 total items across all sections.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Simple menu
 * MenuResponse response = MenuResponse.builder()
 *     .body("What would you like to order?")
 *     .buttonText("View Menu")
 *     .addSection("Drinks", List.of(
 *         new MenuItem("coffee", "Coffee", "$3.50"),
 *         new MenuItem("tea", "Tea", "$2.50")
 *     ))
 *     .addSection("Food", List.of(
 *         new MenuItem("sandwich", "Sandwich", "$5.00"),
 *         new MenuItem("salad", "Salad", "$4.50")
 *     ))
 *     .build();
 *
 * // With context
 * MenuResponse response = MenuResponse.builder()
 *     .header("Our Menu")
 *     .body("Browse our selection:")
 *     .buttonText("Open Menu")
 *     .footer("Tap an item to order")
 *     .addItem("burger", "Burger", "Juicy beef burger")
 *     .addItem("pizza", "Pizza", "Authentic Italian")
 *     .replyTo("wamid.xyz123")
 *     .reactTo("wamid.xyz123", "😋")
 *     .build();
 * }</pre>
 *
 * @param body       the message body text
 * @param buttonText the text on the list button (max 20 chars)
 * @param sections   the menu sections with items
 * @param header     optional header text
 * @param footer     optional footer text
 * @param context    optional response context
 * @author Agentle Team
 * @since 2.1
 */
public record MenuResponse(
        @NonNull String body,
        @NonNull String buttonText,
        @NonNull List<Section> sections,
        @Nullable String header,
        @Nullable String footer,
        @Nullable ResponseContext context
) implements WhatsAppResponse {

    public static final int MAX_TOTAL_ITEMS = 10;
    public static final int MAX_BUTTON_TEXT_LENGTH = 20;

    public MenuResponse {
        Objects.requireNonNull(body, "body cannot be null");
        Objects.requireNonNull(buttonText, "buttonText cannot be null");
        Objects.requireNonNull(sections, "sections cannot be null");

        if (buttonText.length() > MAX_BUTTON_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "buttonText cannot exceed " + MAX_BUTTON_TEXT_LENGTH + " characters");
        }

        int totalItems = sections.stream()
                .mapToInt(s -> s.items.size())
                .sum();
        if (totalItems == 0) {
            throw new IllegalArgumentException("At least one menu item is required");
        }
        if (totalItems > MAX_TOTAL_ITEMS) {
            throw new IllegalArgumentException(
                    "Maximum " + MAX_TOTAL_ITEMS + " total items allowed (found: " + totalItems + ")");
        }

        sections = List.copyOf(sections);
    }

    /**
     * Creates a builder for MenuResponse.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public @NonNull List<OutboundMessage> toMessages() {
        InteractiveMessage.ListMessage.Builder builder = InteractiveMessage.ListMessage.builder()
                .body(body)
                .buttonText(buttonText);

        if (header != null) {
            builder.header(header);
        }
        if (footer != null) {
            builder.footer(footer);
        }

        for (Section section : sections) {
            List<ListRow> rows = section.items.stream()
                    .map(item -> new ListRow(item.id, item.title, item.description))
                    .toList();
            builder.addSection(section.title, rows);
        }

        return List.of(builder.build());
    }

    @Override
    public @NonNull String getTextContent() {
        StringBuilder sb = new StringBuilder(body);
        for (Section section : sections) {
            if (section.title != null) {
                sb.append("\n").append(section.title).append(":");
            }
            for (MenuItem item : section.items) {
                sb.append("\n- ").append(item.title);
                if (item.description != null) {
                    sb.append(": ").append(item.description);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public @Nullable String getReactionEmoji() {
        return context != null ? context.reactionEmoji() : null;
    }

    @Override
    public @Nullable String getReactToMessageId() {
        return context != null ? context.reactToMessageId() : null;
    }

    @Override
    public @Nullable String getReplyToMessageId() {
        return context != null ? context.replyToMessageId() : null;
    }

    @Override
    public @Nullable ResponseContext getContext() {
        return context;
    }

    /**
     * Represents a menu section with optional title.
     *
     * @param title optional section title
     * @param items the items in this section
     */
    public record Section(
            @Nullable String title,
            @NonNull List<MenuItem> items
    ) {
        public Section {
            Objects.requireNonNull(items, "items cannot be null");
            if (items.isEmpty()) {
                throw new IllegalArgumentException("Section must have at least one item");
            }
            items = List.copyOf(items);
        }

        /**
         * Creates a section without a title.
         *
         * @param items the menu items
         */
        public Section(@NonNull List<MenuItem> items) {
            this(null, items);
        }
    }

    /**
     * Represents a selectable menu item.
     *
     * @param id          unique item identifier
     * @param title       item display title (max 24 chars)
     * @param description optional item description (max 72 chars)
     */
    public record MenuItem(
            @NonNull String id,
            @NonNull String title,
            @Nullable String description
    ) {
        public MenuItem {
            Objects.requireNonNull(id, "id cannot be null");
            Objects.requireNonNull(title, "title cannot be null");
            if (title.length() > ListRow.MAX_TITLE_LENGTH) {
                throw new IllegalArgumentException(
                        "Item title cannot exceed " + ListRow.MAX_TITLE_LENGTH + " characters");
            }
            if (description != null && description.length() > ListRow.MAX_DESCRIPTION_LENGTH) {
                throw new IllegalArgumentException(
                        "Item description cannot exceed " + ListRow.MAX_DESCRIPTION_LENGTH + " characters");
            }
        }

        /**
         * Creates a menu item without description.
         *
         * @param id    item identifier
         * @param title item title
         */
        public MenuItem(@NonNull String id, @NonNull String title) {
            this(id, title, null);
        }
    }

    /**
     * Builder for MenuResponse.
     */
    public static final class Builder {
        private String body;
        private String buttonText = "Menu";
        private final List<Section> sections = new ArrayList<>();
        private List<MenuItem> currentSectionItems = new ArrayList<>();
        private String currentSectionTitle = null;
        private String header;
        private String footer;
        private ResponseContext.Builder contextBuilder;

        private Builder() {}

        /**
         * Sets the message body text.
         *
         * @param body the body text
         * @return this builder
         */
        public Builder body(@NonNull String body) {
            this.body = body;
            return this;
        }

        /**
         * Sets the button text that opens the list.
         *
         * @param buttonText the button text (max 20 chars)
         * @return this builder
         */
        public Builder buttonText(@NonNull String buttonText) {
            this.buttonText = buttonText;
            return this;
        }

        /**
         * Sets the optional header text.
         *
         * @param header the header text
         * @return this builder
         */
        public Builder header(@Nullable String header) {
            this.header = header;
            return this;
        }

        /**
         * Sets the optional footer text.
         *
         * @param footer the footer text
         * @return this builder
         */
        public Builder footer(@Nullable String footer) {
            this.footer = footer;
            return this;
        }

        /**
         * Adds a complete section with items.
         *
         * @param title section title (optional)
         * @param items section items
         * @return this builder
         */
        public Builder addSection(@Nullable String title, @NonNull List<MenuItem> items) {
            flushCurrentSection();
            this.sections.add(new Section(title, items));
            return this;
        }

        /**
         * Starts a new section. Items added after this will belong to this section.
         *
         * @param title section title
         * @return this builder
         */
        public Builder section(@Nullable String title) {
            flushCurrentSection();
            this.currentSectionTitle = title;
            return this;
        }

        /**
         * Adds an item to the current section (or default section if none started).
         *
         * @param id          item identifier
         * @param title       item title
         * @param description optional item description
         * @return this builder
         */
        public Builder addItem(@NonNull String id, @NonNull String title, @Nullable String description) {
            this.currentSectionItems.add(new MenuItem(id, title, description));
            return this;
        }

        /**
         * Adds an item without description to the current section.
         *
         * @param id    item identifier
         * @param title item title
         * @return this builder
         */
        public Builder addItem(@NonNull String id, @NonNull String title) {
            return addItem(id, title, null);
        }

        /**
         * Sets the message ID to reply to.
         *
         * @param messageId the message ID
         * @return this builder
         */
        public Builder replyTo(@NonNull String messageId) {
            ensureContextBuilder().replyTo(messageId);
            return this;
        }

        /**
         * Sets the reaction for this response.
         *
         * @param messageId the message ID to react to
         * @param emoji     the reaction emoji
         * @return this builder
         */
        public Builder reactTo(@NonNull String messageId, @NonNull String emoji) {
            ensureContextBuilder().reactTo(messageId, emoji);
            return this;
        }

        private void flushCurrentSection() {
            if (!currentSectionItems.isEmpty()) {
                sections.add(new Section(currentSectionTitle, new ArrayList<>(currentSectionItems)));
                currentSectionItems.clear();
                currentSectionTitle = null;
            }
        }

        private ResponseContext.Builder ensureContextBuilder() {
            if (contextBuilder == null) {
                contextBuilder = ResponseContext.builder();
            }
            return contextBuilder;
        }

        /**
         * Builds the MenuResponse.
         *
         * @return the built response
         */
        public MenuResponse build() {
            flushCurrentSection();
            ResponseContext context = contextBuilder != null ? contextBuilder.build() : null;
            return new MenuResponse(body, buttonText, sections, header, footer, context);
        }
    }
}
