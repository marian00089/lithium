package com.wire.bots.sdk.user;

import com.wire.bots.cryptobox.CryptoException;
import com.wire.bots.sdk.MessageHandlerBase;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.server.model.NewBot;
import com.wire.bots.sdk.server.model.Payload;
import com.wire.bots.sdk.server.resources.MessageResourceBase;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.util.UUID;

public class UserMessageResource extends MessageResourceBase {
    private UUID userId;
    private StorageFactory storageFactory;
    private CryptoFactory cryptoFactory;
    private Client client;
    private Crypto crypto;
    private State state;

    public UserMessageResource(MessageHandlerBase handler) {
        super(handler, null);
    }

    void onNewMessage(UUID eventId, UUID convId, Payload payload) throws Exception {
        if (convId == null) {
            Logger.warning("onNewMessage: %s convId is null", payload.type);
            return;
        }

        try {
            WireClient client = getWireClient(convId);

            handleMessage(eventId, payload, client);
        } catch (CryptoException e) {
            Logger.error("onNewMessage: msg: %s, conv: %s, %s", eventId, convId, e);
        }
    }

    void onUpdate(UUID id, Payload payload) throws CryptoException, IOException {
        Crypto crypto = getCrypto();
        NewBot newBot = getStorage().getState();
        String token = newBot.token;
        String clientId = newBot.client;
        API api = new API(client, null, token);
        UserClient userClient = new UserClient(userId, clientId, null, crypto, api);

        handleUpdate(id, payload, userClient);
    }

    private WireClient getWireClient(UUID convId) throws CryptoException, IOException {
        Crypto crypto = getCrypto();
        NewBot newBot = getStorage().getState();
        String token = newBot.token;
        String clientId = newBot.client;
        API api = new API(client, convId, token);
        return new UserClient(userId, clientId, convId, crypto, api);
    }

    private State getStorage() throws IOException {
        if (state == null)
            state = storageFactory.create(userId);
        return state;
    }

    private Crypto getCrypto() throws CryptoException {
        if (crypto == null)
            crypto = cryptoFactory.create(userId);
        return crypto;
    }

    UserMessageResource addUserId(UUID userId) {
        this.userId = userId;
        return this;
    }

    UserMessageResource addStorageFactory(StorageFactory storageFactory) {
        this.storageFactory = storageFactory;
        return this;
    }

    UserMessageResource addCryptoFactory(CryptoFactory cryptoFactory) {
        this.cryptoFactory = cryptoFactory;
        return this;
    }

    UserMessageResource addClient(Client client) {
        this.client = client;
        return this;
    }
}
