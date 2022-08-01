# Kaitai Struct support plugin for IntelliJ

This plugin adds basic support for editing [Kaitai Struct](https://kaitai.io) files in IntelliJ IDEA and other
JetBrains' IDEs. The earliest supported version of the IDEs set is 2021.1.

## What is there

* File type recognition
* Schema validation

## What is not there

Pretty much everything else:

* Imports path validation
* Autocompletion support
* Rename refactoring
* etc.

## FAQ

### How do I run this?

Right now the plugin is simply not complete enough to be put on the JetBrains plugin market, so for now unless you plan
to test this or work on it, just hold on until things are ready. That said, once you can successfully build the plugin
using the instructions in the appropriate section, executing the `runIde` Gradle task should do the trick.

If that's good enough for you, you can run `gradle buildPlugin` in the plugin's root directory, and a file called
`build/libs/Kaitai-1.0-SNAPSHOT.jar` should have been created if everything went right. You can then manually install
the plugin by pointing the IDE to that file when importing external plugins.

## Licences and copyright

* Both the IDE filetype icon and the plugin's own icon come from a traced version
  of [Kaitai's logo](https://kaitai.io/img/kaitai_16x_dark.png).
* The included JSON schema comes from [Kaitai's own schema repository](https://github.com/kaitai-io/ksy_schema).

A copy of the Apache 2.0 licence is available in the repository as `LICENCE.txt`.

Where it applies: Copyright 2022 Alessandro Gatti - frob.it
