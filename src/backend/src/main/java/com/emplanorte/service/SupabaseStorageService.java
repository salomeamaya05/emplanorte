package com.emplanorte.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;

@Service
public class SupabaseStorageService {
    @Value("${supabase.url:}") private String supabaseUrl;
    @Value("${supabase.secret-key:}") private String secretKey;
    @Value("${supabase.storage.bucket:facturas-proveedores}") private String bucket;
    private final HttpClient client=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();

    public record ArchivoDescargado(byte[] contenido,String tipo,String nombre){}

    public void subir(String ruta,byte[] datos,String tipo){
        validar();
        try{
            HttpRequest req=HttpRequest.newBuilder(URI.create(base()+"/object/"+bucket+"/"+ruta))
                .timeout(Duration.ofSeconds(60)).header("apikey",secretKey)
                .headers(authorizationHeaders())
                .header("Content-Type",tipo).header("x-upsert","true").PUT(HttpRequest.BodyPublishers.ofByteArray(datos)).build();
            HttpResponse<String> res=client.send(req,HttpResponse.BodyHandlers.ofString());
            if(res.statusCode()<200||res.statusCode()>=300)throw new RuntimeException("Supabase Storage rechazó el archivo: "+res.body());
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException("Carga de archivo interrumpida");}
         catch(java.io.IOException e){throw new RuntimeException("No fue posible conectar con Supabase Storage");}
    }

    public ArchivoDescargado descargar(String ruta,String tipo,String nombre){
        validar();
        try{
            HttpRequest req=HttpRequest.newBuilder(URI.create(base()+"/object/"+bucket+"/"+ruta))
                .timeout(Duration.ofSeconds(60)).header("apikey",secretKey).headers(authorizationHeaders()).GET().build();
            HttpResponse<byte[]> res=client.send(req,HttpResponse.BodyHandlers.ofByteArray());
            if(res.statusCode()!=200)throw new RuntimeException("No fue posible descargar el soporte de la factura");
            return new ArchivoDescargado(res.body(),tipo!=null?tipo:"application/octet-stream",nombre!=null?nombre:"soporte");
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new RuntimeException("Descarga interrumpida");}
         catch(java.io.IOException e){throw new RuntimeException("No fue posible conectar con Supabase Storage");}
    }
    private String base(){return supabaseUrl.replaceAll("/+$","")+"/storage/v1";}
    private String[] authorizationHeaders(){
        // Las claves legacy service_role son JWT y usan Bearer. Las nuevas sb_secret_* se envían solo como apikey.
        return secretKey != null && secretKey.startsWith("eyJ")
            ? new String[]{"Authorization", "Bearer " + secretKey}
            : new String[0];
    }
    private void validar(){if(supabaseUrl==null||supabaseUrl.isBlank()||secretKey==null||secretKey.isBlank())throw new RuntimeException("Faltan SUPABASE_URL y SUPABASE_SECRET_KEY (o SUPABASE_SERVICE_ROLE_KEY) en Render/local");}
}
