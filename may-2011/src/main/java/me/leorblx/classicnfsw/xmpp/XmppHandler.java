package me.leorblx.classicnfsw.xmpp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmppHandler {

	private XmppTalk xmppTalk;

	public XmppHandler(XmppTalk xmppTalk) {
        this.xmppTalk = xmppTalk;
	}

	public String read() {
		String read = xmppTalk.read();
		if (read != null && !read.isEmpty()) {
			if (read.contains("presence to")) {
				Pattern regPattern = Pattern
						.compile("<presence to='channel.(\\w+)__(\\d+)@conference.(.*)/nfsw.(\\d+)'/>");
				Matcher match = regPattern.matcher(read);
				if (match.find()) {
					String channelName = match.group(1);
					Integer channelNumber = Integer.valueOf(match.group(2));
					Long personaId = xmppTalk.getPersonaId();

					if (!channelName.equals(xmppTalk.getCurrentChannelName())
							|| !channelNumber.equals(xmppTalk.getCurrentChannelNumber())) {
						XmppChatLobbies
								.getFreeroamLobby(xmppTalk.getCurrentChannelName(), xmppTalk.getCurrentChannelNumber())
								.removeXmppTalk(personaId);
					}

					xmppTalk.setCurrentChannelName(channelName);
					xmppTalk.setCurrentChannelNumber(channelNumber);

					XmppChat.getPresenceResponse(xmppTalk);
				}
			} else if (read.contains("Chat_All")) {
				Pattern regPattern = Pattern.compile("message to='channel.(\\w+)__(\\d+)@");
				Matcher match = regPattern.matcher(read);
				if (match.find()) {
					String channelName = match.group(1);
					Integer channelNumber = Integer.valueOf(match.group(2));
					XmppFreeroamLobby freeroamLobby = XmppChatLobbies.getFreeroamLobby(channelName, channelNumber);
					XmppChat xmppChat = new XmppChat(xmppTalk.getPersonaId(), read);
					freeroamLobby.broadcast(xmppChat);
				}
			} else if (read.contains("Chat_Whisper")) {
				Long personaIdTo = parsePersonaId(read);
				if (personaIdTo != 0) {
					Long personaIdFrom = xmppTalk.getPersonaId();
					XmppChat xmppChat = new XmppChat(personaIdFrom, read);
					XmppTalk xmppTalkTo = XmppSrv.get(personaIdTo);
					if (xmppTalkTo != null) {
						xmppTalkTo.write(xmppChat.getWhisperMessage(personaIdTo));
					} else {
						System.err.println("persona: " + personaIdTo + " not online in xmpp server");
					}
				}
			}
		}
		return read;
	}

	private Long parsePersonaId(String read) {
		Pattern regPattern = Pattern.compile("message to='nfsw.(\\d+)@(.*)");
		Matcher match = regPattern.matcher(read);
		if (match.find()) {
			return Long.valueOf(match.group(1));
		}
		return 0L;
	}

}