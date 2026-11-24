# Spring PetClinic on darks-jvm

Builds [Spring PetClinic 2.7.3](https://github.com/spring-projects/spring-petclinic)
with a JDK 11, runs the fat jar on darks-jvm and checks a few pages with curl.

## Usage

Build darks-jvm first (`cmake --build build -j`), then:

```bash
./examples/spring-petclinic/run.sh
```

The first run clones PetClinic and runs Maven (a few minutes); starting the
app on darks-jvm takes roughly 17-20 minutes. The script exits non-zero when a
check fails and prints the log path. Logs live in `examples/spring-petclinic/work/`.

Overrides (all optional):

| Variable | Default | Meaning |
| --- | --- | --- |
| `PORT` | `8090` | HTTP port |
| `WORK_DIR` | `examples/spring-petclinic/work` | clone/build/logs |
| `JAVA11_HOME` | `/usr/lib/jvm/java-11-openjdk-amd64` | JDK 11 used for Maven |
| `DARKS_JVM` | `build/bin/darks-jvm` | darks-jvm executable |
| `STARTUP_TIMEOUT` | `1800` | seconds to wait for the port |

Smoke checks: `/` (Welcome), `/owners/find` (Find Owners), `/owners/2`
(Owner Information, Betty Davis).

## Windows

`run.sh` is a bash script and has not been ported to PowerShell. It runs under
MSYS2 (or Git Bash) but assumes POSIX tools and a JDK 11 for Maven; on Windows
run the same steps manually (clone PetClinic at `9ecdc11`, `mvn -DskipTests
package` with a JDK 11, then
`build\bin\darks-jvm.exe -cp build\classes -jar target\spring-petclinic-2.7.3.jar`).
Keep `darks-jvm.exe`, `darksvm.dll` and `native.dll` in `build\bin`, with the
MSYS2 `mingw64\bin` directory on `PATH` for the runtime DLLs.
