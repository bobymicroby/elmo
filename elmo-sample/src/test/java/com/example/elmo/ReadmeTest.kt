package com.example.elmo

import dev.boby.elmo.*
import dev.boby.elmo.View
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import org.junit.Test
import java.lang.RuntimeException
import java.util.concurrent.CopyOnWriteArrayList


data class WalletModel(val cents: Long, val canRetry: Boolean, val error: String?)

data class BankError(val requestedAmount: Long, val errorMessage: String)

sealed class Msg {
    data class RequestMoney(val requestedAmount: Long) : Msg()
    data class BankResponse(val result: Result<BankError, Long>) : Msg()
    data class Error(val errorMessage: String) : Msg()
}

sealed class Cmd {
    data class RequestFromBankApi(val cents: Long) : Cmd()
}


interface CommandInterpreter<Message, Command> {
    fun call(cmd: Command): Observable<out Message>
}


class TestInterpreter(private val shouldThrow: Boolean) : CommandInterpreter<Msg, Cmd> {
    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            is Cmd.RequestFromBankApi -> Observable.fromCallable {

                if (shouldThrow) {
                    throw  RuntimeException("Bang")
                } else {
                    Msg.BankResponse(Ok(cmd.cents))
                }

            }
        }
    }

}

class TestView<Model>(override val viewScheduler: Scheduler) : View<Model> {
    val models = CopyOnWriteArrayList<Model>()
    override fun view(model: Model) {
        models.add(model)
    }
}


class Update(override val updateScheduler: Scheduler,
             private val interpreter: CommandInterpreter<Msg, Cmd>) : dev.boby.elmo.effect.Update<WalletModel, Msg, Cmd> {


    override fun update(msg: Msg, model: WalletModel): Return<WalletModel, Cmd> {
        return when (msg) {
            is Msg.BankResponse -> {
                when (msg.result) {
                    is Ok -> Pure(model.copy(canRetry = true, cents = model.cents + msg.result.value))
                    is Err -> if (model.canRetry) {
                        Effect(model.copy(canRetry = false), Cmd.RequestFromBankApi(msg.result.error.requestedAmount))
                    } else {
                        Pure(model.copy(error = msg.result.error.errorMessage))
                    }
                }
            }
            is Msg.RequestMoney -> {
                Effect(model, Cmd.RequestFromBankApi(msg.requestedAmount))
            }
            is Msg.Error -> {
                Pure(model.copy(error = msg.errorMessage))
            }
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return interpreter.call(cmd)
    }

    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        return Msg.Error("Unknown error occurred while processing $cmd")
    }

}


class Test {
    @Test
    fun sanity() {

        val initial = Pure(WalletModel(cents = 0, canRetry = true, error = null))

        val sameThreadScheduler = Schedulers.trampoline()

        val interpreter = TestInterpreter(shouldThrow = false)

        val testView = TestView<WalletModel>(sameThreadScheduler)

        val sandbox = Sandbox.create(initial,Update(sameThreadScheduler,interpreter),testView)

        sandbox.accept(Msg.RequestMoney(100))



    }
}

