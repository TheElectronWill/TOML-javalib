# TOML-javalib

**⚠️ Warning: this library is no longer maintained. Recent versions of TOML aren't supported, and there can be bugs.  
Please head over to the [Night-Config project](https://github.com/TheElectronWill/night-config).**

This *was* a simple [TOML](https://github.com/toml-lang/toml) library.
Its goal is to allow Java developers to easily use the TOML format in their applications.
This library is compatible with TOML v0.4.0.

## How to use
The important class is Toml.java. It contains public static methods for reading and writing TOML data.  
You can read data like this:
```java
//import com.electronwill.toml.Toml;
File file = new File("myFile.toml");
Map<String, Object> data = Toml.read(file);
```

And write data like this:
```java
//import com.electronwill.toml.Toml;
File file = new File("myFile.toml");
Map<String, Object> data = ...//put your data in a Map
Toml.write(data, file);
```

You may also use the TomlReader and TomlWriter classes directly.

## Data types
The TOML data is mapped to the following java types:

TOML | Java
---- | ----
Integer | `int` or `long` (it depends on the size)
Decimal | `double`
String (all types of string: basic, literal, multiline, ...) | `String`
DateTime | `ZonedDateTime`, `LocalDateTime` or `LocalDate` (it depends on what informations are available, see the comments in Toml.java)
Array | `List`
Table | `Map<String, Object>`

## Lenient bare keys
This library supports (since v1.1) "lenient" **and** "strict" bare keys. Strict bare keys are those defined in the TOML specification. Lenient bare keys are less restrictive and much more practical. Here is a comparison between strict and lenient bare keys:

/               | Strict bare keys (TOML specification) | Lenient bare keys (Option of this library)
----------------|---------------------------------------|---------------------------
**May contain** | ASCII alphanumeric characters only: `A-Z a-z 0-9 _ -`   | Any character after the space one in the unicode table, except the following ones: `. [ ] # =`

By default, this library will be lenient when reading some TOML data. You may choose to be strict by adding some parameters to the read method:
```java
String dataString = ... //your TOML data
map = Toml.read(dataString);//lenient. This actually calls Toml.read(dataString, false)
map = Toml.read(dataString, true);//strict!
```
Write operations of TOML-javalib are *not* affected by this feature: they will always output data in compliance with the TOML specification. Any key with a non strictly valid character will be surrounded by quotes.

## What does currently work?
Everything works fine! A valid TOML data is correctly parsed, and the TOMLWriter produces valid TOML files. There might be some minor improvement to do and some features to add.

## Java version
This library requires Java 8.

