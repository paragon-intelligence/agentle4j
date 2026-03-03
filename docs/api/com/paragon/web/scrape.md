# :material-database: Scrape

> This docs was updated at: 2026-03-03


`com.paragon.web.Scrape` &nbsp;·&nbsp; **Record**

---

Action to scrape the current page content.

Returns a `ScrapeResult` containing the URL and HTML content of the page.

## Methods

### `Scrape`

```java
private static final Scrape INSTANCE = new Scrape()
```

The default scrape action instance.

---

### `create`

```java
public static Scrape create()
```

Returns the default Scrape action.

**Returns**

The Scrape action instance

