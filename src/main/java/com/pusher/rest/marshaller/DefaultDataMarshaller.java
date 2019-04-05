package com.pusher.rest.marshaller;

import com.google.gson.Gson;

public class DefaultDataMarshaller implements DataMarshaller {

   private final Gson gson;

   public DefaultDataMarshaller() {
      gson = new Gson();
   }

   public DefaultDataMarshaller(Gson customGsonInstance) {
      gson = customGsonInstance;
   }

   public String marshal(final Object data) {
      return gson.toJson(data);
   }
}
