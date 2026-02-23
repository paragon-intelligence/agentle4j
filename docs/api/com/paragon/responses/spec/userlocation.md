# :material-code-braces: UserLocation

> This docs was updated at: 2026-02-23

`com.paragon.responses.spec.UserLocation` &nbsp;·&nbsp; **Class**

---

The approximate location of the user.

## Methods

### `UserLocation`

```java
public UserLocation(
      @Nullable String city,
      @Nullable String country,
      @Nullable String region,
      @Nullable String timezone)
```

@param city Free text input for the city of the user, e.g. `San Francisco`.

**Parameters**

| Name | Description |
|------|-------------|
| `country` | The two-letter ISO country code of the user, e.g. US. |
| `region` | Free text input for the region of the user, e.g. `California`. |
| `timezone` | The IANA timezone of the user, e.g. `America/Los_Angeles`. |

