# marlin-console-configurator

# Example profile.yaml
```yaml
files:
  - Configuration.h
  - Configuration_adv.h
enabled:
  HEADER_WITH_VALUE_NUMBER: 1
  HEADER_WITH_VALUE_STRING: hello everyone !
  HEADER_WITH_VALUE_COMPLEX_STRING: "hello everyone: it's me :D"
  HEADER_WITH_VALUE_ARRAY: "{ 14, 15, 28 }"
  HEADER_WITHOUT_VALUE: # ':' is mandatory, even if no value !!
disabled:
  - DISABLED_HEADER
  - DiSaBLeD_HEaDEr2
```
Notes:
- we don't care about the order
- we don't care about the constant location (in which file we enable/disable a constant), because if there is missing constant from output files, the script will exit and cancel any modification

# Regex:
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

# Lexic
- output: indicates `Configuration.h` and `Configuration_adv.h`

# Todo
- When a constant is disabled (profile) but enabled in output: just prefix with `//` without modifying the line
- When a constant is disabled (profile) and disabled in output: do nothing
- When a constant is enabled (profile) and enabled in output with same value (or no value for both): do nothing
- When a constant is enabled (profile) with value and enabled in output without value: ERROR
- When a constant is enabled (profile) without value and enabled in output with value: ERROR
- When a constant is enabled (profile) but disabled in output: remove the prefix `//` and adjust the value from profile without modifying the rest of the line
    Exception: if the constant contains a value while output have no value: ERROR
    Exception: if the constant have no value while output contains a value: ERROR 
- When a constant is not defined in profile while it is in output (enabled or disabled): warn like `Oh, XXX is not defined in profile`
- When a constant is defined in profile (enabled or disabled), while it isn't into output (non-present): ERROR
- Can create a profile.yaml from output
- Can create a backup.yaml from output

# Done
- Regex for parsing constants
- Design the `profile.yaml`
