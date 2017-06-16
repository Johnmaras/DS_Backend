package Messages;

import PointAdapter.Coordinates;
import PointAdapter.PolylineAdapter;
import com.google.gson.*;

import java.lang.reflect.Type;
import java.util.Arrays;

public class MessageDeserializer implements JsonDeserializer{
    @Override
    public Message deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        JsonObject object = jsonElement.getAsJsonObject();

        int requestType = object.get("requestType").getAsInt();
        Coordinates query = context.deserialize(object.get("query"), Coordinates.class);
        PolylineAdapter[] results = context.deserialize(object.get("results"), PolylineAdapter[].class);

        Message message = new Message(requestType, query);
        message.setResults(Arrays.asList(results));

        return message;
    }
}
