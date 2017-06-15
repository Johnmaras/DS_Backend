package PointAdapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class LatLngAdapterSerializer implements JsonSerializer{
    @Override
    public JsonElement serialize(Object o, Type type, JsonSerializationContext jsonSerializationContext) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("lat", ((LatLngAdapter)o).getLatitude());
        jsonObject.addProperty("lng", ((LatLngAdapter)o).getLongitude());

        return jsonObject;
    }
}
