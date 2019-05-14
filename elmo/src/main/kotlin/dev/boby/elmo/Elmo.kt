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
 * A way to update your model and trigger side-effects (make async calls for example) . It specifies
 * how the application's model changes in response to Messages and Commands sent to it. All code is
 * executed on the [updateScheduler]
 *
 * You Model must be immutable data class and you should always use the `copy` function inside
 * the [update] method in order to change the model .The Message and Command types
 * should  be immutable and form [sealed class](https://kotlinlang.org/docs/reference/sealed-classes.html)
 * hierarchies (aka Algebraic data types). Otherwise you risk facing concurrency problems.
 *
 * @param Model Type of the application state.
 * @param Message Type of the messages.
 * @param Command Type of the commands that trigger side-effects.
 *
 * @property none Marks one of your Command classes as the none (no-op) command.
 * @property updateScheduler Used for executing [update] method calls and subscribing to the
 * observable returned from [call]
 *
 *
 *
 */
interface CommandUpdate<Model, Message, Command> {

    /**
     * Marks one of your Command classes as the none (no-op) command.
     */
    val none: Command

    /**
     * Used for executing [update] method calls and subscribing
     * to the observables returned from [call].
     */
    val updateScheduler: Scheduler


    /**
     *
     * Updates the current application state and provides a way to schedule the execution
     * of a Command (side-effect)
     *
     * @param msg Incoming message.
     * @param model Current application state.
     *
     * @return new state and a command (side-effect) to execute. If you don't wish to execute
     *         a side-effect, return pair(state,[none]).
     *
     */
    fun update(msg: Message, model: Model): Pair<Model, Command>

    /**
     *
     * This function serves as a repository, mapping each command to a way to receive
     * (probably asynchronously) a stream of messages. Elmo doesn't require you to have a deep
     * knowledge of RxJava's [Observable] to use it, because there is already a easy way for
     * [creating observables](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables)
     * from various types. If this is not enough, there are tons of open-source interop libraries.
     *
     * The returned Observable will be subscribed on the provided [updateScheduler]
     *
     * @param cmd The Command which will trigger a subscription for a stream of messages
     * @return A stream of messages
     *
     */
    fun call(cmd: Command): Observable<out Message>


    /**
     * If one of your commands throws an unhandled exception this method is responsible
     * for transforming it to message. Usually this message signature will look like:
     * `data class Error(cmd: Command, t: Throwable) : Message()`
     *
     * Only non-fatal errors will be forwarded, the fatal ones will destroy the Sandbox as expected,
     * and will propagate up the stack.
     *
     * For commands that are expected to throw exceptions like network calls you your [Command]
     * should return a [Message] that contains a [Result] that can be either [Ok] or [Err] .
     * If you don't need information about the error you can use a [Message] that contains [Maybe].
     *
     */
    fun onUnhandledError(cmd: Command, t: Throwable): Message
}


/**
 *
 * A way to update your model,  It specifies how the application's model changes in response to
 * Messages  sent to it. All [update] calls are executed on the [updateScheduler]
 *
 * You Model must be immutable data class and you should always use the `copy` function inside
 * the [update] method in order to change the model .The Message type
 * should  be immutable and form [sealed class](https://kotlinlang.org/docs/reference/sealed-classes.html)
 * hierarchies (aka Algebraic data types). Otherwise you risk facing concurrency problems.
 *
 * @param Model Type of the application state.
 * @param Message Type of the messages.
 *
 * @property updateScheduler Used for executing [update] method calls and subscribing
 *                           to the observables returned from [call].
 *
 *
 */
interface Update<Model, Message> {

    /**
     * Used for executing [update] method calls
     */
    val updateScheduler: Scheduler


    /**
     *
     * Updates the current application state
     *
     * @param msg Incoming message.
     * @param model Current application state.
     *
     * @return new state
     */
    fun update(msg: Message, model: Model): Model


}

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
     * Called every time the [CommandUpdate] produces a new model. Used to display
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
         * @param seed The initial state of the program, if you don't want to trigger a side
         *             effect right away, start with pair(state,[Update.none])
         * @param update The [CommandUpdate] which will be used in the program's update event-loop
         * @param view The [View] which will receive [Model] updates
         *
         */
        fun <Model, Message, Command> create(
                seed: Pair<Model, Command>,
                update: CommandUpdate<Model, Message, Command>,
                view: View<Model>
        ): Sandbox<Message> {

            val messages = BehaviorSubject.create<Message>().toSerialized() // BehaviorSubject is used because otherwise the result from the first command will be lost
            val disposables = CompositeDisposable()
            val eventLoop = messages
                    .observeOn(update.updateScheduler)
                    .scan(seed) { (model, _), msg -> update.update(msg, model) }
                    .doOnNext { (_, cmd) ->
                        if (cmd != update.none) {
                            @Suppress("UNCHECKED_CAST")  //This is type-safe because the observable is covariant and read only.
                            val call = update.call(cmd) as Observable<Message>
                            disposables.add(call
                                    .onErrorReturn { t: Throwable -> update.onUnhandledError(cmd, t) }
                                    .subscribeOn(update.updateScheduler)
                                    .subscribe({ m -> messages.onNext(m) }, { e -> throw e }))
                        }
                    }
                    .observeOn(view.viewScheduler)

            disposables.add(
                    eventLoop.subscribe(
                            { (model, _) -> view.view(model) }
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
         * @param seed The initial state of the program
         * @param update The [Update] which will be used in the program's update event-loop
         * @param view The [View] which will receive [Model] updates
         *
         */
        fun <Model, Message> create(
                seed: Model,
                update: Update<Model, Message>,
                view: View<Model>
        ): Sandbox<Message> {

            val messages = PublishSubject.create<Message>().toSerialized()
            val disposable = messages
                    .observeOn(update.updateScheduler)
                    .scan(seed) { model, msg -> update.update(msg, model) }
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
