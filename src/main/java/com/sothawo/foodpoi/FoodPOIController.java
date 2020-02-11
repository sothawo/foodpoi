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

import io.reactivex.Observable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.elasticsearch.core.geo.GeoPoint;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@RestController
@RequestMapping("/foodpoi")
public class FoodPOIController {

    private static final Logger LOG = LoggerFactory.getLogger(FoodPOIController.class);

    private final FoodPOIRepository repository;

    public FoodPOIController(FoodPOIRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/load")
    public void load() throws IOException, URISyntaxException {

        AtomicLong count = new AtomicLong();
        Stream<String> lines = Files.lines(Paths.get("europe-latest-food.csv"));
        Observable.fromIterable(lines::iterator)
            .skip(1)
            .filter(StringUtils::hasLength)
            .map(this::getFoodPOI)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .window(1000)
            .map(foodPOIObservable -> foodPOIObservable.toList().subscribe(foodPOIS -> {
                count.addAndGet(foodPOIS.size());
                LOG.info("saving {} POIs now {}", foodPOIS.size(), count.get());
                repository.saveAll(foodPOIS);
            }))
            .blockingSubscribe();
    }

    private Optional<FoodPOI> getFoodPOI(String line) {
        try {
            final String[] fields = line.split("\\|");
            if (fields.length != 5) {
                throw new IllegalArgumentException("no 5 fields in line");
            }
            Integer category = Integer.valueOf(fields[0]);
            String id = fields[1];
            double lat = Double.parseDouble(fields[2]);
            double lon = Double.parseDouble(fields[3]);
            String name = fields[4];
            GeoPoint location = new GeoPoint(lat, lon);
            return Optional.of(new FoodPOI(id, category, name, location));
        } catch (Exception e) {
            LOG.error("error in line: \"{}\", {}", line, e.getMessage());
            return Optional.empty();
        }
    }
}
