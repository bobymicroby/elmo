

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
those familiar with Elm, well it will be like Elm. And for those still unfamiliar with 
unidirectional data flow architectures , immutable state-containers and the lot, it will be 
something new and exciting to learn and it will empower you to write easy, fast and thread-safe UI 
applications.




* [Installation](#installation)



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


### Command Update

This is more complex version of `Update` that can interact with the outside world, trigger 
side-effects and handle errors. If your activity or fragment do not make api calls, or store
things in a database, then use the simpler version.



*Example*:

```kotlin 

class WalletCommandUpdate : CommandUpdate<WalletModel, Msg, Cmd> {

    override val none: Cmd get() = Cmd.None

    override fun update(msg: Msg, model: WalletModel): Pair<WalletModel, Cmd> {
        return when (msg) {
            is Msg.Receive -> Pair(model.copy(cents = model.cents + msg.cents), none)
            is Msg.Spend -> {

                val newModel = model.copy(cents = model.cents - msg.cents)

                if (newModel.cents < 0) {
                    Pair(newModel, Cmd.RequestFromMom(0 - newModel.cents))
                } else {
                    Pair(newModel, none)
                }
            }
        }
    }

    override fun call(cmd: Cmd): Observable<out Msg> {
        return when (cmd) {
            is Cmd.RequestFromMom -> Observable.fromCallable {
                Msg.Receive(cmd.cents)
            }
            Cmd.None -> Observable.empty() 
        }
    }
    override fun onUnhandledError(cmd: Cmd, t: Throwable): Msg {
        System.err.println("Error while processing $cmd: $t")
        return Msg.Receive(0)
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

