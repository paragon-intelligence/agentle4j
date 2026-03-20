# :material-database: Recipient

`com.paragon.messaging.core.Recipient` &nbsp;·&nbsp; **Record**

---

Representa um destinatário de mensagem com validação automática.

*Since: 2.0*

## Methods

### `ofPhoneNumber`

```java
public static Recipient ofPhoneNumber(@NotBlank String phoneNumber)
```

Cria um destinatário usando número de telefone.

O número será validado automaticamente quando usado com @Valid.

**Parameters**

| Name | Description |
|------|-------------|
| `phoneNumber` | número de telefone no formato E.164 (ex: +5511999999999) |

**Returns**

destinatário validado

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | se o número não estiver em formato E.164 |

---

### `ofPhoneNumberNormalized`

```java
public static Recipient ofPhoneNumberNormalized(String phoneNumber)
```

Cria um destinatário usando número de telefone com normalização automática.

Este método tenta normalizar o número removendo espaços, parênteses, etc.

**Parameters**

| Name | Description |
|------|-------------|
| `phoneNumber` | número de telefone em qualquer formato |

**Returns**

destinatário validado

**Throws**

| Type | Condition |
|------|-----------|
| `IllegalArgumentException` | se o número não puder ser normalizado |

---

### `ofUserId`

```java
public static Recipient ofUserId(@NotBlank String userId)
```

Cria um destinatário usando ID de usuário.

**Parameters**

| Name | Description |
|------|-------------|
| `userId` | ID do usuário na plataforma |

**Returns**

destinatário

---

### `ofEmail`

```java
public static Recipient ofEmail(@NotBlank @Email String email)
```

Cria um destinatário usando email.

**Parameters**

| Name | Description |
|------|-------------|
| `email` | endereço de email |

**Returns**

destinatário

---

### `isPhoneNumber`

```java
public boolean isPhoneNumber()
```

Verifica se este destinatário é um número de telefone.

**Returns**

true se for número de telefone

---

### `isUserId`

```java
public boolean isUserId()
```

Verifica se este destinatário é um ID de usuário.

**Returns**

true se for user ID

---

### `isEmail`

```java
public boolean isEmail()
```

Verifica se este destinatário é um email.

**Returns**

true se for email

---

### `value`

```java
public String value()
```

Returns the recipient value (alias for identifier()). Provided for API compatibility.

**Returns**

the recipient identifier

