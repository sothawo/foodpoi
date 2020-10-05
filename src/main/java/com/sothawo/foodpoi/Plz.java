/*
 * (c) Copyright 2020 sothawo
 */
package com.sothawo.foodpoi;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.core.geo.GeoJson;
import org.springframework.data.elasticsearch.core.geo.GeoJsonPolygon;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@Document(indexName = "plz")
public class Plz {
    @Id
    private String plz;

    private GeoJson<?> geometry;

    public Plz() {
    }

    public Plz(String plz, GeoJson<?> geometry) {
        this.plz = plz;
        this.geometry = geometry;
    }

    public String getPlz() {
        return plz;
    }

    public void setPlz(String plz) {
        this.plz = plz;
    }

    public GeoJson<?> getGeometry() {
        return geometry;
    }

    public void setGeometry(GeoJson<?> geometry) {
        this.geometry = geometry;
    }
}
