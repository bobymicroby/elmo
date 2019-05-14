package com.example.elmo.elmo

import dev.boby.elmo.Update
import dev.boby.elmo.CommandUpdate
import dev.boby.elmo.View
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers


interface UpdateCommandIO<State, Msg, Cmd> : CommandUpdate<State, Msg, Cmd> {

    override val updateScheduler: Scheduler get() = Schedulers.io()
}


interface UpdateIO<State, Msg> : Update<State, Msg> {
    override val updateScheduler: Scheduler get() = Schedulers.io()
}


interface MainThreadView<Model> : View<Model> {
    override val viewScheduler: Scheduler
        get() = AndroidSchedulers.mainThread()
}