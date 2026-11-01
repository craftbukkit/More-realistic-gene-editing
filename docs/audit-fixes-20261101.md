# Science and code audit corrections — 2026-11-01

This change addresses the science and Minecraft-mod code findings from the 2026-10-30 source audit. The supported target is Minecraft 1.21 / Fabric / Java 21. The independent web prototype and unfinished artwork are outside this change.

| Audit area | Resulting behavior | Regression coverage |
| --- | --- | --- |
| Build and registration (C01–C05) | Pinned Yarn/Loom configuration, restored checksum-verified Gradle wrapper, one canonical registry, working block-entity factories and menu registration, separate client source set | Full main/client compilation and JAR remapping in CI |
| Genome data and providers (C06–C11, I05) | Owned immutable sample revisions, explicit reference/edit coordinates, world persistence, bounded validated providers and caches; corrupted records are rejected | `GenomeDataTest` |
| Network authority (N01–N04) | Typed payloads, bounded requests, owned active-menu/sample/revision checks; client messages cannot manufacture project evidence | `GenomeRequestPolicyTest`, `SafeNBTTest`, project rule tests |
| Inventory and job persistence (C12, I01–I04, I06–I07) | Server-tick jobs reserve inputs, preserve pending work, check output identity/capacity, and commit once; offline owners retain valid sequencing evidence | `InventorySafetyTest`; world-level restart and automation playtesting remains separate |
| Editing, PCR, sequencing and gel (BIO07–BIO13) | Strand/PAM geometry, valid edit bounds, no-op HDR detection, PCR input requirements, read/quality orientation, FASTQ lengths, correct N50, no invented variant counts and zero-drive migration | `ScientificAlgorithmsTest`, sequence verification tests |
| Instruments and sample semantics (BIO14–BIO17) | Stable sample measurements, spectral limits, distinct delivery/survival/editing concepts, actual fragment-derived bands, explicit fractionation; DNA aliquots are separate from fictional cell cultures | `MeasurementModelsTest`, inventory tests |
| Research content (BIO01–BIO06, BIO21–BIO22) | Corrected mechanisms; clinical examples are reading material with completion disabled. The playable sample exercise requires a sampled, edited, sequence-verified revision belonging to the same player | `ScientificContentTest`, `ProjectCompletionRulesTest`, `ProjectPersistenceTest` |
| Historical plans (BIO18–BIO20) | Explicit corrections and implementation boundaries take precedence over the retained PDFs | [Scientific model](scientific-model.md) |

## Intentional limits

The algorithms remain teaching simulations with fictional samples and illustrative instrument presets. They do not predict clinical efficacy, whole-organism phenotype, population mosaicism or experimentally validated off-target risk. Values requiring an unimplemented model are represented as unknown, rather than invented measurements.

The original PDFs include a more extensive cell-processing workflow that is still a design goal. This change does not relabel simple recipes as that complete system. Existing saves with unverified project-completion flags must earn valid evidence again; a full migration from every unfinished prototype save format is not promised.

Compilation and automated tests are distinct from interactive client playtesting and full world save/reload testing. Build outputs and exact test reports are published by the repository's GitHub Actions workflow.
