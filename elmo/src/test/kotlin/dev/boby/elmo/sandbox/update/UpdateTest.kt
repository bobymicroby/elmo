package dev.boby.elmo.sandbox.update

import dev.boby.elmo.Sandbox
import dev.boby.elmo.testutil.TestView
import dev.boby.elmo.Update


import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.specs.StringSpec
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.locks.LockSupport


data class State(val counter: Int)


sealed class Msg {
    object Noop : Msg()
    object Decrement : Msg()
    object Increment : Msg()

}


class DelayingCommandsUpdate(private val delayNs: Long, override val updateScheduler: Scheduler) : Update<State, Msg> {
    override fun update(msg: Msg, model: State): State {
        return when (msg) {
            Msg.Noop -> model
            Msg.Increment -> {
                LockSupport.parkNanos(delayNs)
                model.copy(counter = model.counter + 1)
            }
            Msg.Decrement -> model.copy(counter = model.counter - 1)
        }
    }

}


class UpdateTest : StringSpec() {
    private val msgGen = Gen.list(Gen.from(listOf(Msg.Noop, Msg.Increment, Msg.Decrement)))

    init {
        "for all randomly generated messages feeded concurrently the counter should hold" {

            forAll(msgGen) { messages: List<Msg> ->

                val view = TestView<State>(Schedulers.io())
                val update = DelayingCommandsUpdate(1000, Schedulers.io())
                val seed = State(0)
                val sandbox = Sandbox.create(seed, update, view)


                for (op in messages) {
                    Thread {
                        sandbox.accept(op)
                    }.start()
                }

                val expectedViewUpdates = messages.size + 1
                while (view.models.size != expectedViewUpdates) {
                    LockSupport.parkNanos(100)
                }

                sandbox.dispose()

                if (view.models.isEmpty() && messages.isEmpty()) {
                    true
                } else {

                    val counter = view.models.last().counter
                    val expectedCounter = expectedCounter(messages)

                    expectedCounter == counter
                }
            }
        }


    }

    private fun expectedCounter(ops: List<Msg>): Int {
        return ops.foldRight(0) { msg, acc ->
            when (msg) {
                Msg.Increment -> acc + 1
                Msg.Decrement -> acc - 1
                else -> acc
            }
        }
    }
}


