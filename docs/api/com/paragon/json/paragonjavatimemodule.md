# :material-code-braces: ParagonJavaTimeModule

`com.paragon.json.ParagonJavaTimeModule` &nbsp;·&nbsp; **Class**

Extends `SimpleModule`

---

Minimal Java time module for the temporal types persisted by this project.

The published Jackson 3 jsr310 module currently lags behind databind 3.1.x, so we register
lightweight ISO-8601 serializers/deserializers locally for the types used in this codebase.
