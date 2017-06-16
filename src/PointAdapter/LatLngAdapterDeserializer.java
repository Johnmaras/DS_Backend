package PointAdapter;

import com.google.gson.*;

import java.lang.reflect.Type;

public class LatLngAdapterDeserializer implements JsonDeserializer{

    @Override
    public LatLngAdapter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();

        double lat = object.get("lat").getAsDouble();
        double lng = object.get("lng").getAsDouble();

        return new LatLngAdapter(lat, lng);

    }
}
