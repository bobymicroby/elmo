package com.example.elmo.ui.hello

import android.app.Activity
import android.os.Bundle
import com.example.elmo.R
import com.example.elmo.elmo.MainThreadView
import com.example.elmo.elmo.UpdateIO
import dev.boby.elmo.Sandbox
import kotlinx.android.synthetic.main.hello_activity.*

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