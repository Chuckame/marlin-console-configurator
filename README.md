# marlin-console-configurator

Regex:
```regex
^(?:\h*(\/\/))?\h*#define\h+(\w+)(?:\h+([^\/\r\n]+))?(\h*\/\/\h*(.+))?$
```

|Group index|Name|Comment|Can be absent|
|---|---|---|---|
|0|Full matched line|||
|1|Indicates if the constant is enabled or not|enabled (absent/empty group) or disabled (present/non-empty group)|yes|
|2|Constant name|No need to be trimmed|no|
|3|Value|Contains the value of the constant. Must be trimmed to have the real value|yes|
|4|Comment|More info about the constant, without `//`. Note: only the line is matched, so we cannot easily determine if there is a previous/next/multiline comment|yes|
