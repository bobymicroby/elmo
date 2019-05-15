

<img src="logo.png" width="170">

[![Build Status](https://travis-ci.com/bobymicroby/elmo.svg?branch=master)](https://travis-ci.com/bobymicroby/elmo)


Elmo is the Android framework from Badge Labs that I use for nearly all product development at [Badge](https://badge.app). When I began creating Elmo, my goal was not to create yet another architecture pattern like MVP, MVC,MVVM, it was to allow even a single person to deliver a large scale application easier, faster, and to have fun in the process. 

Elmo will help you write your android applications in a modern, unidirectional and functional way. You will have easy and great tests for free, and you will never have to deal with concurrency and async issues.

I am a firm believer in functional programming, and I know that for many Android developers this is still uncharted territory, but if you keep reading until the end, I promise you that you will rise again, harder and stronger! For what is immutable my never change!


What does it looks like:
```kotlin
data class HelloWorldModel(val title: String)

sealed class Msg {
    object Reverse : Msg()
}

class HelloWorldUpdate : Update<HelloWorldModel, Msg> {
   
   override fun update(msg: Msg, model: HelloWorldModel): HelloWorldModel {
        return when (msg) {
            Msg.Reverse -> model.copy(title = model.title.reversed())
        }
    }
}

class HelloWorldActivity : Activity(), View<HelloWorldModel> {

    private lateinit var sandbox: Sandbox<Msg>

    override fun view(model: HelloWorldModel) {
        textview.text = model.title
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hello_activity)
        sandbox = Sandbox.create(HelloWorldModel(title = "Hello World!"), HelloWorldUpdate(), this)
        button.setOnClickListener { sandbox.accept(Msg.Reverse) }
    }

    override fun onDestroy() {
        super.onDestroy()
        sandbox.dispose()
    }
}

```

For those familiar with React, Elmo will feel a lot like Redux with fancy async middleware. For 
those familiar with Elm, well it will look a lot like Elm. And for those still unfamiliar with 
unidirectional data flow architectures , immutable state-containers and the lot, it will be 
something new and exciting to learn and it will empower you to write easy, fast and thread-safe UI 
applications.

I am working on a tutorial in a series of blog posts that you can find on my [blog](https://boby.dev)

* [Installation](#installation)
* [Model](#Model)
* [Update](#Update)
* [Messages](#Messages)
* [View](#View)
* [Update with side-effects](#update-with-side-effects)
* [Error handling](#error-handling)
* [Result](#Result)
* [Multithreading](#Multithreading)
* [Testing pure code](#testing-pure-code)
* [Testing code with side-effects in a pure way](#testing-code-with-side-effects-in-a-pure-way)
* [Testing code with side-effects by mocking](#testing-code-with-side-effects-by-mocking)
* [Working with lists](#working-with-lists)








## Installation

Just add the dependency to your project `build.gradle` file:

```groovy
dependencies {
  implementation 'dev.boby.elmo:elmo:2.y.z'
}

```

Replace and `y` and `z` with the latest version number: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.boby.elmo/elmo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.boby.elmo/elmo)




## Model
The state of your application. It must be immutable Kotlin [data class](https://kotlinlang.org/docs/reference/data-classes.html) that contains the 
properties  necessary to render your screen.

Because the model updates are  done concurrently,utilising many of the cores available in modern phones, it is very important that
the model is immutable.This means that you must always use [copy](https://kotlinlang.org/docs/reference/data-classes.html#copying) 
in order to change your model.



*Example*:

```kotlin 

data class WalletModel(val userName: String, val cents: Long)

```


## Update
Provides a way to update your `Model` It specifies how the application's model changes in 
response to  `Messages`  sent to it. If you prefer this term - it is the thing that handles all 
business logic. 


*Example*:

```kotlin 

class WalletUpdate : Update<WalletModel, Msg> {

    override fun update(msg: Msg, model: WalletModel): WalletModel {
        return when (msg) {
            is Msg.Receive -> model.copy(cents = model.cents + msg.cents)
            is Msg.Spend -> model.copy(cents = model.cents - msg.cents)
        }
    }
}

```





## Messages

Are what the `Update` reacts in order to to update your model. They can represent taps on the 
screen, responses from external api, data from your phone sensors, etc.

The `Message` types must form [sealed class](https://kotlinlang.org/docs/reference/sealed-classes.html) 
hierarchies and must be immutable. Then you can use them to do pattern match in `return when` 
statements as shown in the `Update` example. Also when you add new message type, the compiler
will ask you to add the remaining branches in order for the `when` statement to be exhaustive, 
saving you from bugs.


*Example*:

```kotlin 

sealed class Msg { // The  base `Message` sealed class

    data class Receive(val cents: Long) : Msg()     // Receive message extends the base class
    data class Spend(val cents: Long) : Msg()      // So does and the Spend
}

```



## View

Responsible for rendering your model on screen. It is a interface that is usually implemented in 
your fragments or activities.

*Example*:

```kotlin 

class WalletActivity : View<WalletModel>, Activity() {

    override fun view(model: WalletModel) {
        userName.text = model.userName
        cents.text =model.cents
    }
}
```


## Commands 

Used to trigger side-effects ( async api calls, etc) that can produce new 
`Messages`.  The `Command` types must form sealed class hierarchies just like your `Message`.

*Example*:

```kotlin 

sealed class Cmd {
    data class RequestFromMom(val cents: Long) : Cmd()
}

```


## Update with side-effects

There is another version of `Update` that can interact with the outside world, trigger 
side-effects and handle errors. In order to do so, there are three things that you must do:

- Describe how to get `Observable<Msg>` from a `Command`
- Provide a way to transform  unhandled errors back to `Messages`.
- Return either a `Pure(model)`  or `Effect(model,command)` when calling the `update` method




*Example*:

```kotlin
class WalletUpdate : effect.Update<WalletModel, Msg, Cmd> {

    override fun update(msg: Msg, model: WalletModel): Return<WalletModel, Cmd> {
        return when (msg) {
            is Msg.Receive -> Pure(model.copy(cents = model.cents + msg.cents))
            is Msg.Spend -> {
                val newModel = model.copy(cents = model.cents - msg.cents)
                if (newModel.cents < 0) {
                    Effect(newModel, Cmd.RequestFromMom(0 - newModel.cents))
                } else {
                    Pure(newModel)
                }
            }
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            is Cmd.RequestFromMom -> Observable.fromCallable {
                Msg.Receive(cmd.cents) //this can be a async http call or database read or any other side effect
            }
        }
    }

    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        System.err.println("Error while processing $cmd: $t")
        return Msg.Receive(0)
    }


}
```

> You can think of Pure like it is a pair(model,`none`) and of Effect like it is a pair(model,command).


>[RxJava](https://github.com/ReactiveX/RxJava) `Observables` can be [created](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables)
from most standard types and many http-clients, database drivers, etc, provide RxJava 
adapters. If you are new to RxJava elmo can help you use it while you learn it.



## Error handling

How the `Update` deal with errors:

- Runtime errors in `update` method calls will crash your application, and they should because they show
a bug in the code and lack of tests.

- Unhandled errors from Commands will be delivered to `onUnhandledError` where you can deal with them.
Usually you will want to transform them back to Message.

- Fatal errors from `update` method calls and from Commands will crash your application



## Result

You can deal with specific Command errors at declaration site using `onErrorReturn` and the `Result` type

 
The Result type represents values with two possibilities: 
a value of type `Result<E,A>` is either `Err<E>` or `Ok<A>`.

The Result type is sometimes used to  represent a computation that may fail and it is a great
way to manage errors. The `Err` constructor is used to hold an error value and the `Ok`
constructor is used to hold a correct value.
 
Example :

    
```kotlin
sealed class Msg {
    /**
    *In this case the Err type is String ( error message ) and the Ok type is Long ( cents received)  
    */
    data class BankResponse(val result: Result<String, Long>) : Msg()
}

sealed class Cmd {
    data class RequestFromBankApi(val cents: Long) : Cmd()
}

class WalletUpdate : Update<WalletModel, Msg, Cmd> {

    override fun update(msg: Msg, model: WalletModel): Return<WalletModel, Cmd> {
        return when (msg) {
            is Msg.BankResponse -> {
                when (msg.result) {
                    is Ok -> Pure(model.copy(cents = model.cents + msg.result.value))
                    is Err -> Pure(model)
                }
            }
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            is Cmd.RequestFromBankApi -> Observable.fromCallable {
                if (cmd.cents > 100) {
                    throw RuntimeException()
                } else {
                Msg.BankResponse( Ok(cmd.cents) )
                }
               
            }.onErrorReturn { t -> Msg.BankResponse( Err("Snap!")) } 
        }
    }
    
}

```    

## Multithreading

RxJava is so popular with Android developers because it makes switching computation contexts easy.
Elmo makes it even easier. What most of you want is to render your model on your main thread,
and do model updates and execute commands concurrently, so you will not ever have to block.

So to have a Update that asynchronously do updates and executes commands you can do:

```kotlin
interface Update<Model, Msg> : dev.boby.elmo.pure.Update<Model, Msg> {
    override val updateScheduler: Scheduler
        get() = Schedulers.io()
}
```

And to have a View that performs rendering on the main thread 

```kotlin
interface View<Model> : dev.boby.elmo.View<Model> {
    override val viewScheduler: Scheduler
        get() = AndroidSchedulers.mainThread()
}
```

You can then extend this interfaces everywhere in your app.

> AndroidSchedulers.mainThread() is courtesy of the [RxAndroid](https://github.com/ReactiveX/RxAndroid) authors


## Testing pure code

Testing pure `Update` is pretty straightforward. We can just pass a test model and a message to the update function and assert we have the correct response.
This is why it is very important the update function is kept [referentially transparent](https://en.wikipedia.org/wiki/Referential_transparency),
If you are using immutable model and not doing any side-effects it should be, so tests will come by easy. In future versions
elmo will have a linter that warn you if your update function is not pure.

Example:

```kotlin
class WalletUpdate : Update<WalletModel, Msg> {

    override fun update(msg: Msg, model: WalletModel): WalletModel {
        return when (msg) {
            is Msg.Receive -> model.copy(cents = model.cents + msg.cents)
            is Msg.Spend -> model.copy(cents = model.cents - msg.cents)
        }
    }
}

class Test {
    @Test
    fun sanity() {
        val update = WalletUpdate()
        val initialState = WalletModel("Richie Rich", 0)
        val richer = update.update(Msg.Receive(100), initialState)
        assertTrue(richer.cents == 100L)
        val poorer = update.update(Msg.Spend(100), richer)
        assertTrue(poorer == initialState)
    }
}

```

## Testing code with side-effects in a pure way

Testing asynchronous side-effects is always hard and the test results struggle to reach 100% reliability.
Some languages and tools make it reasonable enough. Elmo allows you to test your logic without executing
and mocking your side-effects, but it makes it easy if you want to do so.

Let's see how you can test side-effecting code without executing any side-effects. This update bellow
is supposed to retry only once after an error. 


```kotlin
data class WalletModel(val cents: Long, val canRetry: Boolean)
sealed class Msg {
    data class BankResponse(val result: Result<Long, Long>) : Msg()
}
sealed class Cmd {
    data class RequestFromBankApi(val cents: Long) : Cmd()
}

class Update : dev.boby.elmo.effect.Update<WalletModel, Msg, Cmd> {
    override fun update(msg: Msg, model: WalletModel): Return<WalletModel, Cmd> {
        return when (msg) {
            is Msg.BankResponse -> {
                when (msg.result) {
                    is Ok -> Pure(model.copy(canRetry = true, cents = model.cents + msg.result.value))
                    is Err -> if (model.canRetry) {
                        Effect(model.copy(canRetry = false), Cmd.RequestFromBankApi(msg.result.error))
                    } else {
                        Pure(model)
                    }
                }
            }
        }
    }
}

@Test
fun shouldRetryOnlyOnce() { 

            val initialState = WalletModel(cents = 0, canRetry = true)
            val errRes = Msg.BankResponse(Err(100))
            val update = Update()
            
            val r1 = update.update(errRes, initialState)
            val r2 = update.update(errRes, r1.model)

            r1 shouldBe Effect(WalletModel(cents = 0, canRetry = false), Cmd.RequestFromBankApi(100))
            r2 shouldBe Pure(WalletModel(cents = 0, canRetry = false))
            
            
       }


```

Since exceptions from commands are always transformed into messages writing your tests this
is sane, and if there is a runtime error in your update it will show up in your tests. 
 

## Testing code with side-effects by mocking 

So, if you want to test the complete machinery, it doesnt matter what language or tool you use,
you need to choose between using a 'mock' interpreter for your side-effects or actually run them.
Running them can be slow and unpredictable if you are making network calls.
The example bellow can be easily and reliably tested as shown in *Testing code with side-effects in a pure way*, but
there is nothing stopping you to follow the example bellow.


This example is very involved, and I am working on easy to follow tutorial in a series of blog posts.

Example: 

```kotlin

sealed class Msg {
    data class RequestMoney(val requestedAmount: Long) : Msg()
    data class BankResponse(val result: Result<BankError, Long>) : Msg()
    data class Error(val errorMessage: String) : Msg()
}

sealed class Cmd {
    data class RequestFromBankApi(val cents: Long) : Cmd()
}

interface CommandInterpreter<Message, Command> {
    fun call(cmd: Command): Observable<out Message>
}

class TestInterpreter(private val shouldThrow: Boolean) : CommandInterpreter<Msg, Cmd> {
    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            is Cmd.RequestFromBankApi -> Observable.fromCallable {
                if (shouldThrow) {
                    throw  RuntimeException("Bang")
                } else {
                    Msg.BankResponse(Ok(cmd.cents))
                }
            }
        }
    }
}

class TestView<Model>(override val viewScheduler: Scheduler) : View<Model> {
    val models = CopyOnWriteArrayList<Model>()
    override fun view(model: Model) {
        models.add(model)
    }
}



class Update(override val updateScheduler: Scheduler,
             private val interpreter: CommandInterpreter<Msg, Cmd>) : dev.boby.elmo.effect.Update<WalletModel, Msg, Cmd> {

    override fun update(msg: Msg, model: WalletModel): Return<WalletModel, Cmd> {
        return when (msg) {
            is Msg.BankResponse -> {
                when (msg.result) {
                    is Ok -> Pure(model.copy(canRetry = true, cents = model.cents + msg.result.value))
                    is Err -> if (model.canRetry) {
                        Effect(model.copy(canRetry = false), Cmd.RequestFromBankApi(msg.result.error.requestedAmount))
                    } else {
                        Pure(model.copy(error = msg.result.error.errorMessage))
                    }
                }
            }
            is Msg.RequestMoney -> {
                Effect(model, Cmd.RequestFromBankApi(msg.requestedAmount))
            }
            is Msg.Error -> {
                Pure(model.copy(error = msg.errorMessage))
            }
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return interpreter.call(cmd)
    }

    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        return Msg.Error("Unknown error occurred")
    }
}


class UpdateTest : StringSpec() {
    val initial = Pure(WalletModel(cents = 0, canRetry = true, error = null))
    val sameThreadScheduler = Schedulers.trampoline()
    val notThrowingInterpreter = TestInterpreter(shouldThrow = false)
    val throwingInterpreter = TestInterpreter(shouldThrow = true)

    init {
        "If there is no error while processing a command, the request should succeed" {
            val testView = TestView<WalletModel>(sameThreadScheduler)
            val sandbox = Sandbox.create(initial, Update(sameThreadScheduler, notThrowingInterpreter), testView)
            sandbox.accept(Msg.RequestMoney(100))

            testView.models.last() shouldBe WalletModel(100, true, null)
            sandbox.dispose()

        }
        "If there is unhandled error while processing a command, the request should fail" {
            val testView = TestView<WalletModel>(sameThreadScheduler)
            val sandbox = Sandbox.create(initial, Update(sameThreadScheduler, throwingInterpreter), testView)
            sandbox.accept(Msg.RequestMoney(100))

            testView.models.last() shouldBe WalletModel(0, true, "Unknown error occurred")
            sandbox.dispose()

        }
    }
}

```

## Working with lists

Lists present unique challenges, especially on Android. 

- There are no persistent ( immutable ) collections present so updating  them  in place will introduce 
concurrency problems
- When you render them, you must use RecyclerViews, otherwise you risk OOM , GC pauses and all other 
things that steal those precious fps and make your apps unresponsive. Replacing the RecyclerView
collection  every time your Update provides you with a new list means you need to use DiffUtil 
to calculate the difference between the old and the new list, otherwise the fps gained by using 
a RecyclerView are lost. 

The first problem is easily solvable by using the [pcollections](https://github.com/hrldcpr/pcollections)
lib which will provide you with immutable collections. Let's hope in the near future kotlin stdlib
will provide us with persistent and immutable collections.

The second problem is already solved by the folks at AirBnb with [epoxy](https://github.com/airbnb/epoxy).
It is incredible library simplifying working with  RecyclerViews so much, that in my opinion it
should be part of the android stdlib.

Google already are making steps towards a functional  UI programming model with [Jetpack Compose](https://developer.android.com/jetpack/compose)
It is still in alpha but when it is production ready it will be a perfect match for Elmo's views.


License
-------

     Copyright 2019 Borislav Ivanov

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

