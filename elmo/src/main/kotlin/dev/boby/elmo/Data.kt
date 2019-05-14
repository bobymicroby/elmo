package dev.boby.elmo

/**
 * The Maybe type encapsulates an optional value. A value of type Maybe<[A]> either contains a
 * value of type [A] (represented as Just<[A]>), or it is empty (represented as [Nothing]).
 *
 * Using Maybe is a good way to deal with errors or exceptional cases without resorting to
 * drastic measures such as exceptions. A richer version of Maybe is the [Result] type, which can
 * hold information about the error (represented as [Err]) instead of simply [Nothing].
 */
sealed class Maybe<out A>

/**
 * Represents an empty [Maybe]
 */
object Nothing : Maybe<kotlin.Nothing>()

/**
 * Represents an [Maybe] that contains value of type [A] (represented as Just<[A]>)
 */
data class Just<A>(val value: A) : Maybe<A>()

/**
 * If the maybe is [Just] it will transform the value it contains using the function [f].
 * If this maybe is [Nothing] it will do nothing.
 *
 * Example:
 *
 * Just(1).map { a-> a + 1 } == Just(2)
 * Nothing.map { a-> a + 1 } == Nothing
 *
 */
inline fun <A, B> Maybe<A>.map(f: (value: A) -> B): Maybe<B> {
    return when (this) {
        is Nothing -> Nothing
        is Just -> Just(f(value))
    }
}

/**
 * Allows you to chain a second maybe based on the value of this maybe.
 * If the type of this maybe is [Nothing] it will return [Nothing], otherwise it will
 * return new maybe containing [B], resulting from calling [f] with the value [A] of this maybe.
 *
 * Example:
 * Just(1).flatMap { a -> if (a==1) Just(2) else Nothing } == Just(2)
 * Just(2).flatMap { a -> if (a==1) Just(2) else Nothing } == Nothing
 * Nothing.flatMap { a -> if (a==1) Just(2) else Nothing } == Nothing
 *
 *
 */
inline fun <A, B> Maybe<A>.flatMap(f: (value: A) -> Maybe<B>): Maybe<B> {
    return when (this) {
        is Nothing -> Nothing
        is Just -> f(value)
    }
}

/**
 * If this maybe is [Nothing] it will return null, otherwise it will return [A]
 *
 * Example:
 *
 * Just(1).getOrNull() == 1
 * Nothing.getOrNull() == null
 *
 */
fun <A> Maybe<A>.getOrNull(): A? {
    return when (this) {
        is Just -> value
        is Nothing -> null
    }
}

/**
 * The Result type represents values with two possibilities:
 * a value of type [Result]<[E],[A]> is either [Err]<[E]> or [Ok]<[A]>.
 *
 * The Result type is sometimes used to  represent a computation that may fail and it is a great
 * way to manage errors. The [Err] constructor is used to hold an error value and the [Ok]
 * constructor is used to hold a correct value.
 *
 */
sealed class Result<out E, out A>

/**
 * Type used to hold the correct value [A] from a [Result]<E,[A]>
 */
data class Ok<A>(val value: A) : Result<kotlin.Nothing, A>()

/**
 * Type used to hold the error value [E] from a [Result]<[E],A>
 */
data class Err<E>(val error: E) : Result<E, kotlin.Nothing>()


/**
 * If the result is [Ok] it will transform the [A] value it contains using the function [f].
 * If this result is [Err] it will do nothing.
 *
 * Example:
 *
 * Ok(1).map { a-> a + 1 } == Ok(2)
 * Err("Snap!").map { a-> a + 1 } == Err("Snap!")
 *
 */
inline fun <A, B, E> Result<E, A>.map(f: (value: A) -> B): Result<E, B> {
    return when (this) {
        is Ok -> Ok(f(value))
        is Err -> this
    }
}

/**
 * Allows you to chain a second result based on the value of this result.
 * If this result is [Err] it will return the same [Err], otherwise it will
 * return new [Result] produced by calling [f]  with the [A] value of this result.
 *
 * Example:
 * Ok(1).flatMap { a -> if (a==1) Ok(2) else Err("not 1") } == Ok(2)
 * Ok(2).flatMap { a -> if (a==1) Ok(2) else Err("not 1") } == Err("not 1")
 * Err("Snap!").flatMap { a -> if (a==1) Ok(2) else Err("not 1") ) == Err("Snap!")
 *
 *
 */
inline fun <A, B, E> Result<E, A>.flatMap(f: (value: A) -> Result<E, B>): Result<E, B> {
    return when (this) {
        is Ok -> f(value)
        is Err -> this
    }
}

/**
 * Transform an [Err] value.
 *
 * Example:
 *
 * Err("Snap!").mapError { err-> "$err logged at 17 Nov 1983" } == Err("Snap! logged at 17 Nov 1983")
 * Ok(1).mapError { err-> "$err logged at 17 Nov 1983" } == Ok(1)
 *
 */
inline fun <A, E, E1> Result<E, A>.mapError(f: (error: E) -> E1): Result<E1, A> {
    return when (this) {
        is Ok -> this
        is Err -> Err(f(error))
    }
}

/**
 * If the result is [Ok] return the value, but if the result is an [Err] then return a
 * given default value.
 *
 * Example:
 *
 * Err("Snap!").withDefault("ko") == "ko"
 * Ok("ok").withDefault("ok") == "ok"
 */
fun <A, E> Result<E, A>.withDefault(a: A): A {
    return when (this) {
        is Ok -> value
        is Err -> a
    }
}

/**
 * Convert to a simpler [Maybe] if the actual [Err] is not needed or you need to interact with
 * some code that primarily uses maybes.
 *
 * Example:
 *
 * Err("Snap!").toMaybe() shouldBe Nothing
 * Ok("ok").toMaybe() shouldBe Just("ok")
 *
 */
fun <A, E> Result<E, A>.toMaybe(): Maybe<A> {
    return when (this) {
        is Ok -> Just(value)
        is Err -> Nothing
    }
}

/**
 * If this result is [Err] it will return null, otherwise it will return the [A] contained in the
 * [Ok]
 *
 * Example:
 *
 * Ok(1).getOrNull() == 1
 * Err("Snap!").getOrNull() == null
 *
 */
fun <A, E> Result<E, A>.getOrNull(): A? {
    return when (this) {
        is Ok -> value
        is Err -> null
    }
}


