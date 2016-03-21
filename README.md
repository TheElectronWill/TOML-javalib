# TOML-javalib
This is a fast and simple [TOML](https://github.com/toml-lang/toml) library.
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


## What does currently work?
Everything works fine! A valid TOML data is correctly parsed, and the TOMLWriter produces valid TOML files. The detection of errors could be improved a little.


