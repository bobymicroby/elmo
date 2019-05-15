package dev.boby.elmo.pure

import io.reactivex.Scheduler

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