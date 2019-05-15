

<img src="logo.png" width="170">

[![Build Status](https://travis-ci.com/bobymicroby/elmo.svg?branch=master)](https://travis-ci.com/bobymicroby/elmo)


Elmo is the Android framework from Badge that I use for nearly all product development at [Badge](https://badge.app). When I began creating Elmo, my goal was not to create yet another architecture pattern like MVP, MVC,MVVM, it was to allow even a single person to deliver a large scale application easier, faster, and to have fun in the process. 

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




## Installation

Just add the dependency to your project `build.gradle` file:

```groovy
dependencies {
  implementation 'dev.boby.elmo:elmo:x.y.z'
}

```

Replace `x` and `y` and `z` with the latest version number: [![Maven Central](https://maven-badges.herokuapp.com/maven-central/dev.boby.elmo/elmo/badge.svg)](https://maven-badges.herokuapp.com/maven-central/dev.boby.elmo/elmo)


## Core Concepts

### Model
The state of your application. It must be immutable Kotlin [data class](https://kotlinlang.org/docs/reference/data-classes.html) that contains the 
properties  necessary to render your screen.

Because the model updates are  done concurrently,utilising many of the cores available in modern phones, it is very important that
the model is immutable.This means that you must always use [copy](https://kotlinlang.org/docs/reference/data-classes.html#copying) 
in order to change your model.



*Example*:

```kotlin 

data class WalletModel(val userName: String, val cents: Long)

```


### Update
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





### Messages

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

    data class Receive(val cents: Long) : Msg() // Receive message extends the base class
    data class Spend(val cents: Long) : Msg()  // So does and the Spend
}

```



### View

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


### Commands 

Used to trigger side-effects ( async api calls, etc) that can produce new 
`Messages`.  The `Command` types must form sealed class hierarchies just like your `Message`.

*Example*:

```kotlin 

sealed class Cmd {
    data class RequestFromMom(val cents: Long) : Cmd()
}

```


### Update with side-effects

There is another version of `Update` that can interact with the outside world, trigger 
side-effects and handle errors. In order to do so, there are three things that you must do:

- Describe how to get `Observable<Msg>` from a `Command`
- Provide a way to transform  unhandled errors back to `Messages`.
- Return either a `Pure(model)`  or `Effect(model,command)`


> You can think of Pure like it is a pair(model,`none`) and of Effect like it is a pair(model,command).


>[RxJava](https://github.com/ReactiveX/RxJava) `Observables` can be [created](https://github.com/ReactiveX/RxJava/wiki/Creating-Observables)
from most standard types and many http-clients, database drivers, etc, provide RxJava 
adapters. If you are new to RxJava elmo can help you use it while you learn it.


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


### Error handling

How the `Update` deal with errors:

- Runtime errors in `update` method calls will crash your application, and they should because they show
a bug in the code and lack of tests.

- Unhandled errors from Commands will be delivered to `onUnhandledError` where you can deal with them.
Usually you will want to transform them back to Message.

- Fatal errors from `update` method calls and from Commands will crash your application



### Result

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

