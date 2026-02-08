JLS
===

Java Logic Simulator

## Compiling

### Windows 10

With JDK installed:
```shell
javac -d ./bin -cp ./lib/jhall.jar ./src/jls/*.java ./src/jls/edit/*.java ./src/jls/elem/*.java ./src/jls/sim/*.java ./xz/*.java ./xz/org/tukaani/xz/*.java ./xz/org/tukaani/xz/check/*.java ./xz/org/tukaani/xz/common/*.java ./xz/org/tukaani/xz/delta/*.java ./xz/org/tukaani/xz/index/*.java ./xz/org/tukaani/xz/lz/*.java ./xz/org/tukaani/xz/lzma/*.java ./xz/org/tukaani/xz/rangecoder/*.java ./xz/org/tukaani/xz/simple/*.java
java -cp ./bin/ jls.JLS
```
