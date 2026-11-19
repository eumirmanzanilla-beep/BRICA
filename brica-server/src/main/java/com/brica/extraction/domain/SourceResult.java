package com.brica.extraction.domain;
import com.brica.server.shared.validation.domain.ExtractionRules;

import java.util.*;

public record SourceResult(long attachmentId, String sha256, String readerVersion, List<Fragment> fragments, List<String> warnings) {
    public record Location(Integer page, String sheet, Integer row, Integer column, String part,
            Float x, Float y, Float width, Float height) { }
    public record Fragment(UUID id, long attachmentId, Location location, String original, String formula, String storedValue,
            String kind, boolean hidden, String warning) {
        public Fragment {
            ExtractionRules.require(id!=null && attachmentId>0 && location!=null,"fragment","Identifica fuente, fragmento y ubicación.");
            ExtractionRules.text("original",original,16384,false); ExtractionRules.text("formula",formula,16384,false);
            ExtractionRules.text("storedValue",storedValue,16384,false); ExtractionRules.text("kind",kind,32,true); ExtractionRules.text("warning",warning,1000,false);
            ExtractionRules.text("sheet",location.sheet(),255,false); ExtractionRules.text("part",location.part(),1000,false);
            ExtractionRules.require(location.page()==null || location.page()>=1 && location.page()<=40,"page","Página fuera del límite.");
            ExtractionRules.require(location.row()==null || location.row()>=1 && location.row()<=1048576,"row","Fila no válida.");
            ExtractionRules.require(location.column()==null || location.column()>=1 && location.column()<=256,"column","Columna no válida.");
            for(Float coordinate:new Float[]{location.x(),location.y(),location.width(),location.height()}) {
                ExtractionRules.require(coordinate==null || Float.isFinite(coordinate),"location","Coordenadas no finitas.");
            }
        }
    }
    public SourceResult {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        ExtractionRules.require(attachmentId>0 && sha256!=null && sha256.matches("[a-f0-9]{64}"),"source","Identifica la fuente y su hash SHA-256.");
        ExtractionRules.text("readerVersion",readerVersion,80,true); warnings.forEach(w->ExtractionRules.text("warning",w,1000,true));
        ExtractionRules.require(fragments.size() <= 200_000 && warnings.size() <= 200, "result", "Resultado excedido.");
    }
}
