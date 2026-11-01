package com.morerealisticgeneediting.project;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ScientificContentTest {
    private Map<String,ResearchProject> load() throws Exception {
        try(var stream=getClass().getResourceAsStream("/data/morerealisticgeneediting/projects.json")){
            assertNotNull(stream);List<ResearchProject> rows=new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),new TypeToken<List<ResearchProject>>(){}.getType());
            Map<String,ResearchProject> result=new HashMap<>();for(var row:rows)assertNull(result.put(row.id(),row));return result;
        }
    }
    @Test void projectGraphAndCompletionScopeAreValid() throws Exception {
        var rows=load();for(var p:rows.values())assertTrue(rows.keySet().containsAll(p.prerequisites()));
        assertEquals("sample_edit_verify",rows.get("simulated_sample_edit").completionMode());
        for(String id:List.of("blood_disease_beam101","rare_disease_pert","blood_disease_casgevy","immunotherapy_cart"))assertTrue(rows.get(id).isReferenceOnly());
    }
    @Test void namedTherapiesDescribeCorrectTargetsAndSeparateReadouts() throws Exception {
        var rows=load();var beam=rows.get("blood_disease_beam101");
        assertTrue(beam.description().contains("HBG1/HBG2"));assertTrue(beam.description().contains("HbF"));
        var pert=rows.get("rare_disease_pert");assertTrue(pert.description().contains("内源 tRNA"));
        assertTrue(rows.get("cardio_therapy_lnp").description().contains("LPA"));
        assertTrue(beam.sources().stream().anyMatch(s->s.startsWith("https://beamtx.com/")));
        assertTrue(pert.sources().stream().anyMatch(s->s.startsWith("https://www.nature.com/")));
    }
}
