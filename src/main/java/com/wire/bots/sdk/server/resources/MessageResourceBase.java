package com.wire.bots.sdk.server.resources;

import com.google.protobuf.InvalidProtocolBufferException;
import com.waz.model.Messages;
import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.models.otr.PreKey;
import com.wire.bots.sdk.server.GenericMessageProcessor;
import com.wire.bots.sdk.server.model.InboundMessage;
import com.wire.bots.sdk.tools.Logger;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.UUID;

public abstract class MessageResourceBase {

    final ClientRepo repo;
    private final MessageHandlerBase handler;

    public MessageResourceBase(MessageHandlerBase handler, ClientRepo repo) {
        this.handler = handler;
        this.repo = repo;
    }

    protected void handleMessage(InboundMessage inbound, WireClient client) throws Exception {
        String botId = client.getId();
        InboundMessage.Data data = inbound.data;
        switch (inbound.type) {
            case "conversation.otr-message-add": {
                Logger.debug("conversation.otr-message-add: bot: %s from: %s:%s", botId, inbound.from, data.sender);

                GenericMessageProcessor processor = new GenericMessageProcessor(client, handler);

                Messages.GenericMessage message = decrypt(client, inbound);

                handler.onEvent(client, inbound.from, message);

                boolean process = processor.process(inbound.from, data.sender, message);
                if (process)
                    processor.cleanUp(message.getMessageId());
            }
            break;
            case "conversation.member-join": {
                Logger.debug("conversation.member-join: bot: %s", botId);

                // Check if this bot got added to the conversation
                if (data.userIds.remove(botId)) {
                    handler.onNewConversation(client);
                }

                int minAvailable = 8 * data.userIds.size();
                if (minAvailable > 0) {
                    ArrayList<Integer> availablePrekeys = client.getAvailablePrekeys();
                    availablePrekeys.remove(new Integer(65535));  //remove last prekey
                    if (availablePrekeys.size() < minAvailable) {
                        Integer lastKeyOffset = Collections.max(availablePrekeys);
                        ArrayList<PreKey> keys = client.newPreKeys(lastKeyOffset + 1, minAvailable);
                        client.uploadPreKeys(keys);
                        Logger.info("Uploaded " + keys.size() + " prekeys");
                    }
                    handler.onMemberJoin(client, data.userIds);
                }

                // Send dummy message just initialize the session for the new member
                client.sendReaction(UUID.randomUUID().toString(), "");
            }
            break;
            case "conversation.member-leave": {
                Logger.debug("conversation.member-leave: bot: %s", botId);

                // Check if this bot got removed from the conversation
                if (data.userIds.remove(botId)) {
                    repo.removeClient(botId);
                    handler.onBotRemoved(botId);
                    repo.purgeBot(botId);
                }

                if (!data.userIds.isEmpty()) {
                    handler.onMemberLeave(client, data.userIds);
                }
            }
            break;
            case "conversation.delete": {
                Logger.debug("conversation.delete: bot: %s", botId);

                // Cleanup
                repo.removeClient(botId);
                handler.onBotRemoved(botId);
                repo.purgeBot(botId);
            }
            break;
            case "conversation.create": {
                client.sendReaction(UUID.randomUUID().toString(), ""); //todo hack
                handler.onNewConversation(client);
            }
            break;
            case "conversation.rename": {
                handler.onConversationRename(client);
            }
            break;
            // Legacy code starts here
            case "user.connection": {
                if (inbound.connection.status.equals("pending")) {
                    client.acceptConnection(inbound.connection.to);
                }
            }
            break;
            // Legacy code ends here
            default:
                Logger.warning("Unknown event: %s, bot: %s", inbound.type, client.getId());
                break;
        }
    }

    private Messages.GenericMessage decrypt(WireClient client, InboundMessage inbound)
            throws CryptoException, InvalidProtocolBufferException {
        String userId = inbound.from;
        String clientId = inbound.data.sender;
        String cypher = inbound.data.text;

        String encoded = client.decrypt(userId, clientId, cypher);
        byte[] decoded = Base64.getDecoder().decode(encoded);
        return Messages.GenericMessage.parseFrom(decoded);
    }
}
