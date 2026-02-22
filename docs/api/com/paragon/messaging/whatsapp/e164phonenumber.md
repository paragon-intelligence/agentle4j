# :material-at: E164PhoneNumber

`com.paragon.messaging.whatsapp.E164PhoneNumber` &nbsp;·&nbsp; **Annotation**

---

Anotação de validação para números de telefone no formato E.164.

Formato E.164: [+][código do país][número]

Exemplo: +5511999999999, +14155552671

Regras:

  
- Pode iniciar com +
- Primeiro dígito não pode ser 0
- Total de 1 a 15 dígitos (sem contar o +)
- Apenas números (sem espaços, parênteses, hífens)

*Since: 2.0*

## Annotation Elements

### `normalize`

```java
public static String normalize(String phoneNumber)
```

Normaliza um número de telefone para o formato E.164.

Remove espaços, parênteses, hífens e adiciona + se necessário.

**Parameters**

| Name | Description |
|------|-------------|
| `phoneNumber` | número a ser normalizado |

**Returns**

número normalizado ou null se inválido

---

### `isValid`

```java
public static boolean isValid(String phoneNumber)
```

Verifica se um número está no formato E.164 válido.

**Parameters**

| Name | Description |
|------|-------------|
| `phoneNumber` | número a verificar |

**Returns**

true se válido

---

### `extractCountryCode`

```java
public static String extractCountryCode(String phoneNumber)
```

Extrai o código do país de um número E.164.

**Parameters**

| Name | Description |
|------|-------------|
| `phoneNumber` | número em formato E.164 |

**Returns**

código do país ou null se não puder extrair


## Fields

### `E164_PATTERN`

```java
private static final String E164_PATTERN = "^\\+?[1-9]\\d
```

Regex para validação de número E.164.

Padrão: ^\\+?[1-9]\\d{1,14}$

  
- ^ - início da string
- \\+? - + opcional
- [1-9] - primeiro dígito de 1-9 (não pode ser 0)
- \\d{1,14} - de 1 a 14 dígitos adicionais
- $ - fim da string
