package dev.boby.elmo.effect

import dev.boby.elmo.Return
import io.reactivex.Observable
import io.reactivex.Scheduler

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
 * @property none Marks one of your Command classes as the None (no-op) command.
 * @property updateScheduler Used for executing [update] method calls and subscribing to the
 * observable returned from [call]
 *
 *
 *
 */
interface Update<Model, Message, Command> {


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
     * a side-effect, return [dev.boby.elmo.Pure] result, otherwise return [dev.boby.elmo.Effect]
     *
     */
    fun update(msg: Message, model: Model): Return<Model, Command>

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
     * should return a [Message] that contains a [dev.boby.elmo.Result] that can be either [dev.boby.elmo.Ok] or
     * [dev.boby.elmo.Err] .
     * If you don't need information about the error you can use a [Message] that contains [dev.boby.elmo.Maybe].
     *
     */
    fun onUnhandledError(cmd: Command, t: Throwable): Message

}