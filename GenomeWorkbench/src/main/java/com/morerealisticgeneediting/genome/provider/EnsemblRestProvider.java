package com.morerealisticgeneediting.genome.provider;
import com.google.gson.*;
import com.morerealisticgeneediting.genome.GenomeSlice;
import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
/** Bounded public reference lookup. Does not guess chromosome size or silently change assembly. */
public final class EnsemblRestProvider implements GenomeProvider {
    private static final HttpClient CLIENT=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NEVER).build();
    private static final int MAX_RESPONSE_BYTES=250_000;
    @Override public boolean canProvide(String identifier){return identifier!=null&&identifier.startsWith("ensembl:");}
    @Override public CompletableFuture<GenomeSlice> getSlice(String identifier,long start,int length){
        try{
            String[] parts=identifier.split(":",-1);
            if(parts.length!=4||!parts[0].equals("ensembl")||!parts[1].matches("[a-z_]{1,64}")||!parts[2].matches("[A-Za-z0-9_.-]{1,64}")||!parts[3].matches("[A-Za-z0-9_.-]{1,64}")||start<0||length<1||length>100_000||start>Long.MAX_VALUE-length)
                throw new IllegalArgumentException("Invalid Ensembl reference or window");
            String path="https://rest.ensembl.org/sequence/region/"+parts[1]+"/"+parts[3]+":"+(start+1)+"-"+(start+length)+":1?coord_system_version="+parts[2];
            HttpRequest request=HttpRequest.newBuilder(URI.create(path)).timeout(Duration.ofSeconds(15)).header("Accept","application/json").GET().build();
            java.util.concurrent.atomic.AtomicReference<InputStream> activeBody=new java.util.concurrent.atomic.AtomicReference<>();
            return CLIENT.sendAsync(request,HttpResponse.BodyHandlers.ofInputStream()).thenApplyAsync(response->{
                activeBody.set(response.body());
                try(InputStream input=response.body()){
                    if(response.statusCode()!=200)throw new IOException("Ensembl returned HTTP "+response.statusCode());
                    byte[] bytes=input.readNBytes(MAX_RESPONSE_BYTES+1);
                    if(bytes.length>MAX_RESPONSE_BYTES)throw new IOException("Ensembl response exceeds limit");
                    JsonObject json=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();
                    if(!json.has("seq"))throw new IOException("Missing Ensembl sequence");
                    String sequence=json.get("seq").getAsString();
                    if(sequence.length()!=length)throw new IOException("Ensembl returned an unexpected window length");
                    // The sequence endpoint does not supply the whole chromosome length.
                    return new GenomeSlice(sequence,start,-1,identifier);
                }catch(IOException|RuntimeException failure){throw new java.util.concurrent.CompletionException(failure);}
            }).orTimeout(20,java.util.concurrent.TimeUnit.SECONDS).whenComplete((result,failure)->{
                if(failure!=null){InputStream body=activeBody.get();if(body!=null)try{body.close();}catch(IOException ignored){}}
            });
        }catch(RuntimeException failure){return CompletableFuture.failedFuture(failure);}
    }
}
