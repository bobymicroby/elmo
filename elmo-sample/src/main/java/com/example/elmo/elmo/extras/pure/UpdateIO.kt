package com.example.elmo.elmo.extras.pure

import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers

interface UpdateIO<State, Msg> : dev.boby.elmo.pure.Update<State, Msg> {
    override val updateScheduler: Scheduler get() = Schedulers.io()
}