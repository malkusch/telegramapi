package de.malkusch.telgrambot.api;

import com.pengrad.telegrambot.ExceptionHandler;
import com.pengrad.telegrambot.UpdatesListener;
import de.malkusch.telgrambot.TelegramApi;
import de.malkusch.telgrambot.UpdateReceiver;

import static de.malkusch.telgrambot.api.InternalTelegramApi.Decorator.identity;


interface InternalTelegramApi extends TelegramApi {

    default void receiveUpdates(UpdateReceiver... receivers) {
        receiveUpdates(identity(), identity(), receivers);
    }

    @FunctionalInterface
    interface Decorator<T> {

        T decorate(T subject);

        static <T> Decorator<T> identity() {
            return it -> it;
        }

        default Decorator<T> then(Decorator<T> decorator) {
            return it -> decorator.decorate(decorate(it));
        }
    }

    void receiveUpdates(Decorator<UpdatesListener> listenerDecorator, Decorator<ExceptionHandler> errorDecorator, UpdateReceiver... receivers);
}
