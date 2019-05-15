package com.example.elmo


import dev.boby.elmo.*
import io.reactivex.Observable


import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

import io.reactivex.schedulers.Schedulers
import java.lang.RuntimeException


data class WalletModel(val userName: String, val cents: Long)

sealed class Msg {
    data class Receive(val cents: Long) : Msg()
    data class Spend(val cents: Long) : Msg()
    data class BankResponse(val result: Result<String, Long>) : Msg()
}

sealed class Cmd {
    data class RequestFromMom(val cents: Long) : Cmd()
    data class RequestFromBankApi(val cents: Long) : Cmd()
}

interface Update<Model, Msg> : dev.boby.elmo.pure.Update<Model, Msg> {
    override val updateScheduler: Scheduler
        get() = Schedulers.io()
}

interface View<Model> : dev.boby.elmo.View<Model> {
    override val viewScheduler: Scheduler
        get() = AndroidSchedulers.mainThread()
}

class WalletUpdate : Update<WalletModel, Msg> {

    override fun update(msg: Msg, model: WalletModel): WalletModel {
        return when (msg) {
            is Msg.Receive -> model.copy(cents = model.cents + msg.cents)
            is Msg.Spend -> model.copy(cents = model.cents - msg.cents)
            is Msg.BankResponse -> {
                when (msg.result) {
                    is Ok -> model.copy(cents = model.cents + msg.result.value)
                    is Err -> model
                }
            }
        }
    }
}

class WalletActivity : View<WalletModel> {

    override fun view(model: WalletModel) {

    }

}


class WalletCommandUpdate : dev.boby.elmo.effect.Update<WalletModel, Msg, Cmd> {
    override val updateScheduler: Scheduler get() = Schedulers.io()

    override fun update(msg: Msg, model: WalletModel): Return<WalletModel, Cmd> {
        return when (msg) {
            is Msg.Receive -> Pure(model.copy(cents = model.cents + msg.cents))
            is Msg.Spend -> {

                val newModel = model.copy(cents = model.cents - msg.cents)

                if (newModel.cents < 0) {
                    Effect(newModel, Cmd.RequestFromMom(0 - newModel.cents))
                } else {
                    Pure(newModel)
                }
            }
            is Msg.BankResponse -> {
                when (msg.result) {
                    is Ok -> Pure(model.copy(cents = model.cents + msg.result.value))
                    is Err -> Pure(model)
                }
            }
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            is Cmd.RequestFromMom -> Observable.fromCallable {
                Msg.Receive(cmd.cents) //this can be a async http call or database read or any other side effect
            }
            is Cmd.RequestFromBankApi -> Observable.fromCallable {
                if (cmd.cents > 100) {
                    throw RuntimeException("We are sorry!")
                }
                Msg.BankResponse(Ok(cmd.cents))
            }.onErrorReturn { t -> Msg.BankResponse(Err("Snap! something went wrong: ${t.message}")) }
        }
    }

    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        System.err.println("Error while processing $cmd: $t")
        return Msg.Receive(0)
    }


}