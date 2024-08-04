package de.malkusch.telgrambot.api;

import de.malkusch.telgrambot.*;
import de.malkusch.telgrambot.Update.CallbackUpdate.CallbackId;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Function;

@RequiredArgsConstructor
public abstract class AbstractTelegramApiProxy implements TelegramApi {

    protected final TelegramApi api;

    protected <R> R delegate(Function<TelegramApi, R> call) {
        return call.apply(api);
    }

    private void delegateVoid(Consumer<TelegramApi> call) {
        delegate(api -> {
            call.accept(api);
            return null;
        });
    }

    @Override
    public void receiveUpdates(UpdateReceiver... receivers) {
        delegateVoid(api -> api.receiveUpdates(receivers));
    }

    @Override
    public void dropPendingUpdates() {
        delegateVoid(TelegramApi::dropPendingUpdates);
    }

    @Override
    public MessageId send(String message, Button... buttons) {
        return delegate(api -> api.send(message, buttons));
    }

    @Override
    public MessageId send(String message) {
        return delegate(api -> api.send(message));
    }

    @Override
    public void pin(MessageId message) {
        delegateVoid(api -> api.pin(message));
    }

    @Override
    public PinnedMessage pinned() {
        return delegate(TelegramApi::pinned);
    }

    @Override
    public void unpin(MessageId message) {
        delegateVoid(api -> api.unpin(message));
    }

    @Override
    public void unpin() {
        delegateVoid(TelegramApi::unpin);
    }

    @Override
    public void delete(MessageId message) {
        delegateVoid(api -> api.delete(message));
    }

    @Override
    public void delete(Collection<MessageId> messages) {
        delegateVoid(api -> api.delete(messages));
    }

    @Override
    public void disableButtons(MessageId message) {
        delegateVoid(api -> api.disableButtons(message));
    }

    @Override
    public void react(MessageId message, Reaction reaction) {
        delegateVoid(api -> api.react(message, reaction));
    }

    @Override
    public void answer(CallbackId id) {
        delegateVoid(api -> api.answer(id));
    }

    @Override
    public void answer(CallbackId id, String alert) {
        delegateVoid(api -> api.answer(id, alert));
    }

    @Override
    public void close() throws Exception {
        api.close();
    }
}
