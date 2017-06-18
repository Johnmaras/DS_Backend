package PointAdapter;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Arrays;

public class PolylineAdapterDeserializer implements JsonDeserializer{

    @Override
    public PolylineAdapter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonContext) throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();

        LatLngAdapter[] points = jsonContext.deserialize(object.get("points"), LatLngAdapter[].class);

        PolylineAdapter pl = new PolylineAdapter();
        pl.addAllPoint(Arrays.asList(points));

        return pl;
    }
}
