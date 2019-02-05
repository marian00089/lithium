package com.wire.bots.sdk.user;

import com.wire.bots.sdk.ClientRepo;
import com.wire.bots.sdk.WireClient;
import com.wire.bots.sdk.crypto.Crypto;
import com.wire.bots.sdk.factories.CryptoFactory;
import com.wire.bots.sdk.factories.StorageFactory;
import com.wire.bots.sdk.state.State;
import com.wire.bots.sdk.tools.Logger;

import javax.ws.rs.client.Client;

public class UserClientRepo extends ClientRepo {
    public UserClientRepo(Client httpClient, CryptoFactory cryptoFactory, StorageFactory storageFactory) {
        super(httpClient, cryptoFactory, storageFactory);
    }

    public WireClient getWireClient(String botId, String conv) {
        String key = String.format("%s-%s", botId, conv);
        return clients.computeIfAbsent(key, k -> {
            try {
                Crypto crypto = cryptoFactory.create(botId);
                State storage = storageFactory.create(botId);
                return new UserClient(httpClient, crypto, storage, conv);
            } catch (Exception e) {
                e.printStackTrace();
                Logger.error("GetWireClient. BotId: %s, conv: %s, status: %s",
                        botId,
                        conv,
                        e.getMessage());
                return null;
            }
        });
    }
}
