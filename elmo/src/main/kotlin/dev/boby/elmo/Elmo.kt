package dev.boby.elmo

import dev.boby.elmo.Sandbox.Companion.create
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject


/**
 *
 * The return type of the side-effecting [dev.boby.elmo.effect.Update] containing:
 *
 *  -- in its [Pure] variant just the updated model
 *  -- in its [Effect] variant the updated model and a command to execute
 */
sealed class Return<out Model, out Command> {
    abstract val model: Model
}

/**
 * You can think of the pure variant of [Result] as a type alias for pair(model,none)
 */
data class Pure<Model>(override val model: Model) : Return<Model, kotlin.Nothing>()

/**
 * You can think of the effect variant of [Result] as a type alias for pair(model,cmd)
 */
data class Effect<Model, Command>(override val model: Model, val cmd: Command) : Return<Model, Command>()


/**
 * Syntactic sugar for creating a [Pure], and we all now that too much sugar is bad for your
 * health.
 *
 * Example:
 * model + none == Pure(model)
 *
 */
operator fun <Model, Command> Model.plus(@Suppress("UNUSED_PARAMETER") none: None): Return<Model,
        Command> = Pure(this)

/* Marks that there are no commands.*/
object None

/**
 * Syntactic sugar for creating a [Effect], and we all now that too much sugar is bad for
 * your health.
 *
 * Example:
 * model + Cmd.Increment == Effect(model,Cmd.Increment)
 *
 */
operator fun <Model, Command> Model.plus(that: Command): Return<Model, Command> = Effect(this, that)




/**
 * Receives all view updates . Should be used to render your model on screen.
 * Usually you will implement this interface in your Activity or Fragment
 *
 * @property viewScheduler Used for executing  the [view]  method. Typically this will be your
 *                         main thread scheduler in a UI app.
 */
interface View<Model> {
    val viewScheduler: Scheduler

    /**
     * Called every time the Update produces a new model. Used to display
     * the model in the UI
     *
     * @param model the latest model to display
     */
    fun view(model: Model)
}

/**
 *
 * The Sandbox  contains the program's update event-loop which is started by calling [create].
 * It can [accept] messages, update the model based on them, trigger side effects and call your
 * view back with the updated model.
 *
 * The Sandbox is hot, meaning  once created, it will start pushing model updates to the
 * provided [View]. It is of uttermost importance to understand that once you call [create], you
 * must **always**  call [dispose], otherwise you will **leak memory**. So if you are Android
 * Engineer, the rule of thumb is : call [create] in Activity.onCreate() and call [dispose] in
 * Activity.onDestroy()
 *
 */
abstract class Sandbox<Message> private constructor() : Disposable {

    /**
     * Feeds a message to the program's update event-loop which can result in a model update.
     *
     * @param msg Messages can represent keystrokes, location data, responses from a remote api or
     *            any type of data you want the sandbox to react to.
     */
    abstract fun accept(msg: Message)

    companion object {

        /**
         * Creates and starts a Sandbox  that can trigger Commands and returns a handle to it.
         * The Sandbox is hot, meaning it will already be sending model updates to the provided
         * [view] before this method returns. Sandbox will keep a reference to the caller of
         * this method meaning you will leak references if you don't call [dispose] after you are
         * done with it. So if you are a Android Engineer, call [create] in your
         * Activity.onCreate and call [dispose] in your Activity.onDestroy
         *
         * @param initial The initial state of the program, if you don't want to trigger a side
         *             effect right away use Pure(model), otherwise use Effect(model,cmd)
         * @param update The [dev.boby.elmo.effect.Update] which will be used in the program's update event-loop
         * @param view The [View] which will receive [Model] updates
         *
         */
        fun <Model, Message, Command> create(
                initial: Return<Model, Command>,
                update: dev.boby.elmo.effect.Update<Model, Message, Command>,
                view: View<Model>
        ): Sandbox<Message> {

            val messages = BehaviorSubject.create<Message>().toSerialized() // BehaviorSubject is used because otherwise the result from the first command will be lost
            val disposables = CompositeDisposable()
            val eventLoop = messages
                    .observeOn(update.updateScheduler)
                    .scan(initial) { computation, msg -> update.update(msg, computation.model) }
                    .doOnNext { computation ->
                        if (computation is Effect) {
                            @Suppress("UNCHECKED_CAST")  //This is type-safe because the observable is covariant and read only.
                            val call = update.call(computation.cmd) as Observable<Message>
                            disposables.add(call
                                    .onErrorReturn { t: Throwable -> update.onUnhandledError(computation.cmd, t) }
                                    .subscribeOn(update.updateScheduler)
                                    .subscribe({ m -> messages.onNext(m) }, { e -> throw e }))
                        }
                    }
                    .observeOn(view.viewScheduler)

            disposables.add(
                    eventLoop.subscribe(
                            { computation -> view.view(computation.model) }
                            , { e -> throw e }
                    )
            )
            return object : Sandbox<Message>() {
                override fun isDisposed(): Boolean {
                    return disposables.isDisposed
                }

                override fun dispose() {
                    disposables.dispose()
                }

                override fun accept(msg: Message) {
                    messages.onNext(msg)
                }
            }
        }

        /**
         * Creates and starts a Sandbox that cannot trigger Commands and returns a handle to it.
         * The Sandbox is hot, meaning it will already be sending model updates to the provided
         * [view] before this method returns. Sandbox will keep a reference to the caller of this
         * method meaning you will leak references if you don't call [dispose] after you are done
         * with it. So if you are  a Android Engineer, call [create] in your Activity.onCreate
         * and call [dispose] in your Activity.onDestroy
         *
         * @param initial The initial model of the program
         * @param update The [dev.boby.elmo.pure.Update] which will be used in the program's update
         * event-loop
         * @param view The [View] which will receive [Model] updates
         *
         */
        fun <Model, Message> create(
                initial: Model,
                update: dev.boby.elmo.pure.Update<Model, Message>,
                view: View<Model>
        ): Sandbox<Message> {

            val messages = PublishSubject.create<Message>().toSerialized()
            val disposable = messages
                    .observeOn(update.updateScheduler)
                    .scan(initial) { model, msg -> update.update(msg, model) }
                    .observeOn(view.viewScheduler)
                    .subscribe({ model -> view.view(model) }, { e -> throw e })

            return object : Sandbox<Message>() {
                override fun isDisposed(): Boolean {
                    return disposable.isDisposed
                }

                override fun dispose() {
                    disposable.dispose()
                }

                override fun accept(msg: Message) {
                    messages.onNext(msg)
                }
            }
        }
    }
}
