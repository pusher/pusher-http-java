package com.pusher.rest;

/**
 * Serialises the payload of a notification.
 */
public interface PayloadSerialiser {
	String serialise(Object o);
}
