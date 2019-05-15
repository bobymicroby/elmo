package com.example.elmo.elmo.extras.effect

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

interface UpdateIO<State, Msg, Cmd> :  dev.boby.elmo.effect.Update<State, Msg, Cmd> {

    override val updateScheduler: Scheduler get() = Schedulers.io()
}
