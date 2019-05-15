package com.example.elmo.elmo.extras

import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers

interface View<Model> : dev.boby.elmo.View<Model> {
    override val viewScheduler: Scheduler
        get() = AndroidSchedulers.mainThread()
}