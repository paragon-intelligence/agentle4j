# :material-database: LocationMessage

`com.paragon.messaging.whatsapp.messages.LocationMessage` &nbsp;·&nbsp; **Record**

---

Mensagem de localização geográfica com validação de coordenadas.

*Since: 2.0*

## Methods

### `hasValidCoordinates`

```java
public boolean hasValidCoordinates()
```

Verifica se as coordenadas são válidas (não NaN, não Infinity).

**Returns**

true se as coordenadas são válidas

---

### `distanceTo`

```java
public double distanceTo(LocationMessage other)
```

Calcula a distância aproximada em km até outra localização.

Usa a fórmula de Haversine para cálculo de distância sobre a esfera terrestre.

**Parameters**

| Name | Description |
|------|-------------|
| `other` | outra localização |

**Returns**

distância em quilômetros

---

### `toCoordinatesString`

```java
public String toCoordinatesString()
```

Retorna uma representação de coordenadas no formato "lat,lng".

**Returns**

string de coordenadas

---

### `toGoogleMapsUrl`

```java
public String toGoogleMapsUrl()
```

Retorna um link do Google Maps para esta localização.

**Returns**

URL do Google Maps

