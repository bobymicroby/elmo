package dev.boby.elmo.testutil

import dev.boby.elmo.View
import io.reactivex.Scheduler
import java.util.concurrent.CopyOnWriteArrayList

class TestView<Model>(override val viewScheduler: Scheduler) : View<Model> {


    val models = CopyOnWriteArrayList<Model>()

    override fun view(model: Model) {

        models.add(model)
    }


}
