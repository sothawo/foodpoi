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

import com.sothawo.mapjfx.MapLabel;
import org.springframework.util.Assert;

/**
 * @author P.J. Meisch (pj.meisch@sothawo.com)
 */
public class MapLabelSlave extends MapLabel implements Comparable<MapLabelSlave> {

    private String id;

    public MapLabelSlave(String id, String text) {
        super(text);
        Assert.notNull(id, "id must not be null");
        this.id = id;
    }

    public MapLabelSlave(String id, String text, int offsetX, int offsetY) {
        super(text, offsetX, offsetY);
        Assert.notNull(id, "id must not be null");
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        MapLabelSlave that = (MapLabelSlave) o;

        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + id.hashCode();
        return result;
    }

    @Override
    public int compareTo(MapLabelSlave o) {
        if (o == null) return -1;
        return this.id.compareTo(o.getId());
    }
}
