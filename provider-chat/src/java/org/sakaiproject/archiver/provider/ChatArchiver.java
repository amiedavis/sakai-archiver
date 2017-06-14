package org.sakaiproject.archiver.provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.sakaiproject.archiver.api.ArchiverRegistry;
import org.sakaiproject.archiver.api.ArchiverService;
import org.sakaiproject.archiver.spi.Archiveable;
import org.sakaiproject.archiver.util.Jsonifier;
import org.sakaiproject.chat2.model.ChatChannel;
import org.sakaiproject.chat2.model.ChatManager;
import org.sakaiproject.chat2.model.ChatMessage;
import org.sakaiproject.exception.PermissionException;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
import org.sakaiproject.user.api.UserNotDefinedException;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of {@link Archiveable} for the chat tool
 */
@Slf4j
public class ChatArchiver implements Archiveable {

	private static final String CHAT_TOOL = "sakai.chat";

	public void init() {
		ArchiverRegistry.getInstance().register(CHAT_TOOL, this);
	}

	public void destroy() {
		ArchiverRegistry.getInstance().unregister(CHAT_TOOL);
	}

	@Setter
	private ChatManager chatManager;

	@Setter
	private ArchiverService archiverService;

	@Setter
	private UserDirectoryService userDirectoryService;

	@Override
	public void archive(final String archiveId, final String siteId, final String toolId, final boolean includeStudentContent) {

		final List<ChatChannel> chatChannels = this.chatManager.getContextChannels(siteId, true);

		for (final ChatChannel chatChannel : chatChannels) {

			// Save the metadata for this channel
			final ChatChannelMetadata metadata = createChatChannelMetadata(chatChannel);

			this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(metadata).getBytes(),
					chatChannel.getTitle() + " (metadata).json");

			// Only archive chat messages if we want to include student content
			if (includeStudentContent) {
				final int numMessages = this.chatManager.getChannelMessagesCount(chatChannel, null, null);

				// Go through and get chat messages, 99 at a time (i.e. 0-99, 100-199, etc)
				for (int start = 0; start <= numMessages - (numMessages % 100); start += 100) {

					try {
						final List<ChatMessage> chatMessages = this.chatManager.getChannelMessages(chatChannel, null, null, start, 99,
								true);

						final List<SimpleChatMessage> messagesToSave = createArchiveItems(chatMessages);

						// Convert to JSON and save to file
						final int rangeStart = start + 1;
						final int rangeEnd = numMessages - start >= 100 ? start + 100 : numMessages;
						this.archiverService.archiveContent(archiveId, siteId, toolId, Jsonifier.toJson(messagesToSave).getBytes(),
								chatChannel.getTitle() + "(" + rangeStart + "-" + rangeEnd + ").json",
								chatChannel.getTitle() + "(messages)");

					} catch (final PermissionException e) {
						log.error("Could not retrieve some chat messages for channel: " + chatChannel.getTitle());
						continue;
					}
				}
			}
		}
	}

	private ChatChannelMetadata createChatChannelMetadata(final ChatChannel chatChannel) {

		final ChatChannelMetadata metadata = new ChatChannelMetadata();
		metadata.setDateCreated(chatChannel.getCreationDate());
		metadata.setStartDate(chatChannel.getStartDate());
		metadata.setEndDate(chatChannel.getEndDate());
		metadata.setTitle(chatChannel.getTitle());
		metadata.setDescription(chatChannel.getDescription());
		metadata.setId(chatChannel.getId());

		return metadata;
	}

	/**
	 * Build the list of messages to be archived for this channel
	 *
	 * @param chatMessages
	 * @return the list of messages to be saved
	 */
	private List<SimpleChatMessage> createArchiveItems(final List<ChatMessage> chatMessages) {
		final List<SimpleChatMessage> messagesToSave = new ArrayList<SimpleChatMessage>();
		for (final ChatMessage message : chatMessages) {
			final SimpleChatMessage simpleChatMessage = createArchiveItem(message);
			messagesToSave.add(simpleChatMessage);
		}

		return messagesToSave;
	}

	/**
	 * Build the archive item for an individual chat message
	 *
	 * @param message
	 * @return
	 */

	private SimpleChatMessage createArchiveItem(final ChatMessage message) {

		final SimpleChatMessage simpleChatMessage = new SimpleChatMessage();
		simpleChatMessage.setBody(message.getBody());
		simpleChatMessage.setDate(message.getMessageDate());

		final User user = getUser(message.getOwner());
		if (user != null) {
			simpleChatMessage.setOwner(user.getDisplayName());
			simpleChatMessage.setEid(user.getEid());
		} else {
			simpleChatMessage.setOwner(message.getOwner());
			simpleChatMessage.setEid(message.getOwner());
		}

		return simpleChatMessage;
	}

	/**
	 * Helper to get the user associated with a chat message
	 *
	 * @param owner
	 * @return user
	 */
	private User getUser(final String owner) {

		User user;

		try {
			user = this.userDirectoryService.getUser(owner);
			return user;

		} catch (final UserNotDefinedException e) {
			log.error("Could not find user with userId: " + owner + ", uuid will be used in place of display name and eid.");
		}
		return null;
	}

	/**
	 * Simplified helper class to represent an individual chat message
	 */
	private class SimpleChatMessage {

		@Getter
		@Setter
		private String body;

		@Getter
		@Setter
		private Date date;

		@Getter
		@Setter
		private String owner;

		@Getter
		@Setter
		private String eid;

	}

	/**
	 * Simplified helper class to represent the metadata for a chat channel
	 */
	private class ChatChannelMetadata {

		@Getter
		@Setter
		private String title;

		@Getter
		@Setter
		private String description;

		@Getter
		@Setter
		private String id;

		@Getter
		@Setter
		private Date dateCreated;

		@Getter
		@Setter
		private Date startDate;

		@Getter
		@Setter
		private Date endDate;
	}
}
