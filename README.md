

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
class HelloWorldUpdate : UpdateIO<HelloWorldModel, Msg> {
    override fun update(msg: Msg, model: HelloWorldModel): HelloWorldModel {
        return when (msg) {
            Msg.Reverse -> model.copy(title = model.title.reversed())
        }
    }
}
class HelloWorldActivity : Activity(), MainThreadView<HelloWorldModel> {

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

