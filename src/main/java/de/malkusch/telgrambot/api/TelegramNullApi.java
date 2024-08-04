package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.*;
import de.malkusch.telgrambot.Update.CallbackUpdate.CallbackId;

import java.util.Collection;

import static de.malkusch.telgrambot.PinnedMessage.NO_MESSAGE;

public final class TelegramNullApi implements TelegramApi {

    private static final MessageId NO_MESSAGE_ID = new MessageId(0);

    @Override
    public void receiveUpdates(UpdateReceiver... receivers) {
    }

    @Override
    public MessageId send(String message, Button... buttons) {
        return NO_MESSAGE_ID;
    }

    @Override
    public void pin(MessageId message) {
    }

    @Override
    public PinnedMessage pinned() {
        return NO_MESSAGE;
    }

    @Override
    public void unpin(MessageId message) {
    }

    @Override
    public void unpin() {
    }

    @Override
    public void delete(MessageId message) {
    }

    @Override
    public void delete(Collection<MessageId> messages) {
    }

    @Override
    public void disableButtons(MessageId message) {
    }

    @Override
    public void react(MessageId message, Reaction reaction) {
    }

    @Override
    public void answer(CallbackId id) {
    }

    @Override
    public void answer(CallbackId id, String alert) {
    }

    @Override
    public MessageId send(String message) {
        return NO_MESSAGE_ID;
    }

    @Override
    public void dropPendingUpdates() {
    }

    @Override
    public void close() throws Exception {
    }
}
