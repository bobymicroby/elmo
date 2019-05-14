package dev.boby.elmo.data

import dev.boby.elmo.*
import dev.boby.elmo.Nothing
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class DataTest : StringSpec() {


    init {
        "Maybe is sane" {

            val nothing: Maybe<String> = Nothing
            nothing.map { "b" } shouldBe Nothing
            nothing.flatMap { Just(1) } shouldBe Nothing
            nothing.flatMap { Nothing } shouldBe Nothing


            nothing.getOrNull() shouldBe null

            val justOne: Maybe<Int> = Just(1)
            justOne.getOrNull() shouldBe 1

            justOne.map { a -> a.toString() } shouldBe Just("1")
            justOne.flatMap { a -> Just("1_$a") } shouldBe Just("1_1")
            justOne.flatMap { Nothing } shouldBe Nothing


        }

        "Result is sane" {

            val err = Err("err")
            val ok = Ok("ok")

            val r1: Result<String, String> = err
            val r2: Result<String, String> = ok

            r1.map { o -> "$o map $o" } shouldBe err
            r2.map { o -> "$o map $o" } shouldBe Ok("ok map ok")

            r1.flatMap { ok } shouldBe err
            r1.flatMap { err } shouldBe err
            r2.flatMap { err } shouldBe err
            r2.flatMap { o -> Ok("$o fmap $o") } shouldBe Ok("ok fmap ok")

            r1.mapError { e -> "$e map $e" } shouldBe Err("err map err")
            r2.mapError { e -> "$e map $e" } shouldBe Ok("ok")

            r1.withDefault("ko") shouldBe "ko"
            r2.withDefault("ko") shouldBe "ok"

            r1.toMaybe() shouldBe Nothing
            r2.toMaybe() shouldBe Just("ok")

            r1.getOrNull() shouldBe null
            r2.getOrNull() shouldBe "ok"

        }

    }




}