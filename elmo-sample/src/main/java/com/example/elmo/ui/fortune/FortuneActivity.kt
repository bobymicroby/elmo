package com.example.elmo.ui.fortune

import android.app.Activity
import android.os.Bundle
import androidx.core.view.isVisible
import com.example.elmo.R
import com.example.elmo.elmo.UpdateCommandIO
import dev.boby.elmo.*
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.demo_activity.*

import java.net.URL

data class State(val loading: Boolean, val fortune: String)
sealed class Msg {
    object Refresh : Msg()
    data class Fortune(val fortune: String) : Msg()
}
sealed class Cmd {
    object CallFortuneApi : Cmd()
}

class Update : UpdateCommandIO<State, Msg, Cmd> {

    override fun update(msg: Msg, model: State): Computation<State, Cmd> {
        return when (msg) {
            Msg.Refresh -> Effect(model.copy(loading = true), Cmd.CallFortuneApi)
            is Msg.Fortune -> Pure(model.copy(loading = false, fortune = msg.fortune))
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            Cmd.CallFortuneApi -> {
                Observable.fromCallable { Msg.Fortune(URL("https://helloacm.com/api/fortune/").readText()) }
            }
        }
    }
    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        return Msg.Fortune("Snap! Cannot load a fortune right now. ")
    }
}

class FortuneActivity : Activity(), View<State> {
    override val viewScheduler: Scheduler get() = AndroidSchedulers.mainThread()
    private lateinit var sandbox: Sandbox<Msg>

    override fun view(model: State) {
        fortune.text = model.fortune
        progress.isVisible = model.loading
        fortune.isVisible = !model.loading
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.demo_activity)
        val initialState = Effect(State(loading = true, fortune = ""), Cmd.CallFortuneApi)
        sandbox = Sandbox.create(initialState, Update(), this)
        refresh.setOnClickListener {
            sandbox.accept(Msg.Refresh)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        sandbox.dispose()
    }
}
