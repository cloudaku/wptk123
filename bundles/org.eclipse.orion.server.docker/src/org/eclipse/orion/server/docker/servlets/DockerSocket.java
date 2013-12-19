/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.orion.server.docker.servlets;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebSocket
public class DockerSocket {

	private CountDownLatch messageLatch;

	private String outMessage;

	private Session session;

	public String getOutMessage() {
		return this.outMessage;
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		this.session = session;
	}

	@OnWebSocketMessage
	public void onMessage(String msg) {
		Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.servlets.OrionServlet"); //$NON-NLS-1$
		if (logger.isDebugEnabled()) {
			// Create a JSONObject as a good way to print out the control characters as \r, \n, etc.
			JSONObject jsonObject = new JSONObject();
			String received = msg;
			try {
				jsonObject.put("message", msg);
				received = jsonObject.toString();
			} catch (JSONException e) {
				// do not do anything here, just use the message
			}
			logger.debug("Docker Socket received: " + received);
		}
		this.outMessage += msg;
		this.messageLatch.countDown();
	}

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.servlets.OrionServlet"); //$NON-NLS-1$
		if (logger.isDebugEnabled()) {
			logger.debug("Docker Socket closed" + (statusCode != StatusCode.NORMAL ? ": " + reason : ""));
		}
		session.close();
	}

	public void sendCmd(String cmd) {
		try {
			if (!session.isOpen()) {
				// session has been closed, just return
				return;
			}
			this.messageLatch = new CountDownLatch(1);
			this.outMessage = "";
			this.session.getRemote().sendString(cmd);
			Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.servlets.OrionServlet"); //$NON-NLS-1$
			if (logger.isDebugEnabled()) {
				// Create a JSONObject as a good way to print out the control characters as \r, \n, etc.
				JSONObject jsonObject = new JSONObject();
				String sent = cmd;
				try {
					jsonObject.put("message", cmd);
					sent = jsonObject.toString();
				} catch (JSONException e) {
					// do not do anything here, just use the cmd
				}
				logger.debug("Docker Socket sent: " + sent);
			}
		} catch (IOException e) {
			Logger logger = LoggerFactory.getLogger("org.eclipse.orion.server.servlets.OrionServlet"); //$NON-NLS-1$
			logger.error(e.getLocalizedMessage(), e);
			session.close();
		}
	}

	public boolean waitResponse(int amount, TimeUnit unit) throws InterruptedException {
		return this.messageLatch.await(amount, unit);
	}
}
