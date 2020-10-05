/*
 * (c) Copyright 2020 sothawo
 */
package com.sothawo.foodpoi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPolygon;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@RestController
@RequestMapping("/plz")
public class PlzController {

    private final ElasticsearchOperations operations;

    public PlzController(ElasticsearchOperations operations) {
        this.operations = operations;
    }


    @GetMapping("/load")
    public void load() throws IOException {
        IndexOperations indexOps = operations.indexOps(Plz.class);
        indexOps.delete();
        indexOps.create();
        indexOps.putMapping(Plz.class);

        ObjectMapper objectMapper = new ObjectMapper();
        try (FileInputStream fis = new FileInputStream("plz-2stellig.geojson")) {

            Map map = objectMapper.readValue(fis, Map.class);

            Document document = Document.from(map);
            List<Map<String, Object>> features = (List<Map<String, Object>>) document.get("features");

            ConversionService conversionService = operations.getElasticsearchConverter().getConversionService();
            MapToFeatureConverter converter = new MapToFeatureConverter(conversionService);

            features.stream()
                .map(converter::convert)
                .map(it -> new Plz(it.getProperties().getPlz(), it.getGeometry()))
                .forEach(operations::save);
        }
    }

    static class Feature {
        private String type;
        private GeoJson<?> geometry;
        private Props properties;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public GeoJson<?> getGeometry() {
            return geometry;
        }

        public void setGeometry(GeoJsonPolygon geometry) {
            this.geometry = geometry;
        }

        public Props getProperties() {
            return properties;
        }

        public void setProperties(Props properties) {
            this.properties = properties;
        }
    }

    static class Props {
        private String plz;
        private Double qkm;
        private Integer einwohner;

        public String getPlz() {
            return plz;
        }

        public void setPlz(String plz) {
            this.plz = plz;
        }

        public Double getQkm() {
            return qkm;
        }

        public void setQkm(Double qkm) {
            this.qkm = qkm;
        }

        public Integer getEinwohner() {
            return einwohner;
        }

        public void setEinwohner(Integer einwohner) {
            this.einwohner = einwohner;
        }
    }

    @ReadingConverter
    static class MapToFeatureConverter implements Converter<Map<String, Object>, Feature> {

        private final ConversionService conversionService;

        MapToFeatureConverter(ConversionService conversionService) {
            this.conversionService = conversionService;
        }

        @Override
        public Feature convert(Map<String, Object> source) {

            Feature feature = new Feature();
            feature.setType((String) source.get("type"));

            Map<String, Object> geometry = (Map<String, Object>) source.get("geometry");

            GeoJson<?> geoJsonPolygon = conversionService.convert(geometry, GeoJson.class);
            feature.geometry = geoJsonPolygon;

            Map<String, Object> properties = (Map<String, Object>) source.get("properties");

            Props props = new Props();
            props.setPlz((String) properties.get("plz"));
            props.setEinwohner((Integer) properties.get("einwohner"));
            props.setQkm((Double) properties.get("qkm"));

            feature.setProperties(props);
            return feature;
        }
    }
}
