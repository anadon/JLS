classes: ./src/jls/*.java ./src/jls/edit/*.java ./src/jls/elem/*.java ./src/jls/edit/*.java ./src/jls/sim/*.java ./xz/*.java ./xz/org/tukaani/xz/*.java ./xz/org/tukaani/xz/check/*.java ./xz/org/tukaani/xz/common/*.java ./xz/org/tukaani/xz/delta/*.java ./xz/org/tukaani/xz/index/*.java ./xz/org/tukaani/xz/lz/*.java ./xz/org/tukaani/xz/lzma/*.java ./xz/org/tukaani/xz/rangecoder/*.java ./xz/org/tukaani/xz/simple/*.java
	javac -d ./bin -cp ./lib/jhall.jar $^

# not working - "Could not find or load main class bin.jls.JLS"
# tried JLS, jls.JSL, and bin.jls.JLS
JLS.jar: ./bin/jls/*.class ./bin/jls/edit/*.class ./bin/jls/elem/*.class ./bin/jls/edit/*.class ./bin/jls/sim/*.class ./bin/*.class ./bin/org/tukaani/xz/*.class ./bin/org/tukaani/xz/check/*.class ./bin/org/tukaani/xz/common/*.class ./bin/org/tukaani/xz/delta/*.class ./bin/org/tukaani/xz/index/*.class ./bin/org/tukaani/xz/lz/*.class ./bin/org/tukaani/xz/lzma/*.class ./bin/org/tukaani/xz/rangecoder/*.class ./bin/org/tukaani/xz/simple/*.class
	jar cfe JLS.jar JLS $^
