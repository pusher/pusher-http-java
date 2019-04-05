package com.pusher.rest.marshaller;

import com.google.gson.Gson;

/**
 * @author <a href="mailto:marvenec@gmail.com">Martin Večeřa</a>
 */
public class DefaultDataMarshaller implements DataMarshaller {

   private final Gson gson = new Gson();

   public String marshal(final Object data) {
      return gson.toJson(data);
   }
}
