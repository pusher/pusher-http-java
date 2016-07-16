package com.pusher.rest;

import com.google.gson.Gson;

/**
 * Serialises the payload of a notification using Gson.
 */
class GsonPayloadSerialiser implements PayloadSerialiser {
	private final Gson gson;
	
	GsonPayloadSerialiser(Gson gson) {
		this.gson = gson;
	}

	/**
     * @param data an unserialised event payload
     * @return a serialised event payload
     */
	@Override
	public String serialise(Object o) {
		return gson.toJson(o);
	}
}
