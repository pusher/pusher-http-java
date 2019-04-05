package com.pusher.rest.marshaller;

import com.google.gson.Gson;

public class DefaultDataMarshaller implements DataMarshaller {

   private final Gson gson = new Gson();

   public String marshal(final Object data) {
      return gson.toJson(data);
   }
}
