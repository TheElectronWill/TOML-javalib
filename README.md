# TOML-javalib
This is a little and simple [TOML](https://github.com/toml-lang/toml) library.
Its goal is to allow Java developers to easily use the TOML format in their applications.
This library is compatible with TOML v0.4.0.

## How to use
The main class is Toml.java. It contains public static methods for reading and writing TOML data.  
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

## What does currently work?
The TomlWriter works and is very fast, but it has a little problem: it may write the single key/value pair (those who are in the root table) at the end of the file, which is invalid.  
The TomlReader works pretty fine, and is also very fast (It successfully parsed the "example.toml" test file).
