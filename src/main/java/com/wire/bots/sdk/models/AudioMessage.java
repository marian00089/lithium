//
// Wire
// Copyright (C) 2016 Wire Swiss GmbH
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program. If not, see http://www.gnu.org/licenses/.
//

package com.wire.bots.sdk.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.waz.model.Messages;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioMessage extends MessageAssetBase {
    @JsonProperty
    private long duration;

    @JsonCreator
    public AudioMessage(@JsonProperty("messageId") UUID messageId,
                        @JsonProperty("conversationId") UUID convId,
                        @JsonProperty("clientId") String clientId,
                        @JsonProperty("userId") UUID userId) {
        super(messageId, convId, clientId, userId);
    }

    public AudioMessage(MessageAssetBase base, Messages.Asset.AudioMetaData audio) {
        super(base);
        setDuration(audio.getDurationInMillis());
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }
}
