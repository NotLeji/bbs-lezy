# BBS Lezy Headless Replay Benchmark (rbp-1000)

## Overview
Automated headless benchmark harness using `HeadlessMC`, `mc-runtime-test`, and `ReplayBench` in BBS Lezy to sample in-world film replay playback performance and capture JDK Flight Recorder (JFR) execution profiles.

## Setup & Staging
1. Ensure the mod jar is built:
   ```bash
   sh ./gradlew build --no-daemon
   cp build/libs/bbs-lezy-*.jar run/mods/
   ```
2. Verify `run/mods/` contains:
   - `bbs-2.7-1.20.1.jar`
   - `fabric-api-0.92.12+1.20.1.jar`
   - `mc-runtime-test-1.20.1-4.5.2-fabric-release.jar`
   - `bbs-lezy-0.4.2.jar`
3. Verify test film is staged at `run/bench-films/<filmId>.dat` (e.g. `OP-perang-1.dat`).

## Running the Benchmark
Execute the headless runner via Xvfb:
```bash
PWD=$(pwd)
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" \
  java -Dhmc.xvfb.check=true -jar headlessmc.jar --command \
  "launch 1 --jvm \"-Djava.awt.headless=true -Dbbslezy.bench=<filmId>,<warmupTicks>,<sampleTicks>,<withCamera> -XX:StartFlightRecording=filename=$PWD/build/jfr/<profile-name>.jfr,dumponexit=true,maxsize=1024M\""
```

Example for 600 warmup ticks and 1800 sample ticks on film `OP-perang-1`:
```bash
PWD=$(pwd)
xvfb-run -a -s "-screen 0 1920x1080x24 +extension GLX +render -noreset" \
  java -Dhmc.xvfb.check=true -jar headlessmc.jar --command \
  "launch 1 --jvm \"-Djava.awt.headless=true -Dbbslezy.bench=OP-perang-1,600,1800,true -XX:StartFlightRecording=filename=$PWD/build/jfr/baseline-world.jfr,dumponexit=true,maxsize=1024M\""
```

## Reading the Results
- Realtime FPS and summary lines are logged to `run/logs/latest.log` with prefix `BBSLezy[bench]`.
- JFR summary and execution samples:
  ```bash
  /usr/lib/jvm/java-17-openjdk/bin/jfr summary build/jfr/<profile-name>.jfr > docs/perf/<profile-name>-summary.txt
  /usr/lib/jvm/java-17-openjdk/bin/jfr print --events jdk.ExecutionSample build/jfr/<profile-name>.jfr > docs/perf/<profile-name>-samples.txt
  ```
