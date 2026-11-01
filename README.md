# More Realistic Gene Editing

MRGE is an experimental educational Minecraft mod with virtual genome samples, sequence editing, laboratory equipment and research projects. The supported development baseline is **Minecraft 1.21, Fabric and Java 21**, using Yarn mappings. Minecraft 26.1 / Java 25 is a possible future port, not the current build target.

## Build

Install JDK 21, then run:

```sh
cd GenomeWorkbench
./gradlew clean build
```

The checked-in Gradle 8.8 wrapper verifies its distribution checksum. Tests cover sequence coordinate and persistence rules, scientific simulation invariants, request bounds, and research evidence. The distributable mod JAR is written to `GenomeWorkbench/build/libs/`.

## Development scope

Sample genomes and instrument results are simplified game simulations. Real therapy names in the research catalogue are explanatory case studies; successful virtual editing is not evidence that a real therapy has been reproduced. Scientific assumptions, corrected mechanisms and limitations are documented in [docs/scientific-model.md](docs/scientific-model.md).

The playable workflow uses owned samples and server-side sequence data. Edits and verification must refer to the same sample revision; results and machine jobs must survive saving and reloading. The historical planning PDFs describe future features as well as implemented work, and are retained as design references.

Artwork remains in development. The audit's missing textures and models require a separate art pass. The `apps`, `services` and `infra` directories contain an independent web prototype and are not part of the Minecraft mod build.

## Verification

Run `./gradlew test` from `GenomeWorkbench` for regression tests. Client and dedicated-server launch checks are separate from unit tests. A successful build does not by itself establish gameplay balance or validate clinical predictions.
