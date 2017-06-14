import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;

public class PolylineAdapterDeserializer implements JsonDeserializer{

    @Override
    public PolylineAdapter deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonContext) throws JsonParseException {
        JsonArray array = jsonElement.getAsJsonArray();
        JsonObject object = array.get(0).getAsJsonObject();

        /*LatLngAdapter origin = jsonContext.deserialize(object.get("origin"), LatLngAdapter.class);
        LatLngAdapter destination = jsonContext.deserialize(object.get("destination"), LatLngAdapter.class);*/

        LatLngAdapter[] points = jsonContext.deserialize(object.get("points"), LatLngAdapter[].class);

        PolylineAdapter pl = new PolylineAdapter();
        pl.addAllPoint(Arrays.asList(points));

        return pl;
    }
}
