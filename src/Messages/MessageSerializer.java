package Messages;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

public class MessageSerializer implements JsonSerializer{
    @Override
    public JsonElement serialize(Object o, Type type, JsonSerializationContext context) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("requestType", ((Message)o).getRequestType());

        JsonElement query = context.serialize(((Message)o).getQuery());
        JsonElement results = context.serialize(((Message)o).getResults());

        jsonObject.add("query", query);
        jsonObject.add("results", results);

        return jsonObject;
    }
}
