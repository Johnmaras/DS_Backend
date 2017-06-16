package PointAdapter;

import com.google.gson.*;

import java.lang.reflect.Type;

public class CoordinatesDeserializer implements JsonDeserializer{
    @Override
    public Coordinates deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();

        LatLngAdapter origin = context.deserialize(object.get("origin"), LatLngAdapter.class);
        LatLngAdapter destination = context.deserialize(object.get("destination"), LatLngAdapter.class);

        return new Coordinates(origin, destination);

    }
}
