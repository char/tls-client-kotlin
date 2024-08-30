# tls-client-kotlin

(Work-in-progress) Kotlin bindings for [tls-client](https://github.com/bogdanfinn/tls-client).

## Setup

```shell
tls-client-kotlin/ $ cd go
tls-client-kotlin/go/ $ export CGO_ENABLED=1
tls-client-kotlin/go/ $ export CGO_CFLAGS="-I$JAVA_HOME/include -I$JAVA_HOME/include/linux"
tls-client-kotlin/go/ $ go build -buildmode=c-shared -o ./dist/tls-client-linux-amd64.so
tls-client-kotlin/go/ $ cd ../kotlin
tls-client-kotlin/kotlin/ $ ./gradlew build
```
