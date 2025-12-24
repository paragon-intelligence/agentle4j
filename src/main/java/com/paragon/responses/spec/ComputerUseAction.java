package com.paragon.responses.spec;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * This is a sealed abstract class with eighteen permitted implementations:
 *
 * <ul>
 *   <li>{@link ClickAction} - A click action.
 *   <li>{@link DoubleClickAction} - A double click action.
 *   <li>{@link DragAction} - A drag action.
 *   <li>{@link KeyPressAction} - A collection of keypresses the model would like to perform.
 *   <li>{@link MoveAction} - A mouse move action.
 *   <li>{@link ScreenshotAction} - A screenshot action.
 *   <li>{@link ScrollAction} - A scroll action.
 *   <li>{@link TypeAction} - An action to type in text.
 *   <li>{@link WaitAction} - A wait action.
 * </ul>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = ClickAction.class, name = "click"),
  @JsonSubTypes.Type(value = DoubleClickAction.class, name = "double_click"),
  @JsonSubTypes.Type(value = DragAction.class, name = "drag"),
  @JsonSubTypes.Type(value = KeyPressAction.class, name = "keypress"),
  @JsonSubTypes.Type(value = MoveAction.class, name = "move"),
  @JsonSubTypes.Type(value = ScreenshotAction.class, name = "screenshot"),
  @JsonSubTypes.Type(value = ScrollAction.class, name = "scroll"),
  @JsonSubTypes.Type(value = TypeAction.class, name = "type"),
  @JsonSubTypes.Type(value = WaitAction.class, name = "wait")
})
public sealed interface ComputerUseAction
    permits ClickAction,
        DoubleClickAction,
        DragAction,
        KeyPressAction,
        MoveAction,
        ScreenshotAction,
        ScrollAction,
        TypeAction,
        WaitAction {}
