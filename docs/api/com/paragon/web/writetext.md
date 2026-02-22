# :material-database: WriteText

`com.paragon.web.WriteText` &nbsp;·&nbsp; **Record**

---

Action to write text into an input field, text area, or contenteditable element.

Note: You must first focus the element using a 'click' action before writing. The text will be
typed character by character to simulate keyboard input.

## Methods

### `of`

```java
public static WriteText of(@NonNull String text)
```

Creates a WriteText action.

**Parameters**

| Name | Description |
|------|-------------|
| `text` | The text to write |

**Returns**

A new WriteText instance

