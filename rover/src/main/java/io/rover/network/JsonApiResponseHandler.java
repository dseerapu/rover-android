package io.rover.network;

import android.util.JsonReader;
import android.util.JsonToken;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by Rover Labs Inc on 2016-03-31.
 */
public class JsonApiResponseHandler extends JsonResponseHandler {

    public interface JsonApiObjectMapper {
        Object getObject(String type, String identifier, JSONObject attributes);
    }

    public interface JsonApiCompletionHandler {
        void onHandleCompletion(Object response, List includedObject);
    }

    private JsonApiObjectMapper mMapper;
    private JsonApiCompletionHandler mCompletionHandler;

    public JsonApiResponseHandler(JsonApiObjectMapper mapper) {
        mMapper = mapper;
    }

    public JsonApiCompletionHandler getCompletionHandler() { return mCompletionHandler; }
    public void setCompletionHandler(JsonApiCompletionHandler handler) {
        mCompletionHandler = handler;
    }

    @Override
    public void onHandleResponse(HttpResponse httpResponse) throws IOException {

        if (httpResponse.getBody() == null) {
            handleCompletion(null, Collections.emptyList());
            return;
        }

        JsonReader jsonReader = new JsonReader(httpResponse.getBody());

        Object response = null;
        ArrayList<Object> includedObjects = new ArrayList<>();

        jsonReader.beginObject();
        while (jsonReader.hasNext()) {

            String name = jsonReader.nextName();

            if (name.equals("data")) {

                JsonToken token = jsonReader.peek();

                switch (token) {
                    case BEGIN_OBJECT:
                        response = readObject(jsonReader);
                        break;
                    case BEGIN_ARRAY:
                        response = readArray(jsonReader);
                        break;
                    default:
                        break;
                }

            } else if (name.equals("included")) {

                jsonReader.beginArray();
                while (jsonReader.hasNext()) {

                    Object obj = readObject(jsonReader);

                    if (obj != null) {
                        includedObjects.add(obj);
                    }
                }
                jsonReader.endArray();

            } else {
                jsonReader.skipValue();
            }
        }

        jsonReader.endObject();

        handleCompletion(response, includedObjects);
    }

    private void handleCompletion(Object response, List includedObjects) {
        if (mCompletionHandler != null) {
            mCompletionHandler.onHandleCompletion(response, includedObjects);
        }
    }

    private Object readObject(JsonReader reader) throws IOException {
        String type = null;
        String id = null;
        JSONObject attributes = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String name = reader.nextName();

            if (name.equals("id")) {
                id = reader.nextString();
            } else if (name.equals("type")) {
                type = reader.nextString();
            } else if (name.equals("attributes")) {
                attributes = readJSONObject(reader);
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();

        return mMapper.getObject(type, id, attributes);
    }

    private ArrayList readArray(JsonReader reader) throws IOException {
        ArrayList arrayList = new ArrayList();

        reader.beginArray();
        while (reader.hasNext()) {
            Object object = readObject(reader);

            if (object != null) {
                arrayList.add(object);
            }
        }
        reader.endArray();

        return arrayList;
    }

}