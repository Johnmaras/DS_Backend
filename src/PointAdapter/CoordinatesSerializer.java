package PointAdapter;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class CoordinatesSerializer implements JsonSerializer{
    @Override
    public JsonElement serialize(Object o, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        JsonElement origin = context.serialize(((Coordinates)o).getOrigin());
        JsonElement destination = context.serialize(((Coordinates)o).getDestination());

        jsonObject.add("origin", origin);
        jsonObject.add("destination", destination);

        return jsonObject;

    }
}
