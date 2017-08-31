# fn8-io

fn8-io is a work in progress.
The aim is to emulate I/O to fn8 emulator/interpreter.

## fn8

fn8 is a CHIP-8 emulator implemented in clojure.

For detailed description of the CHIP-8 have a look at the wikipedia article
https://en.wikipedia.org/wiki/CHIP-8

<img src="/doc/pong.png">

## Usage

lein cljsbuild once min

open resources/public/index.html

To use the emulator, you need a chip-8 program to run.

There is plenty to find on internet and after you have downloaded a program,
you can load it into memory by clicking the file icon.

To start running the program, you simply click the play icon.

### Bugs
Probably, yes.

### Why?
Wouldn't it be booring to just have an fn8 interpreter consuming energy without a convinient way using it?


## License

Copyright © 2017 David Bern

MIT License