package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.io.IOException;

class JsonParserUtils {
    private JsonParserUtils() {
    }

    /**
     * Consume json object with the given consumer by iterating over its entries
     *
     * @param jsonParser: jsonParser to consume. currentToken() must return START_OBJECT at the beginning.
     * @param consumer:   consumer taking two arguments: json key and the current jsonParser.
     *                    The consumer must only parse and finishes to parse the corresponding value.
     * @throws IOException
     */
    static void consumeJson(
            @Nonnull final JsonParser jsonParser,
            final PartialJsonConsumer consumer
    ) throws IOException {
        // The current position of jsonParser must be "{".
        Preconditions.checkState(jsonParser.currentToken() == JsonToken.START_OBJECT);

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            // The token right after "{" must be field name.
            Preconditions.checkState(jsonParser.currentToken() == JsonToken.FIELD_NAME);

            // Get the current field name.
            final String key = jsonParser.currentName();

            // Go to the next token, which is the beginning of the corresponding value.
            jsonParser.nextToken();

            // consumer must consume the corresponding value.
            consumer.accept(key, jsonParser);
        }
    }

    interface PartialJsonConsumer {
        void accept(String key, JsonParser jsonParser) throws IOException;
    }
}
