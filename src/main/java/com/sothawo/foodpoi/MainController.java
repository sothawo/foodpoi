/*
 Copyright 2020 Peter-Josef Meisch (pj.meisch@sothawo.com)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package com.sothawo.foodpoi;

import com.sothawo.mapjfx.Configuration;
import com.sothawo.mapjfx.Coordinate;
import com.sothawo.mapjfx.Extent;
import com.sothawo.mapjfx.MapLabel;
import com.sothawo.mapjfx.MapType;
import com.sothawo.mapjfx.MapView;
import com.sothawo.mapjfx.Marker;
import com.sothawo.mapjfx.event.MapViewEvent;
import io.reactivex.rxjavafx.observables.JavaFxObservable;
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import net.rgielen.fxweaver.core.FxmlView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.geo.GeoBox;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.data.elasticsearch.core.query.GeoDistanceOrder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
@Component
@FxmlView("/MainController.fxml") // if / is omitted, resource loader expects the fxml in the package
public class MainController {

    private static final Logger LOG = LoggerFactory.getLogger(MainController.class);

    private static final int ZOOM_DEFAULT = 14;
    private static final Coordinate coordKarlsruheCastle = new Coordinate(49.013517, 8.404435);

    private final FoodPOIConfiguration configuration;
    private final FoodPOIRepository repository;

    private final List<MapLabel> labelsMaster = new ArrayList<>();
    private final Marker markerMaster = Marker.createProvided(Marker.Provided.GREEN);

    private Extent extentSlave;
    private final List<MapLabelSlave> labelsSlave = new ArrayList<>();

    @FXML
    private MapView mapViewSlave;
    @FXML
    private MapView mapViewMaster;
    @FXML
    private Label labelMaster;
    @FXML
    private Label labelSlave;

    public MainController(FoodPOIConfiguration configuration, FoodPOIRepository repository) {
        this.configuration = configuration;
        this.repository = repository;
    }

    @FXML
    public void initialize() {
        initMapViewSlave();
        initMapViewMaster();
    }

    private void initMapViewMaster() {
        MapType mapType = MapType.OSM;
        if (configuration.getBingMapsApiKey() != null) {
            mapViewMaster.setBingMapsApiKey(configuration.getBingMapsApiKey());
            mapType = MapType.BINGMAPS_ROAD;
        }

        initMap(mapViewMaster, mapType, Configuration.builder().build());
        markerMaster.setVisible(false);

        mapViewMaster.addEventHandler(MapViewEvent.MAP_CLICKED, event -> {
            event.consume();

            if (mapViewMaster.getInitialized()) {

                Coordinate newPosition = event.getCoordinate().normalize();
                markerMaster.setPosition(newPosition);
                if (!markerMaster.getVisible()) {
                    mapViewMaster.addMarker(markerMaster);
                    markerMaster.setVisible(true);
                }
                labelMaster.textProperty().set(newPosition.toString());

                GeoPoint geoPoint = new GeoPoint(newPosition.getLatitude(), newPosition.getLongitude());
                SearchHits<FoodPOI> searchHits = repository.searchTop5By(Sort.by(new GeoDistanceOrder("location", geoPoint).withUnit("km")));
                displaySearchHitsInMapMaster(searchHits);

                syncSlave();
            }
        });
        mapViewMaster.addEventHandler(MapViewEvent.MAP_BOUNDING_EXTENT, event -> {
            syncSlave();
        });
    }

    private void syncSlave() {
        Coordinate center = mapViewMaster.getCenter();
        mapViewSlave.setCenter(center);
        double zoom = mapViewMaster.getZoom();
        mapViewSlave.setZoom(zoom);
        mapViewMaster.setZoom(zoom);
    }

    private void initMapViewSlave() {
        MapType mapType = MapType.OSM;
        if (configuration.getBingMapsApiKey() != null) {
            mapViewSlave.setBingMapsApiKey(configuration.getBingMapsApiKey());
            mapType = MapType.BINGMAPS_CANVAS_GRAY;
        }
        initMap(mapViewSlave, mapType, Configuration.builder().interactive(false).showZoomControls(false).build());

        JavaFxObservable.eventsOf(mapViewSlave, MapViewEvent.MAP_BOUNDING_EXTENT)
            .debounce(250, TimeUnit.MILLISECONDS)
            .observeOn(JavaFxScheduler.platform())
            .subscribe(event -> {
                event.consume();
                extentSlave = event.getExtent();
                updateMapSlave();
            });
    }

    private void initMap(MapView mapView, MapType mapType, Configuration mapConfiguration) {
        mapView.setAnimationDuration(100);
        mapView.setCustomMapviewCssURL(getClass().getResource("/dbstations.css"));
        mapView.initializedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                mapView.setZoom(ZOOM_DEFAULT);
                mapView.setCenter(coordKarlsruheCastle);
            }
        });
        mapView.setMapType(mapType);
        mapView.initialize(mapConfiguration);
    }

    private void updateMapSlave() {
        if (mapViewSlave.getInitialized()) {

            GeoBox geoBox = new GeoBox(new GeoPoint(extentSlave.getMax().getLatitude(), extentSlave.getMin().getLongitude()),
                new GeoPoint(extentSlave.getMin().getLatitude(), extentSlave.getMax().getLongitude()));

            SearchHits<FoodPOI> searchHits = null;
            try {
                searchHits = repository.searchByLocationNear(geoBox);
            } catch (Throwable t) {
                LOG.error("OOPS", t);
            }

            displaySearchHitsInMapSlave(searchHits);
        }
    }

    private void displaySearchHitsInMapSlave(SearchHits<FoodPOI> searchHits) {

        List<MapLabelSlave> newLabels = new ArrayList<>();

        if (searchHits != null) {

            LOG.debug("updating slave map");
            searchHits.forEach(searchHit -> {
                FoodPOI foodPOI = searchHit.getContent();
                MapLabelSlave mapLabel = new MapLabelSlave(foodPOI.getId().toString(), "&nbsp;");
                mapLabel.setPosition(new Coordinate(foodPOI.getLocation().getLat(), foodPOI.getLocation().getLon()));
//                mapLabel.setCssClass(foodPOI.getCategory().toString());

                newLabels.add(mapLabel);

                if (labelsSlave.contains(mapLabel)) {
                    labelsSlave.remove(mapLabel);
                } else {
                    mapViewSlave.addLabel(mapLabel);
                }
            });

            labelsSlave.forEach(mapViewSlave::removeLabel);
            labelsSlave.clear();
            labelsSlave.addAll(newLabels);

            labelsSlave.forEach(mapLabel -> mapLabel.setVisible(true));
            LOG.debug("finished updating slave map");
        }
    }

    private void displaySearchHitsInMapMaster(SearchHits<FoodPOI> searchHits) {
        labelsMaster.forEach(mapViewMaster::removeLabel);
        labelsMaster.clear();
        searchHits.forEach(searchHit -> {
            FoodPOI foodPOI = searchHit.getContent();
            MapLabel mapLabel = new MapLabel(String.format("%1$3.1f km - %2$s", searchHit.getSortValues().get(0), foodPOI.getName()));
            mapLabel.setPosition(new Coordinate(foodPOI.getLocation().getLat(), foodPOI.getLocation().getLon()));
//            mapLabel.setCssClass(foodPOI.getCategory());
            mapLabel.setVisible(true);
            mapViewMaster.addLabel(mapLabel);
            labelsMaster.add(mapLabel);
        });
    }
}
