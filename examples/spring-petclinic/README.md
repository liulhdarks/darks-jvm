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
