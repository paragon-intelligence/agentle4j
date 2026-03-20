# :material-database: FunctionToolDefinition

`com.paragon.responses.spec.FunctionToolDefinition` &nbsp;·&nbsp; **Record**

---

A concrete, deserializable representation of a function tool definition.

This class is used for deserializing function tool definitions from API responses. Unlike
`FunctionTool` which is abstract and meant to be extended by users with their own
implementations, this class is a simple data carrier that can be instantiated by Jackson during
deserialization.

According to the OpenAPI spec for FunctionTool:

  
- `type` - string (enum), required. Always "function"
- `name` - string, required. The name of the function to call
- `description` - any, optional
- `parameters` - any, required. JSON Schema for the function parameters
- `strict` - any, required
