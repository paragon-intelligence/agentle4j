# :material-approximately-equal: WebAction

`com.paragon.responses.spec.WebAction` &nbsp;·&nbsp; **Interface**

---

An object describing the specific action taken in this web search call. Includes details on how
the model used the web (search, open_page, find). This is a sealed abstract class with eighteen
permitted implementations:

  
- `SearchAction` - Action type "search" - Performs a web search query.
- `OpenPageAction` - Action type "open_page" - Opens a specific URL from search
      results.
- `FindAction` - Action type "find": Searches for a pattern within a loaded page.
