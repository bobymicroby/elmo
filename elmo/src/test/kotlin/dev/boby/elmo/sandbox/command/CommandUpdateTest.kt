package dev.boby.elmo.sandbox.command

import dev.boby.elmo.*
import dev.boby.elmo.testutil.TestView


import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec
import io.reactivex.Observable
import io.reactivex.Observable.*
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import io.reactivex.schedulers.TestScheduler
import java.lang.RuntimeException
import java.util.concurrent.*
import java.util.concurrent.locks.LockSupport


data class State(val counter: Int)


sealed class Msg {
    object Noop : Msg()
    object RequestIncrement : Msg()
    object Decrement : Msg()
    object Incremented : Msg()
    data class Error(val cmd: Cmd, val t: Throwable) : Msg()
}

sealed class Cmd {

    object Increment : Cmd()
    data class TriggerError(val t: Throwable) : Cmd()
}


class DelayingCommandsUpdate(private val delayNs: Long, override val updateScheduler: Scheduler) : CommandUpdate<State, Msg, Cmd> {

    val commands = CopyOnWriteArrayList<Cmd>()
    val messages = CopyOnWriteArrayList<Msg>()

    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        return Msg.Error(cmd, t)
    }


    override fun update(msg: Msg, model: State): Computation<State, Cmd> {
        messages += msg
        return when (msg) {
            Msg.Noop -> Pure(model)
            Msg.RequestIncrement -> Effect(model, Cmd.Increment)
            Msg.Incremented -> Pure(model.copy(counter = model.counter + 1))
            Msg.Decrement ->  Pure(model.copy(counter = model.counter - 1))
            is Msg.Error ->  Pure(model.copy(counter = Integer.MIN_VALUE))
        }


    }

    override fun call(cmd: Cmd): Observable<out Msg> {

        commands += cmd
        return when (cmd) {
            Cmd.Increment -> just(Msg.Incremented).delay(
                    delayNs,
                    TimeUnit.NANOSECONDS,
                    updateScheduler
            )
            is Cmd.TriggerError -> error(cmd.t)
        }
    }


}


class CommandUpdateTest : StringSpec() {
    private val msgGen = Gen.list(Gen.from(listOf(Msg.Noop, Msg.RequestIncrement, Msg.Decrement)))

    init {
        "for all randomly generated messages feeded concurrently the counter should hold" {

            forAll(msgGen) { messages: List<Msg> ->

                val view = TestView<State>(Schedulers.io())
                val update = DelayingCommandsUpdate(1000, Schedulers.io())
                val seed = Effect(State(0), Cmd.Increment)
                val sandbox = Sandbox.create(seed, update, view)


                for (op in messages) {
                    Thread {
                        sandbox.accept(op)
                    }.start()
                }

                val expectedViewUpdates = messages.size +
                        messages.filter { m -> m == Msg.RequestIncrement }.count() + 2
                while (view.models.size != expectedViewUpdates) {
                    LockSupport.parkNanos(100)
                }

                sandbox.dispose()

                if (view.models.isEmpty() && messages.isEmpty()) {
                    true
                } else {

                    val counter = view.models.last().counter
                    val expectedCounter = expectedCounter(messages) + 1

                    expectedCounter == counter
                }
            }
        }
        "Testing time dependant code must work" {

            val commandExecutionDelay = 1000L
            val scheduler = TestScheduler()
            val view = TestView<State>(scheduler)
            val update = DelayingCommandsUpdate(commandExecutionDelay, scheduler)
            val seed = Effect(State(0), Cmd.Increment)
            val sandbox = Sandbox.create(seed, update, view)

            scheduler.triggerActions()
            view.models shouldBe listOf(State(0))

            scheduler.advanceTimeBy(commandExecutionDelay - 1, TimeUnit.NANOSECONDS)
            view.models shouldBe listOf(State(0))

            scheduler.advanceTimeBy(1, TimeUnit.NANOSECONDS)
            view.models shouldBe listOf(State(0), State(1))

            sandbox.accept(Msg.Decrement)
            view.models shouldBe listOf(State(0), State(1))

            scheduler.triggerActions()
            view.models shouldBe listOf(State(0), State(1), State(0))

            sandbox.dispose()
        }

        "Synchronous testing on the same thread must work" {

            val sameThreadScheduler = Schedulers.trampoline()
            val view = TestView<State>(sameThreadScheduler)
            val update = DelayingCommandsUpdate(1000, sameThreadScheduler)
            val seed = Effect(State(0), Cmd.Increment)

            val sandbox = Sandbox.create(seed, update, view)

            view.models shouldBe listOf(State(0), State(1))

            sandbox.accept(Msg.Decrement)
            view.models shouldBe listOf(State(0), State(1), State(0))

            sandbox.accept(Msg.Noop)
            view.models shouldBe listOf(State(0), State(1), State(0), State(0))
            sandbox.dispose()
        }


        "Unhandled command errors must be transformed to messages" {


            val view = TestView<State>(Schedulers.trampoline())
            val update = DelayingCommandsUpdate(0, Schedulers.trampoline())
            val error = RuntimeException("Snap!")
            val cmdErr = Cmd.TriggerError(error)
            val seed = Effect(State(0), cmdErr)
            val sandbox = Sandbox.create(seed, update, view)

            update.commands shouldBe listOf(cmdErr)
            update.messages shouldBe listOf(Msg.Error(cmdErr, error))
            view.models shouldBe listOf(State(0), State(Int.MIN_VALUE))
            sandbox.dispose()

        }


    }

    private fun expectedCounter(ops: List<Msg>): Int {
        return ops.foldRight(0) { msg, acc ->
            when (msg) {
                Msg.RequestIncrement -> acc + 1
                Msg.Decrement -> acc - 1
                else -> acc
            }
        }
    }
}


