# marlin-console-configurator

![Release](https://github.com/Chuckame/marlin-console-configurator/workflows/Release/badge.svg)

[Example profile.yaml](example/profile.yaml)
Notes:
- we don't care about the order
- we don't care about the constant location (in which file we enable/disable a constant), because if there is missing constant from output files, the script will exit without doing any modification

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
- Can create a profileProperties.yaml from output
- Can create a backup.yaml from output

# Done
- Regex for parsing constants
- Design the `profileProperties.yaml`
- When a constant is disabled (profileProperties) but enabled in output: just prefix with `//` without modifying the line
- When a constant is disabled (profileProperties) and disabled in output: do nothing
- When a constant is enabled (profileProperties) and enabled in output with same value (or no value for both): do nothing
- When a constant is enabled (profileProperties) with value and enabled in output without value: ERROR
- When a constant is enabled (profileProperties) without value and enabled in output with value: ERROR
- When a constant is defined in profileProperties (enabled or disabled), while it isn't into output (non-present): ERROR
- When a constant is enabled (profileProperties) but disabled in output: remove the prefix `//` and adjust the value from profileProperties without modifying the rest of the line.
- When a constant is not defined in profileProperties while it is in output (enabled or disabled): warn like `Oh, XXX is not defined in profileProperties`
