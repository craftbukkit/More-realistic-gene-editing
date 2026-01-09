package com.morerealisticgeneediting.genome.provider;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.morerealisticgeneediting.genome.Genome;
import com.morerealisticgeneediting.genome.GenomeSlice;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class EnsemblRestProvider implements GenomeProvider {

    // ... (fields and canProvide remain the same) ...
    private static final String ENSEMBL_API_HOST = "https://rest.ensembl.org";
    private static final HttpClient httpClient = HttpClient.newHttpClient();
    private static final Gson gson = new Gson();

    @Override
    public boolean canProvide(String identifier) {
        return identifier != null && identifier.startsWith("ensembl:");
    }

    @Override
    public CompletableFuture<GenomeSlice> getSlice(String identifier, long start, int length) {
        // ... (parsing and request building is the same) ...
        String[] parts = identifier.split(":");
        if (parts.length != 4) { return CompletableFuture.failedFuture(new IllegalArgumentException("Invalid Ensembl identifier format.")); }
        String species = parts[1];
        String chromosome = parts[3];
        long ensemblStart = start + 1;
        long ensemblEnd = start + length;
        String path = String.format("/sequence/region/%s/%s:%d-%d?content-type=application/json", species, chromosome, ensemblStart, ensemblEnd);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENSEMBL_API_HOST + path))
                .header("Content-Type", "application/json")
                .build();
        
        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> parseAndCreateSlice(response.body(), start, identifier));
    }

    private GenomeSlice parseAndCreateSlice(String jsonBody, long sliceStartInGenome, String identifier) {
        JsonObject jsonObject = gson.fromJson(jsonBody, JsonObject.class);
        String sequence = jsonObject.get("seq").getAsString();
        
        // Placeholder for total length
        long conceptualTotalLength = sliceStartInGenome + sequence.length() + 10000; 

        Genome virtualGenome = Genome.createVirtualGenome(UUID.randomUUID(), sequence, sliceStartInGenome, conceptualTotalLength);

        // Use the updated constructor
        return new GenomeSlice(virtualGenome, sequence, sliceStartInGenome);
    }
}
