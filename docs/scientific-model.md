# Scientific model and evidence contract

MRGE uses fictional Minecraft samples and teaching models. A stored sample, its edits, a simulated instrument observation, and the conclusion drawn from that observation are separate records. A random success flag or decorative graph does not establish an edit, phenotype or clinical effect. In-game time compression and resource recipes are gameplay choices.

## Playable sample exercise

Use `/research list`, start and complete `welcome_to_the_lab`, then start `simulated_sample_edit`. Capture a virtual sample, submit an actual sequence edit, and run sequencing concordance validation of the same sample's current revision. `/research status` shows its sample UUID and sampled, edited and verified revisions. Completion is server-controlled; starting a project is insufficient.

Verification means sequence concordance within this virtual model: newly simulated reads must provide quality-supported coverage and consensus agreement with the stored edited sample. Normalizing read orientation is required. Editing provenance establishes the preceding change. This does not assert blind indel discovery, detection of every off-target event, protein function or efficacy. Merely requesting a genome window, generating a read-set label or renaming an animal is not verification.

Project evidence is isolated by player and sample and persists with the world. A newer edit invalidates the older verification. At most 32 sample evidence histories per player are retained; capturing additional samples can evict the oldest history. Old project saves that predate evidence validation do not retain unverified completion flags.

## Corrected real-world reference cases

| Case | Mechanism represented in reading material | Distinctions retained |
| --- | --- | --- |
| Casgevy / exa-cel | Editing the BCL11A erythroid enhancer raises HbF through altered regulation. | Target editing, HbF expression, and clinical outcomes require different evidence; generic PCR or gel bands do not quantify cutting efficiency. |
| BEAM-101 | Base editing HBG1/HBG2 promoters promotes HbF expression. | It is not direct reversal of HBB rs334. HbS commonly corresponds to HBB c.20A>T. |
| NCF1 / CGD | Prime editing research addresses ΔGT-associated p47phox deficiency. | Sequence repair, expression and oxidative-burst function are separate; no prescribed editor variant or universally effective design is asserted. |
| LPA / Lp(a) | LPA encodes apolipoprotein(a); Lp(a) is a lipoprotein particle. | A gene name, encoded protein and circulating biomarker are not interchangeable. |
| PERT | Prime editing converts redundant endogenous tRNA genes into suppressor tRNAs. | This is not safe-harbor insertion of an exogenous tRNA; full-length protein, protein function and unintended termination readthrough require separate assessment. |
| Allogeneic CAR-T | TCR reduction can reduce alloreactivity. | Graft-versus-host reactions and host rejection are distinct. TCR or PD-1 editing does not guarantee safety or stronger antitumor effects. |

These named clinical examples are read-only reference cases. The current virtual-sample tools cannot establish their target-specific, cellular or clinical endpoints. Generic editing evidence cannot complete them or award a clinical achievement. Future playable versions must define and implement their own evidence contracts before enabling completion.

## Algorithm and observation semantics

- Nucleases require distinct PAM orientation, target strand and cut geometry. Coordinate conventions must remain explicit across selection, editing and read-back.
- A single virtual allele is not a cell population. An indel length alone cannot determine mosaicism or frameshift without population or coding-frame context. Unknown quantities are displayed as not evaluated, not zero risk.
- Read sequence and quality arrays are equal length; reverse reads and quality order are normalized before comparison. Read-length N50 is not assembly contig N50.
- Gel migration requires time and a driving electric field. Zero time or zero field gives zero migration. Gel bands are derived from sample fragments and remain visible after completion.
- A visible-only 340–1000 nm instrument cannot measure A230, A260 or A280. Stable sample properties underlie repeated measurements; measurement noise does not create a different sample.
- CFU/µg is a microbial transformation metric. Delivery, survival, expression, genome editing and colony formation are separate outcomes, with explicit units and cell-type applicability.
- Centrifugation separates fractions; nucleic-acid extraction needs a separately represented preparation operation. Sample purity, quantity and sequence identity cannot be inferred from item names alone.

## Corrections to historical planning PDFs

The original PDFs are retained as historical design material. The following corrections take precedence for implementation.

| Historical location | Required interpretation |
| --- | --- |
| `全自动细胞处理系统CliniMACS Prodigy.pdf`, p. 1, magnetic separation | Separation depends on specific magnetic labeling; strong magnets do not directly and generally attract cells. The beads mentioned on p. 3 must have a defined target/label relation. |
| Same PDF, p. 4, liquid appearance | Turbidity is at most an illustrative observation of suspended material; it cannot demonstrate editing success, sterile culture or correct phenotype. |
| Same PDF, p. 4, non-viral transfer | Separate delivery, viability, expression and genome editing. None of these measurements alone proves the others. |
| `更真实的基因编辑模组_草案阶段_2026-05-24.pdf`, p. 7, `max(1, floor(...))` | Determine success before calculating product yield. Zero expansion or a failed batch cannot produce a guaranteed qualifying cell product. A consolation research note must be a different outcome. |
| Same draft, pp. 3, 6–8, TS520/gas/Day 0–11 matrix | These are planned abstractions. A simple machine recipe must not be described as implementing the complete cell-processing workflow. |

## Sources

Mechanism references, not laboratory instructions:

- Beam Therapeutics, BEAM-101 mechanism: https://beamtx.com/pipeline_candidates/beam-101/
- HBG1/HBG2 promoter base-editing clinical study: https://www.nejm.org/doi/10.1056/NEJMoa2504835
- NCBI ClinVar, HBB c.20A>T: https://www.ncbi.nlm.nih.gov/clinvar/RCV000224000/
- Casgevy label: https://dailymed.nlm.nih.gov/dailymed/fda/fdaDrugXsl.cfm?setid=7c3e12ad-e2fe-4d3f-a630-ea7364d9e846&type=display
- PERT original research: https://www.nature.com/articles/s41586-025-09732-2
- NCBI Gene, LPA: https://www.ncbi.nlm.nih.gov/gene/4018
- NCF1 prime-editing research: https://www.nejm.org/doi/10.1056/NEJMoa2509807
- Prime-editing mechanism: https://www.nature.com/articles/s41586-019-1711-4
- Cas12a/Cpf1 original research: https://doi.org/10.1016/j.cell.2015.09.038
- Allogeneic CAR-T study: https://doi.org/10.1038/s41467-026-71904-z
- Illumina quality-score definition: https://www.illumina.com/science/technology/next-generation-sequencing/plan-experiments/quality-scores.html
- NEB transformation-efficiency definition: https://www.neb.com/en/faqs/how-should-i-calculate-the-transformation-efficiency
