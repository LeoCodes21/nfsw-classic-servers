package me.leorblx.classicnfsw.xmpp;

import me.leorblx.classicnfsw.core.XmlUtils;
import me.leorblx.classicnfsw.http.controller.Session;
import me.leorblx.classicnfsw.xmpp.jaxb.XMPP_MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

public class XmppSrv {
	public static ConcurrentHashMap<Long, XmppTalk> xmppClients = new ConcurrentHashMap<Long, XmppTalk>();

	public static void addXmppClient(Long personaId, XmppTalk xmppClient) {
        xmppClients.put(personaId, xmppClient);
	}

	public static void sendMsg(Long personaId, String msg) {
		if (xmppClients.containsKey(personaId)) {
			XmppTalk xTalk = xmppClients.get(personaId);
			if (xTalk != null) {
				XMPP_MessageType messageType = new XMPP_MessageType();
				messageType.setFrom("nfsw.engine.engine@" + Session.getXmppIp());
				messageType.setToPersonaId(personaId);
				messageType.setBody(msg);
				
				xTalk.write(XmlUtils.marshal(messageType));
			} else {
				System.err.println(
						"xmppClient with the personaId " + personaId + " is attached to a null XmppTalk instance!");
			}
		} else {
			System.err.println("xmppClients doesn't contain personaId " + personaId);
		}
	}

	public static void removeXmppClient(Long personaId) {
		xmppClients.remove(personaId);
	}

	public static void main(String[] args) throws Exception {
		new XmppSrv();
	}

	public XmppSrv() {
		new XmppSrvRun().start();
	}

	private static class XmppSrvRun extends Thread {
		public void run() {
			try {
				System.out.println("Xmpp server is running.");
				ServerSocket listener = new ServerSocket(5222);
				try {
					while (true) {
						new Capitalizer(listener.accept()).start();
					}
				} finally {
					listener.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static class Capitalizer extends Thread {
		private Socket socket;
		private XmppTalk xmppTalk;

		public Capitalizer(Socket socket) {
			this.socket = socket;
			xmppTalk = new XmppTalk(this.socket);
			System.out.println("New connection at " + socket);
		}

		public void run() {
			try {
				new XmppHandShake(xmppTalk);
				XmppHandler xmppHandler = new XmppHandler(xmppTalk);
				while (true) {
					String input = xmppHandler.read();
					if (input == null || input.contains("</stream:stream>")) {
						XmppChatLobbies.signOut(xmppTalk);
						break;
					}
				}
			} finally {
				try {
					socket.close();
				} catch (IOException e) {
					System.out.println("Couldn't close a socket, what's going on?");
				}
				XmppSrv.removeXmppClient(xmppTalk.getPersonaId());
				System.out.println("Connection with client closed");
			}
		}

	}

	public static XmppTalk get(Long personaId) {
		return xmppClients.get(personaId);
	}

}